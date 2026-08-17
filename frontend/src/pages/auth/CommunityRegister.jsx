import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../../services/api';
import campuscartSymbol from '../../assets/campuscart_symbol.png';
import csLogo from '../../assets/cs_logo.png';
import SearchableSelect from '../../components/common/SearchableSelect';
import './CommunityRegister.css';

export default function CommunityRegister() {
  const navigate = useNavigate();

  // ─── Form state ───
  const [form, setForm] = useState({
    email: '',
    cityId: '',
    phoneNumber: '',
    fullName: '',
    password: '',
    confirmPassword: '',
  });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [isExistingAccount, setIsExistingAccount] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // ─── City data ───
  const [cities, setCities] = useState([]);
  const [citiesLoading, setCitiesLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    api.get('/api/v1/cities')
      .then((res) => {
        if (!cancelled && res.success) {
          setCities(res.data || []);
        }
      })
      .catch(() => {
        if (!cancelled) setCities([]);
      })
      .finally(() => {
        if (!cancelled) setCitiesLoading(false);
      });
    return () => { cancelled = true; };
  }, []);

  // ─── Helpers ───
  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
    if (apiError) {
      setApiError('');
      setIsExistingAccount(false);
    }
  };

  const validate = () => {
    const errs = {};
    if (!form.email.trim()) {
      errs.email = 'Email is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      errs.email = 'Enter a valid email address.';
    }
    if (!form.cityId) {
      errs.cityId = 'City is required.';
    }
    if (!form.phoneNumber.trim()) {
      errs.phoneNumber = 'Phone number is required.';
    } else if (!/^\+?[0-9 ()-]{8,24}$/.test(form.phoneNumber.trim())) {
      errs.phoneNumber = 'Enter a valid phone number.';
    }
    if (!form.fullName.trim()) {
      errs.fullName = 'Full name is required.';
    } else if (form.fullName.trim().length > 150) {
      errs.fullName = 'Full name must be 150 characters or less.';
    }
    if (!form.password) {
      errs.password = 'Password is required.';
    } else if (form.password.length < 8 || form.password.length > 72) {
      errs.password = 'Password must be 8–72 characters.';
    }
    if (!form.confirmPassword) {
      errs.confirmPassword = 'Confirm your password.';
    } else if (form.password !== form.confirmPassword) {
      errs.confirmPassword = 'Passwords do not match.';
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

    try {
      const res = await api.post('/api/v1/auth/register/community', {
        email: form.email.trim(),
        fullName: form.fullName.trim(),
        cityId: form.cityId,
        phoneNumber: form.phoneNumber.trim(),
        password: form.password,
      });

      if (res.success && res.data) {
        const emailOtp = res.data.emailOtp || res.data.otp;
        const pending = {
          emailChallenge: emailOtp,
          challengeId: emailOtp?.challengeId,
          channel: 'EMAIL',
          destination: emailOtp?.destination,
          expiresAt: emailOtp?.expiresAt,
          nextResendAt: emailOtp?.nextResendAt,
          flow: 'community',
          returnPath: '/register/community',
        };
        localStorage.setItem('cc_pending_verification', JSON.stringify(pending));
        navigate('/register/community/verify-otp', { state: pending });
      }
    } catch (err) {
      const isDuplicate =
        err?.error?.code === 'DUPLICATE_RESOURCE' ||
        err?.message?.toLowerCase().includes('already exists') ||
        err?.error?.detail?.toLowerCase().includes('already exists');

      if (isDuplicate) {
        setApiError('An account with this email already exists. Please log in to continue.');
        setIsExistingAccount(true);
      } else {
        const msg = err?.message || err?.error?.detail || 'Registration failed. Please try again.';
        setApiError(msg);
        setIsExistingAccount(false);
      }
    } finally {
      setSubmitting(false);
    }
  };

  // ─── Render ───
  return (
    <div className="cr-page">
      {/* ─── Header ─── */}
      <header className="cr-header">
        <div className="cr-header__left">
          <Link to="/" className="cr-logo-slot" aria-label="CampusCart logo">
            <img src={campuscartSymbol} className="cr-logo-img" alt="CampusCart" />
            <span className="cr-logo-wordmark">
              Campus<span className="cr-logo-wordmark--bold">Cart</span>
            </span>
          </Link>
        </div>
        <div className="cr-header__right">
          <div className="cr-slogan">
            <div>&ldquo;Built by Students,</div>
            <div className="cr-slogan--highlight">For Students.&rdquo;</div>
          </div>
          <div className="cr-header-divider" />
          <div className="cr-cs-slot" aria-label="Campus By Students logo">
            <img src={csLogo} className="cr-cs-logo" alt="Campus By Students" />
          </div>
        </div>
      </header>

      {/* ─── Mobile Back ─── */}
      <Link to="/" className="cr-back">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
        Back
      </Link>

      {/* ─── Main ─── */}
      <main className="cr-main">
        {/* Left Panel — Desktop */}
        <section className="cr-left">
          <div className="cr-left__content">
            <h1 className="cr-welcome">
              Welcome to<br />
              <span className="cr-welcome--brand">CampusCart</span>
            </h1>
            <p className="cr-tagline">Buy • Sell • Connect on Campus</p>

            <ul className="cr-features">
              <li className="cr-feature">
                <div className="cr-feature__icon">
                  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#2563EB" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /><polyline points="9 12 11 14 15 10" /></svg>
                </div>
                <div>
                  <h3 className="cr-feature__title">Secure</h3>
                  <p className="cr-feature__desc">Your data is safe with us. We use advanced security to protect your information.</p>
                </div>
              </li>
              <li className="cr-feature">
                <div className="cr-feature__icon">
                  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#2563EB" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" /></svg>
                </div>
                <div>
                  <h3 className="cr-feature__title">Verified</h3>
                  <p className="cr-feature__desc">Every user and college is verified for a trusted marketplace experience.</p>
                </div>
              </li>
              <li className="cr-feature">
                <div className="cr-feature__icon">
                  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#2563EB" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" /></svg>
                </div>
                <div>
                  <h3 className="cr-feature__title">Fast &amp; Easy</h3>
                  <p className="cr-feature__desc">Simple steps to get started and explore amazing opportunities.</p>
                </div>
              </li>
            </ul>
          </div>

          <div className="cr-illustration">
            <svg className="cr-illustration__svg" viewBox="0 0 520 140" preserveAspectRatio="xMidYMax meet" fill="none">
              <rect x="0" y="120" width="520" height="20" rx="0" fill="#DBEAFE" opacity="0.5" />
              <circle cx="30" cy="100" r="16" fill="#93C5FD" opacity="0.6" /><rect x="28" y="112" width="4" height="14" rx="1" fill="#60A5FA" />
              <circle cx="80" cy="105" r="12" fill="#BFDBFE" opacity="0.7" /><rect x="78" y="114" width="4" height="10" rx="1" fill="#93C5FD" />
              <circle cx="440" cy="100" r="14" fill="#93C5FD" opacity="0.6" /><rect x="438" y="112" width="4" height="14" rx="1" fill="#60A5FA" />
              <circle cx="490" cy="105" r="12" fill="#BFDBFE" opacity="0.7" /><rect x="488" y="114" width="4" height="10" rx="1" fill="#93C5FD" />
              <rect x="110" y="80" width="50" height="40" rx="2" fill="#DBEAFE" />
              <rect x="115" y="86" width="10" height="8" rx="1" fill="#fff" /><rect x="130" y="86" width="10" height="8" rx="1" fill="#fff" /><rect x="145" y="86" width="10" height="8" rx="1" fill="#fff" />
              <rect x="115" y="100" width="10" height="8" rx="1" fill="#fff" /><rect x="130" y="100" width="10" height="8" rx="1" fill="#fff" /><rect x="145" y="100" width="10" height="8" rx="1" fill="#fff" />
              <rect x="220" y="55" width="80" height="65" rx="2" fill="#BFDBFE" />
              <polygon points="260,28 230,55 290,55" fill="#3B82F6" />
              <circle cx="260" cy="72" r="8" fill="#fff" /><circle cx="260" cy="72" r="3" fill="#3B82F6" />
              <rect x="230" y="80" width="12" height="10" rx="1" fill="#fff" /><rect x="248" y="80" width="12" height="10" rx="1" fill="#fff" /><rect x="266" y="80" width="12" height="10" rx="1" fill="#fff" />
              <rect x="248" y="96" width="12" height="10" rx="1" fill="#fff" /><rect x="253" y="108" width="14" height="12" rx="1" fill="#EFF6FF" />
              <rect x="360" y="80" width="50" height="40" rx="2" fill="#DBEAFE" />
              <rect x="365" y="86" width="10" height="8" rx="1" fill="#fff" /><rect x="380" y="86" width="10" height="8" rx="1" fill="#fff" /><rect x="395" y="86" width="10" height="8" rx="1" fill="#fff" />
              <rect x="365" y="100" width="10" height="8" rx="1" fill="#fff" /><rect x="380" y="100" width="10" height="8" rx="1" fill="#fff" /><rect x="395" y="100" width="10" height="8" rx="1" fill="#fff" />
              <path d="M150 30 Q155 24 160 30 Q165 24 170 30" stroke="#BFDBFE" strokeWidth="1.5" fill="none" strokeLinecap="round" />
              <path d="M350 40 Q354 35 358 40 Q362 35 366 40" stroke="#BFDBFE" strokeWidth="1.5" fill="none" strokeLinecap="round" />
            </svg>
          </div>
        </section>

        {/* Right Panel — Form */}
        <section className="cr-right">
          <div className="cr-form-header">
            <div className="cr-form-header__icon">
              <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#2563EB" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" /><line x1="2" y1="12" x2="22" y2="12" />
                <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
              </svg>
            </div>
            <h2 className="cr-form-title">
              <span className="cr-form-title--blue">Community User </span>Registration
            </h2>
          </div>
          <p className="cr-form-subtitle">
            Create your account and start your journey with CampusCart{' '}
            <span className="cr-form-subtitle__check">✓</span>
          </p>

          {apiError && (
            <div className="cr-api-error">
              <div>{apiError}</div>
              {isExistingAccount && (
                <button
                  type="button"
                  onClick={() => navigate('/login', { state: { email: form.email } })}
                  className="cr-api-action-btn"
                >
                  Log in to your account &rarr;
                </button>
              )}
            </div>
          )}

          <form className="cr-form" onSubmit={handleSubmit} noValidate>
            {/* Email */}
            <div className="cr-field">
              <label className="cr-field__label" htmlFor="cr-email">Email ID</label>
              <div className={`cr-field__row${errors.email ? ' cr-field__row--error' : ''}`}>
                <div className="cr-field__icon">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#64748b" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="2" y="4" width="20" height="16" rx="2" /><path d="M22 7l-10 6L2 7" />
                  </svg>
                </div>
                <input id="cr-email" name="email" type="email" className="cr-field__input" placeholder="Enter your email address" value={form.email} onChange={handleChange} autoComplete="email" />
              </div>
              {errors.email && <div className="cr-field__error">{errors.email}</div>}
            </div>

            {/* City */}
            <div className="cr-field">
              <label className="cr-field__label" htmlFor="cr-city">City</label>
              <SearchableSelect
                id="cr-city"
                name="cityId"
                value={form.cityId}
                onChange={handleChange}
                options={cities.map((c) => ({
                  id: c.id,
                  name: c.name,
                  sublabel: c.state,
                }))}
                placeholder="Select your city"
                searchPlaceholder="Search city (e.g. Noida, Delhi)..."
                disabled={citiesLoading}
                loading={citiesLoading}
                hasError={!!errors.cityId}
                icon={
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#64748b" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" /><circle cx="12" cy="10" r="3" />
                  </svg>
                }
              />
              {errors.cityId && <div className="cr-field__error">{errors.cityId}</div>}
            </div>

            {/* Phone Number */}
            <div className="cr-field">
              <label className="cr-field__label" htmlFor="cr-phone">Phone Number</label>
              <div className={`cr-field__row${errors.phoneNumber ? ' cr-field__row--error' : ''}`}>
                <div className="cr-field__icon">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#64748b" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
                  </svg>
                </div>
                <input id="cr-phone" name="phoneNumber" type="tel" className="cr-field__input" placeholder="Enter your phone number" value={form.phoneNumber} onChange={handleChange} autoComplete="tel" />
              </div>
              {errors.phoneNumber && <div className="cr-field__error">{errors.phoneNumber}</div>}
            </div>

            {/* Full Name */}
            <div className="cr-field">
              <label className="cr-field__label" htmlFor="cr-name">Full Name</label>
              <div className={`cr-field__row${errors.fullName ? ' cr-field__row--error' : ''}`}>
                <div className="cr-field__icon">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#64748b" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
                  </svg>
                </div>
                <input id="cr-name" name="fullName" type="text" className="cr-field__input" placeholder="Enter your full name" value={form.fullName} onChange={handleChange} autoComplete="name" />
              </div>
              {errors.fullName && <div className="cr-field__error">{errors.fullName}</div>}
            </div>

            {/* Password */}
            <div className="cr-field">
              <label className="cr-field__label" htmlFor="cr-password">Password</label>
              <div className={`cr-field__row${errors.password ? ' cr-field__row--error' : ''}`}>
                <div className="cr-field__icon">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#64748b" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                </div>
                <input id="cr-password" name="password" type="password" className="cr-field__input" placeholder="Create a password (min 8 chars)" value={form.password} onChange={handleChange} autoComplete="new-password" />
              </div>
              {errors.password && <div className="cr-field__error">{errors.password}</div>}
            </div>

            {/* Confirm Password */}
            <div className="cr-field">
              <label className="cr-field__label" htmlFor="cr-confirm">Confirm Password</label>
              <div className={`cr-field__row${errors.confirmPassword ? ' cr-field__row--error' : ''}`}>
                <div className="cr-field__icon">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#64748b" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                </div>
                <input id="cr-confirm" name="confirmPassword" type="password" className="cr-field__input" placeholder="Re-enter your password" value={form.confirmPassword} onChange={handleChange} autoComplete="new-password" />
              </div>
              {errors.confirmPassword && <div className="cr-field__error">{errors.confirmPassword}</div>}
            </div>

            <button type="submit" className="cr-submit" disabled={submitting}>
              {submitting ? 'Registering...' : 'Continue'}
              {!submitting && (
                <span className="cr-submit__arrow">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="5" y1="12" x2="19" y2="12" /><polyline points="12 5 19 12 12 19" /></svg>
                </span>
              )}
            </button>

            <div className="cr-login-prompt">
              Already have an account?
              <Link to="/login/community" state={{ email: form.email }} className="cr-login-link">
                Log in
              </Link>
            </div>
          </form>

          {/* Security message */}
          <div className="cr-security">
            <div className="cr-security__icon">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="#64748b" stroke="none"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM12 17c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1s3.1 1.39 3.1 3.1v2z"/></svg>
            </div>
            <p className="cr-security__text">
              Your information is secure and<br />will only be used to create your account.
            </p>
          </div>

          {/* Why Community Account */}
          <div className="cr-why">
            <div className="cr-why__icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" /></svg>
            </div>
            <div className="cr-why__content">
              <div className="cr-why__title">Why Community Account?</div>
              <div className="cr-why__benefits">
                <div className="cr-why__benefit">
                  <svg className="cr-why__benefit-check" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" /></svg>
                  Buy amazing products
                </div>
                <div className="cr-why__benefit">
                  <svg className="cr-why__benefit-check" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" /></svg>
                  Sell your own handmade items
                </div>
                <div className="cr-why__benefit">
                  <svg className="cr-why__benefit-check" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" /></svg>
                  Connect with buyers across the city
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* ─── Footer ─── */}
      <footer className="cr-footer">
        <div className="cr-footer__links">
          <a href="#tos" className="cr-footer__link">Terms of Service</a>
          <span className="cr-footer__dot">•</span>
          <a href="#privacy" className="cr-footer__link">Privacy Policy</a>
        </div>
        <p className="cr-footer__copy">© CampusCart 2025. All Rights Reserved.</p>
      </footer>
    </div>
  );
}
