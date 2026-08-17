import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../../services/api';
import campuscartSymbol from '../../assets/campuscart_symbol.png';
import csLogo from '../../assets/cs_logo.png';
import SearchableSelect from '../../components/common/SearchableSelect';
import './StudentRegister.css';

export default function StudentRegister() {
  const navigate = useNavigate();

  // ─── Form state ───
  const [form, setForm] = useState({
    cityId: '',
    collegeId: '',
    officialEmail: '',
    phoneNumber: '',
    fullName: '',
    password: '',
    confirmPassword: '',
  });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [isExistingAccount, setIsExistingAccount] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // ─── City & College data ───
  const [cities, setCities] = useState([]);
  const [citiesLoading, setCitiesLoading] = useState(true);
  const [colleges, setColleges] = useState([]);
  const [collegesLoading, setCollegesLoading] = useState(false);

  // ─── Auto-detection state ───
  const [detectionState, setDetectionState] = useState({
    status: 'idle', // 'idle' | 'detecting' | 'detected' | 'conflict' | 'not_found' | 'error'
    message: '',
    detectedCollege: null,
  });
  const [userManuallySelected, setUserManuallySelected] = useState(false);

  // Load cities on mount
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

  // Load colleges when city changes
  useEffect(() => {
    if (!form.cityId) {
      return;
    }
    let cancelled = false;
    api.get(`/api/v1/colleges?cityId=${form.cityId}`)
      .then((res) => {
        if (!cancelled && res.success) {
          setColleges(res.data || []);
        }
      })
      .catch(() => {
        if (!cancelled) setColleges([]);
      })
      .finally(() => {
        if (!cancelled) setCollegesLoading(false);
      });
    return () => { cancelled = true; };
  }, [form.cityId]);

  // Debounced email domain detection
  useEffect(() => {
    const rawEmail = form.officialEmail.trim().toLowerCase();
    const parts = rawEmail.split('@');
    const domain = parts.length === 2 ? parts[1]?.trim() : '';

    let cancelled = false;

    const timer = setTimeout(() => {
      if (!domain || !domain.includes('.') || domain.endsWith('.')) {
        setDetectionState((prev) =>
          prev.status === 'idle' ? prev : { status: 'idle', message: '', detectedCollege: null }
        );
        return;
      }

      setDetectionState({ status: 'detecting', message: 'Detecting college...', detectedCollege: null });

      api.get(`/api/v1/colleges/by-email-domain/${encodeURIComponent(domain)}`)
        .then((res) => {
          if (cancelled) return;
          if (res.success && res.data) {
            const detected = res.data;
            if (userManuallySelected && form.collegeId && form.collegeId !== detected.collegeId) {
              setDetectionState({
                status: 'conflict',
                detectedCollege: detected,
                message: `Email domain (${domain}) belongs to ${detected.collegeName}, which conflicts with your selected college.`,
              });
            } else {
              setForm((prev) => ({
                ...prev,
                cityId: detected.cityId,
                collegeId: detected.collegeId,
              }));
              setErrors((prev) => ({ ...prev, cityId: '', collegeId: '', officialEmail: '' }));
              setDetectionState({
                status: 'detected',
                detectedCollege: detected,
                message: `College detected: ${detected.collegeName} (${detected.cityName}) ✓`,
              });
            }
          } else {
            setDetectionState({
              status: 'not_found',
              detectedCollege: null,
              message: 'College could not be detected from this email. Please select your city and college manually.',
            });
          }
        })
        .catch(() => {
          if (!cancelled) {
            setDetectionState({
              status: 'error',
              detectedCollege: null,
              message: 'Unable to check email domain. Please select your city and college manually.',
            });
          }
        });
    }, 300);

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [form.officialEmail, form.collegeId, userManuallySelected]);

  // ─── Helpers ───
  const handleChange = (e) => {
    const { name, value } = e.target;

    if (name === 'cityId') {
      setUserManuallySelected(true);
      setForm((prev) => ({ ...prev, cityId: value, collegeId: '' }));
      if (!value) {
        setColleges([]);
      } else {
        setCollegesLoading(true);
        setColleges([]);
      }
    } else if (name === 'collegeId') {
      setUserManuallySelected(true);
      setForm((prev) => ({ ...prev, collegeId: value }));
      if (detectionState.detectedCollege && value && value !== detectionState.detectedCollege.collegeId) {
        setDetectionState({
          status: 'conflict',
          detectedCollege: detectionState.detectedCollege,
          message: `Selected college conflicts with email domain (${detectionState.detectedCollege.collegeName}).`,
        });
      } else if (detectionState.detectedCollege && value === detectionState.detectedCollege.collegeId) {
        setDetectionState({
          status: 'detected',
          detectedCollege: detectionState.detectedCollege,
          message: `College detected: ${detectionState.detectedCollege.collegeName} (${detectionState.detectedCollege.cityName}) ✓`,
        });
      }
    } else {
      setForm((prev) => ({ ...prev, [name]: value }));
    }

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
    if (!form.cityId) {
      errs.cityId = 'City is required.';
    }
    if (!form.collegeId) {
      errs.collegeId = 'College is required.';
    }
    if (!form.officialEmail.trim()) {
      errs.officialEmail = 'College email is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.officialEmail.trim())) {
      errs.officialEmail = 'Enter a valid email address.';
    }
    const cleanPhone = form.phoneNumber.trim().replace(/\D/g, '');
    if (!cleanPhone) {
      errs.phoneNumber = 'Phone number is required.';
    } else if (cleanPhone.length !== 10) {
      errs.phoneNumber = 'Enter a valid 10-digit mobile number.';
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
      const rawPhone = form.phoneNumber.trim().replace(/\D/g, '');
      const formattedPhone = rawPhone.length === 10 ? `+91${rawPhone}` : `+${rawPhone}`;

      const res = await api.post('/api/v1/auth/register/student', {
        cityId: form.cityId,
        collegeId: form.collegeId,
        officialEmail: form.officialEmail.trim(),
        phoneNumber: formattedPhone,
        fullName: form.fullName.trim(),
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
          flow: 'student',
          returnPath: '/register/student',
        };
        localStorage.setItem('cc_pending_verification', JSON.stringify(pending));
        navigate('/register/student/verify-otp', { state: pending });
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
    <div className="sr-page">
      {/* ─── Header ─── */}
      <header className="sr-header">
        <div className="sr-header__left">
          <Link to="/" className="sr-logo-slot" aria-label="CampusCart logo">
            <img src={campuscartSymbol} className="sr-logo-img" alt="CampusCart" />
            <span className="sr-logo-wordmark">
              Campus<span className="sr-logo-wordmark--bold">Cart</span>
            </span>
          </Link>
        </div>
        <div className="sr-header__right">
          <div className="sr-slogan">
            <div>&ldquo;Built by Students,</div>
            <div className="sr-slogan--highlight">For Students.&rdquo;</div>
          </div>
          <div className="sr-header-divider" />
          <div className="sr-cs-slot" aria-label="Campus By Students logo">
            <img src={csLogo} className="sr-cs-logo" alt="Campus By Students" />
          </div>
        </div>
      </header>

      {/* ─── Mobile Back ─── */}
      <Link to="/" className="sr-back">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
        Back
      </Link>

      {/* ─── Main ─── */}
      <main className="sr-main">
        {/* Left Panel — Desktop */}
        <section className="sr-left">
          <div className="sr-left__content">
            <h1 className="sr-welcome">
              Welcome to<br />
              <span className="sr-welcome--brand">CampusCart</span>
            </h1>
            <p className="sr-tagline">Buy • Sell • Connect on Campus</p>

            <ul className="sr-features">
              <li className="sr-feature">
                <div className="sr-feature__icon">
                  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#2563EB" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /><polyline points="9 12 11 14 15 10" /></svg>
                </div>
                <div>
                  <h3 className="sr-feature__title">Secure</h3>
                  <p className="sr-feature__desc">Your data is safe with us. We use advanced security to protect your information.</p>
                </div>
              </li>
              <li className="sr-feature">
                <div className="sr-feature__icon">
                  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#2563EB" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" /></svg>
                </div>
                <div>
                  <h3 className="sr-feature__title">Verified</h3>
                  <p className="sr-feature__desc">Every user and college is verified for a trusted marketplace experience.</p>
                </div>
              </li>
              <li className="sr-feature">
                <div className="sr-feature__icon">
                  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#2563EB" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" /></svg>
                </div>
                <div>
                  <h3 className="sr-feature__title">Fast &amp; Easy</h3>
                  <p className="sr-feature__desc">Simple steps to get started and explore amazing opportunities.</p>
                </div>
              </li>
            </ul>
          </div>

          <div className="sr-illustration">
            <svg className="sr-illustration__svg" viewBox="0 0 520 140" preserveAspectRatio="xMidYMax meet" fill="none">
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
        <section className="sr-right">
          <div className="sr-form-header">
            <div className="sr-form-header__icon">
              {/* Graduation cap icon */}
              <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#2563EB" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
                <path d="M6 12v5c0 2 2 3 6 3s6-1 6-3v-5" />
              </svg>
            </div>
            <h2 className="sr-form-title">
              <span className="sr-form-title--blue">College Student </span>Verification
            </h2>
          </div>
          <p className="sr-form-subtitle">
            Verify with your official college email to continue{' '}
            <span className="sr-form-subtitle__check">✓</span>
          </p>

          {apiError && (
            <div className="sr-api-error">
              <div>{apiError}</div>
              {isExistingAccount && (
                <button
                  type="button"
                  onClick={() => navigate('/login', { state: { email: form.officialEmail } })}
                  className="sr-api-action-btn"
                >
                  Log in to your account &rarr;
                </button>
              )}
            </div>
          )}

          <form className="sr-form" onSubmit={handleSubmit} noValidate>
            {/* Select City */}
            <div className="sr-field">
              <label className="sr-field__label" htmlFor="sr-city">Select City</label>
              <SearchableSelect
                id="sr-city"
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
              {errors.cityId && <div className="sr-field__error">{errors.cityId}</div>}
            </div>

            {/* College Name */}
            <div className="sr-field">
              <label className="sr-field__label" htmlFor="sr-college">College Name</label>
              <SearchableSelect
                id="sr-college"
                name="collegeId"
                value={form.collegeId}
                onChange={handleChange}
                options={colleges.map((c) => ({
                  id: c.id,
                  name: c.name,
                }))}
                placeholder={
                  !form.cityId
                    ? 'Select a city first'
                    : collegesLoading
                      ? 'Loading colleges...'
                      : 'Select your college'
                }
                searchPlaceholder="Search college (e.g. Bennett, Amity)..."
                disabled={!form.cityId || collegesLoading}
                loading={collegesLoading}
                hasError={!!errors.collegeId}
                icon={
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#64748b" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
                    <path d="M6 12v5c0 2 2 3 6 3s6-1 6-3v-5" />
                  </svg>
                }
              />
              {errors.collegeId && <div className="sr-field__error">{errors.collegeId}</div>}
            </div>

            {/* College Email ID */}
            <div className="sr-field">
              <label className="sr-field__label" htmlFor="sr-email">College Email ID</label>
              <div className={`sr-field__row${errors.officialEmail ? ' sr-field__row--error' : ''}`}>
                <div className="sr-field__icon">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#64748b" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="2" y="4" width="20" height="16" rx="2" /><path d="M22 7l-10 6L2 7" />
                  </svg>
                </div>
                <input id="sr-email" name="officialEmail" type="email" className="sr-field__input" placeholder="Enter your official college email" value={form.officialEmail} onChange={handleChange} autoComplete="email" />
              </div>
              {detectionState.status === 'detecting' && (
                <div className="sr-detection-hint sr-detection-hint--loading">
                  Detecting college...
                </div>
              )}
              {detectionState.status === 'detected' && (
                <div className="sr-detection-hint sr-detection-hint--success">
                  {detectionState.message}
                </div>
              )}
              {detectionState.status === 'conflict' && (
                <div className="sr-detection-hint sr-detection-hint--warning">
                  {detectionState.message}
                </div>
              )}
              {detectionState.status === 'not_found' && (
                <div className="sr-detection-hint sr-detection-hint--info">
                  {detectionState.message}
                </div>
              )}
              {detectionState.status === 'error' && (
                <div className="sr-detection-hint sr-detection-hint--info">
                  {detectionState.message}
                </div>
              )}
              {errors.officialEmail && <div className="sr-field__error">{errors.officialEmail}</div>}
            </div>

            {/* Phone Number */}
            <div className="sr-field">
              <label className="sr-field__label" htmlFor="sr-phone">Phone Number</label>
              <div className={`sr-field__row${errors.phoneNumber ? ' sr-field__row--error' : ''}`}>
                <div className="sr-field__icon">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#64748b" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
                  </svg>
                </div>
                <input id="sr-phone" name="phoneNumber" type="tel" className="sr-field__input" placeholder="Enter your 10-digit mobile number" value={form.phoneNumber} onChange={handleChange} autoComplete="tel" />
              </div>
              {errors.phoneNumber && <div className="sr-field__error">{errors.phoneNumber}</div>}
            </div>

            {/* Full Name */}
            <div className="sr-field">
              <label className="sr-field__label" htmlFor="sr-name">Full Name</label>
              <div className={`sr-field__row${errors.fullName ? ' sr-field__row--error' : ''}`}>
                <div className="sr-field__icon">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#64748b" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
                  </svg>
                </div>
                <input id="sr-name" name="fullName" type="text" className="sr-field__input" placeholder="Enter your full name" value={form.fullName} onChange={handleChange} autoComplete="name" />
              </div>
              {errors.fullName && <div className="sr-field__error">{errors.fullName}</div>}
            </div>

            {/* Password */}
            <div className="sr-field">
              <label className="sr-field__label" htmlFor="sr-password">Password</label>
              <div className={`sr-field__row${errors.password ? ' sr-field__row--error' : ''}`}>
                <div className="sr-field__icon">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#64748b" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                </div>
                <input id="sr-password" name="password" type="password" className="sr-field__input" placeholder="Create a password (min 8 chars)" value={form.password} onChange={handleChange} autoComplete="new-password" />
              </div>
              {errors.password && <div className="sr-field__error">{errors.password}</div>}
            </div>

            {/* Confirm Password */}
            <div className="sr-field">
              <label className="sr-field__label" htmlFor="sr-confirm">Confirm Password</label>
              <div className={`sr-field__row${errors.confirmPassword ? ' sr-field__row--error' : ''}`}>
                <div className="sr-field__icon">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#64748b" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                </div>
                <input id="sr-confirm" name="confirmPassword" type="password" className="sr-field__input" placeholder="Re-enter your password" value={form.confirmPassword} onChange={handleChange} autoComplete="new-password" />
              </div>
              {errors.confirmPassword && <div className="sr-field__error">{errors.confirmPassword}</div>}
            </div>

            <button type="submit" className="sr-submit" disabled={submitting}>
              {submitting ? 'Verifying...' : 'Verify & Continue'}
              {!submitting && (
                <span className="sr-submit__arrow">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="5" y1="12" x2="19" y2="12" /><polyline points="12 5 19 12 12 19" /></svg>
                </span>
              )}
            </button>

            <div className="sr-login-prompt">
              Already have an account?
              <Link to="/login/student" state={{ email: form.officialEmail }} className="sr-login-link">
                Log in
              </Link>
            </div>
          </form>

          {/* Security message */}
          <div className="sr-security">
            <div className="sr-security__icon">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="#64748b" stroke="none"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM12 17c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1s3.1 1.39 3.1 3.1v2z"/></svg>
            </div>
            <p className="sr-security__text">
              Your information is secure and will<br />only be used for verification.
            </p>
          </div>

          {/* Trust banner (Student-specific) */}
          <div className="sr-trust">
            <div className="sr-trust__shield">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" />
              </svg>
            </div>
            <p className="sr-trust__text">
              CampusCart protects every user with{' '}
              <span className="sr-trust__link">secure verification</span> &amp;{' '}
              <span className="sr-trust__link">trusted marketplace.</span>
            </p>
          </div>
        </section>
      </main>

      {/* ─── Footer ─── */}
      <footer className="sr-footer">
        <div className="sr-footer__links">
          <a href="#tos" className="sr-footer__link">Terms of Service</a>
          <span className="sr-footer__dot">•</span>
          <a href="#privacy" className="sr-footer__link">Privacy Policy</a>
        </div>
        <p className="sr-footer__copy">© CampusCart 2025. All Rights Reserved.</p>
      </footer>
    </div>
  );
}
