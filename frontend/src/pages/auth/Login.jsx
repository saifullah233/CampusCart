import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import campuscartSymbol from '../../assets/campuscart_symbol.png';
import csLogo from '../../assets/cs_logo.png';
import './Login.css';

export default function Login({ accountType: propAccountType }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const isCommunity = propAccountType === 'COMMUNITY' || location.pathname.includes('/login/community');
  const targetAccountType = isCommunity ? 'COMMUNITY' : 'STUDENT';

  const [form, setForm] = useState({
    email: location.state?.email || '',
    password: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [infoMessage, setInfoMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState(
    location.state?.successMessage || location.state?.message || ''
  );
  const [pendingVerification, setPendingVerification] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
    setApiError('');
    setSuccessMessage('');
    setPendingVerification(null);
  };

  const validate = () => {
    const errs = {};
    if (!form.email.trim()) {
      errs.email = 'Email address is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      errs.email = 'Please enter a valid email address.';
    }
    if (!form.password) {
      errs.password = 'Password is required.';
    }
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;

    setSubmitting(true);
    setApiError('');
    setInfoMessage('');
    setSuccessMessage('');
    setPendingVerification(null);

    try {
      const res = await api.post('/api/v1/auth/login', {
        email: form.email.trim(),
        password: form.password,
        accountType: targetAccountType,
      });

      if (res.success && res.data) {
        if (res.data.status === 'PENDING_VERIFICATION') {
          setPendingVerification(res.data);
          return;
        }

        const accessToken = res.data.tokens?.accessToken || res.data.accessToken;
        const refreshToken = res.data.tokens?.refreshToken || res.data.refreshToken;

        if (accessToken) {
          // Temporarily store access token in localStorage so the profile request is authenticated
          localStorage.setItem('cc_accessToken', accessToken);

          let userProfile = { email: form.email.trim(), accountType: targetAccountType };
          try {
            const profileRes = await api.get('/api/v1/users/me');
            if (profileRes?.data) {
              userProfile = profileRes.data;
            }
          } catch {
            // Fallback to basic user representation if profile fetch fails
          }

          login(userProfile, accessToken, refreshToken);
          navigate('/products', { replace: true });
        }
      }
    } catch (err) {
      const errCode = err?.error?.code;
      if (errCode === 'INVALID_CREDENTIALS') {
        setApiError('Invalid email or password.');
      } else if (errCode === 'ACCOUNT_NOT_ACTIVE') {
        setApiError('Your account is pending verification or suspended. Please contact support.');
      } else if (errCode === 'LOGIN_RATE_LIMITED') {
        setApiError('Too many failed login attempts. Please try again in a few minutes.');
      } else {
        const msg = err?.message || err?.error?.detail || 'Login failed. Please check your credentials and try again.';
        setApiError(msg);
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleContinueVerification = () => {
    if (!pendingVerification) return;
    const flow = isCommunity ? 'community' : 'student';
    const emailOtp = pendingVerification.emailOtp || pendingVerification.otp;
    const pending = {
      emailChallenge: emailOtp,
      challengeId: emailOtp?.challengeId,
      destination: emailOtp?.destination,
      expiresAt: emailOtp?.expiresAt,
      nextResendAt: emailOtp?.nextResendAt,
      emailVerified: !!pendingVerification.emailVerified,
      flow,
      returnPath: isCommunity ? '/login/community' : '/login/student',
    };
    localStorage.setItem('cc_pending_verification', JSON.stringify(pending));
    navigate(isCommunity ? '/register/community/verify-otp' : '/register/student/verify-otp', {
      state: pending,
    });
  };

  return (
    <div className="login-page">
      {/* ─── Header ─── */}
      <header className="login-header">
        <div className="login-header__left">
          <Link to="/" className="login-logo-slot" aria-label="CampusCart logo">
            <img src={campuscartSymbol} className="login-logo-img" alt="CampusCart" />
            <span className="login-logo-wordmark">
              Campus<span className="login-logo-wordmark--bold">Cart</span>
            </span>
          </Link>
        </div>
        <div className="login-header__right">
          <div className="login-slogan">
            <div>&ldquo;Built by Students,</div>
            <div className="login-slogan--highlight">For Students.&rdquo;</div>
          </div>
          <div className="login-header-divider" />
          <div className="login-cs-slot" aria-label="Campus By Students logo">
            <img src={csLogo} className="login-cs-logo" alt="Campus By Students" />
          </div>
        </div>
      </header>

      {/* ─── Back Action Bar ─── */}
      <div className="login-back-bar">
        <Link to="/login" className="login-back-link">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          Back to login selection
        </Link>
      </div>

      {/* ─── Main Content ─── */}
      <main className="login-main">
        <div className="login-card">
          <div className="login-card-header">
            <div className="login-icon-wrapper">
              {isCommunity ? (
                <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="2" y1="12" x2="22" y2="12" />
                  <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10z" />
                </svg>
              ) : (
                <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
                  <path d="M6 12v5c0 2 2 3 6 3s6-1 6-3v-5" />
                </svg>
              )}
            </div>
            <h1 className="login-title">{isCommunity ? 'Community Login' : 'College Student Login'}</h1>
            <p className="login-subtitle">
              {isCommunity
                ? 'Log in to access your community marketplace account.'
                : 'Log in with your official college email.'}
            </p>
          </div>

          {/* Success Banner */}
          {successMessage && (
            <div className="login-alert login-alert--success">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
              <span>{successMessage}</span>
            </div>
          )}

          {/* Info Banner */}
          {infoMessage && (
            <div className="login-alert login-alert--info">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="16" x2="12" y2="12" />
                <line x1="12" y1="8" x2="12.01" y2="8" />
              </svg>
              <span>{infoMessage}</span>
            </div>
          )}

          {/* Error Banner */}
          {apiError && (
            <div className="login-alert login-alert--error">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="16" x2="12" y2="12" />
                <line x1="12" y1="8" x2="12.01" y2="8" />
              </svg>
              <span>{apiError}</span>
            </div>
          )}

          {/* Pending Verification Recovery Card */}
          {pendingVerification && (
            <div className="login-pending-card">
              <div className="login-pending-badge">Verification Required</div>
              <p className="login-pending-text">
                Your account is pending email verification. Please verify your OTP to continue.
              </p>
              <button
                type="button"
                className="login-verify-btn"
                onClick={handleContinueVerification}
              >
                Verify Email OTP &rarr;
              </button>
              <p className="login-pending-hint">
                Didn&apos;t receive the code? You can resend it from the verification screen.
              </p>
            </div>
          )}

          <form className="login-form" onSubmit={handleSubmit} noValidate>
            {/* Email Field */}
            <div className="login-field">
              <label className="login-field__label" htmlFor="login-email">
                {isCommunity ? 'Email Address' : 'Official College Email'}
              </label>
              <div className={`login-field__row ${errors.email ? 'login-field__row--error' : ''}`}>
                <div className="login-field__icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="2" y="4" width="20" height="16" rx="2" />
                    <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
                  </svg>
                </div>
                <input
                  id="login-email"
                  name="email"
                  type="email"
                  className="login-field__input"
                  placeholder={isCommunity ? 'e.g. you@example.com' : 'e.g. student@bennett.edu.in'}
                  value={form.email}
                  onChange={handleChange}
                  autoComplete="email"
                  autoFocus={!form.email}
                />
              </div>
              {errors.email && <div className="login-field__error">{errors.email}</div>}
            </div>

            {/* Password Field */}
            <div className="login-field">
              <div className="login-field__header">
                <label className="login-field__label" htmlFor="login-password">
                  Password
                </label>
                <Link
                  to="/forgot-password"
                  className="login-field__forgot"
                >
                  Forgot Password?
                </Link>
              </div>
              <div className={`login-field__row ${errors.password ? 'login-field__row--error' : ''}`}>
                <div className="login-field__icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                </div>
                <input
                  id="login-password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  className="login-field__input"
                  placeholder="Enter your password"
                  value={form.password}
                  onChange={handleChange}
                  autoComplete="current-password"
                  autoFocus={!!form.email}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="login-field__toggle"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? (
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                      <line x1="1" y1="1" x2="23" y2="23" />
                    </svg>
                  ) : (
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  )}
                </button>
              </div>
              {errors.password && <div className="login-field__error">{errors.password}</div>}
            </div>

            {/* Submit Button */}
            <button type="submit" className="login-submit-btn" disabled={submitting}>
              {submitting ? (
                <>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="animate-spin">
                    <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                  </svg>
                  Logging in...
                </>
              ) : (
                'Log in'
              )}
            </button>
          </form>

          {/* Switch to Registration */}
          <div className="login-switch">
            Don&apos;t have an account?{' '}
            <Link
              to={isCommunity ? '/register/community' : '/register/student'}
              className="login-switch__link"
            >
              {isCommunity ? 'Register as Community' : 'Register as Student'}
            </Link>
          </div>
        </div>
      </main>

      {/* ─── Footer ─── */}
      <footer className="login-footer">
        &copy; {new Date().getFullYear()} CampusCart. Built by Students, For Students.
      </footer>
    </div>
  );
}
