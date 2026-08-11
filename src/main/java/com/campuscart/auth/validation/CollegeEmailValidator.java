package com.campuscart.auth.validation;

import com.campuscart.college.domain.College;
import com.campuscart.college.domain.CollegeEmailDomain;
import com.campuscart.college.repository.CollegeEmailDomainRepository;
import com.campuscart.college.repository.CollegeRepository;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.common.util.ContactNormalizer;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Validates both the selected city/college relationship and the official email domain. */
@Component
public class CollegeEmailValidator {

    private final CollegeRepository collegeRepository;
    private final CollegeEmailDomainRepository domainRepository;

    public CollegeEmailValidator(CollegeRepository collegeRepository,
                                 CollegeEmailDomainRepository domainRepository) {
        this.collegeRepository = collegeRepository;
        this.domainRepository = domainRepository;
    }

    public College validate(UUID cityId, UUID collegeId, String email) {
        College college = collegeRepository.findByIdAndCityId(collegeId, cityId)
                .orElseThrow(() -> new BusinessRuleException("The selected college is not in the selected city."));
        if (!college.isActive() || !college.getCity().isActive()) {
            throw new BusinessRuleException("The selected city or college is inactive.");
        }
        String domain = ContactNormalizer.emailDomain(email);
        CollegeEmailDomain configured = domain == null
                ? null
                : domainRepository.findByDomain(domain).orElse(null);
        if (configured == null || !configured.getCollege().getId().equals(college.getId())) {
            throw new BusinessRuleException("The email domain is not registered for the selected college.");
        }
        return college;
    }
}
