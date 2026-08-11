package com.campuscart.auth.service;

import com.campuscart.auth.dto.AuthTokenResponse;
import com.campuscart.auth.dto.CommunityRegistrationRequest;
import com.campuscart.auth.dto.LoginRequest;
import com.campuscart.auth.dto.RegistrationResponse;
import com.campuscart.auth.dto.StudentRegistrationRequest;
import com.campuscart.auth.dto.VerificationResponse;
import com.campuscart.auth.validation.CollegeEmailValidator;
import com.campuscart.college.domain.College;
import com.campuscart.common.exception.AccountNotActiveException;
import com.campuscart.common.exception.DuplicateResourceException;
import com.campuscart.common.exception.InvalidCredentialsException;
import com.campuscart.common.exception.InvalidTokenException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.common.util.ContactNormalizer;
import com.campuscart.location.domain.City;
import com.campuscart.location.repository.CityRepository;
import com.campuscart.security.JwtService;
import com.campuscart.security.login.LoginRateLimitService;
import com.campuscart.security.otp.OtpChannel;
import com.campuscart.security.refresh.IssuedRefreshToken;
import com.campuscart.security.refresh.RefreshTokenService;
import com.campuscart.user.domain.User;
import com.campuscart.user.repository.UserRepository;
import com.campuscart.user.service.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Application service for registration, credential login, and session lifecycle. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final CollegeEmailValidator collegeEmailValidator;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final LoginRateLimitService loginRateLimitService;

    public AuthService(UserRepository userRepository,
                       CityRepository cityRepository,
                       CollegeEmailValidator collegeEmailValidator,
                       PasswordEncoder passwordEncoder,
                       OtpService otpService,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       UserMapper userMapper,
                       LoginRateLimitService loginRateLimitService) {
        this.userRepository = userRepository;
        this.cityRepository = cityRepository;
        this.collegeEmailValidator = collegeEmailValidator;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.loginRateLimitService = loginRateLimitService;
    }

    @Transactional
    public RegistrationResponse registerStudent(StudentRegistrationRequest request) {
        String email = ContactNormalizer.email(request.officialEmail());
        ensureEmailAvailable(email);
        College college = collegeEmailValidator.validate(request.cityId(), request.collegeId(), email);
        otpService.ensureCanIssue(email);

        User user = new User(email, request.fullName().trim(), college);
        user.changePasswordHash(passwordEncoder.encode(request.password()));
        userRepository.saveAndFlush(user);
        OtpChallengeResponse otp = otpService.issue(user, OtpChannel.EMAIL, email);
        return new RegistrationResponse(user.getId(), user.getStatus().name(), otp);
    }

    @Transactional
    public RegistrationResponse registerCommunity(CommunityRegistrationRequest request) {
        String email = ContactNormalizer.email(request.email());
        String phone = normalizePhone(request.phoneNumber());
        ensureEmailAvailable(email);
        if (userRepository.existsByPhoneNumber(phone)) {
            throw new DuplicateResourceException("An account with this phone number already exists.");
        }
        City city = cityRepository.findByIdAndActiveTrue(request.cityId())
                .orElseThrow(() -> ResourceNotFoundException.of("City", request.cityId()));
        otpService.ensureCanIssue(phone);

        User user = User.community(email, request.fullName().trim(), city, phone);
        user.changePasswordHash(passwordEncoder.encode(request.password()));
        userRepository.saveAndFlush(user);
        OtpChallengeResponse otp = otpService.issue(user, OtpChannel.PHONE, phone);
        return new RegistrationResponse(user.getId(), user.getStatus().name(), otp);
    }

    @Transactional
    public VerificationResponse verifyRegistration(UUID challengeId, String code) {
        User user = otpService.verify(challengeId, code);
        return new VerificationResponse(issueTokens(user), userMapper.toProfile(user));
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        String email = ContactNormalizer.email(request.email());
        loginRateLimitService.ensureAllowed(email);
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            loginRateLimitService.recordFailure(email);
            return new InvalidCredentialsException();
        });
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginRateLimitService.recordFailure(email);
            throw new InvalidCredentialsException();
        }
        if (!user.getStatus().canAuthenticate()) {
            loginRateLimitService.recordSuccess(email);
            throw new AccountNotActiveException();
        }
        loginRateLimitService.recordSuccess(email);
        return issueTokens(user);
    }

    @Transactional
    public AuthTokenResponse refresh(String rawRefreshToken) {
        IssuedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken);
        User user = userRepository.findById(rotated.userId()).orElseThrow(InvalidTokenException::new);
        if (!user.getStatus().canAuthenticate()) {
            throw new InvalidTokenException();
        }
        return tokenResponse(user, rotated);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthTokenResponse issueTokens(User user) {
        return tokenResponse(user, refreshTokenService.issueFor(user));
    }

    private AuthTokenResponse tokenResponse(User user, IssuedRefreshToken refreshToken) {
        return new AuthTokenResponse(
                "Bearer",
                jwtService.generateAccessToken(user),
                jwtService.getAccessTokenTtl().toSeconds(),
                refreshToken.rawToken(),
                refreshToken.expiresAt());
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }
    }

    private String normalizePhone(String value) {
        String normalized = ContactNormalizer.phone(value);
        if (normalized == null || !normalized.matches("\\+[1-9][0-9]{7,14}")) {
            throw new BusinessRuleException("Invalid phone number.");
        }
        return normalized;
    }
}
