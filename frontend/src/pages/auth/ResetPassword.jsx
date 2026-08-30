import { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import api from '../../services/api';
import campuscartSymbol from '../../assets/campuscart_symbol.png';
import csLogo from '../../assets/cs_logo.png';
import './ResetPassword.css';

export default function ResetPassword() {
  const navigate = useNavigate();
  const location = useLocation();

  const [resetToken, setResetToken] = useState(() => {
    return location.state?.resetToken || sessionStorage.getItem('cc_reset_token') || '';
  });

  const [form, setForm] = useState({
    newPassword: '',
    confirmPassword: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!resetToken) {
      navigate('/forgot-password', { replace: true });
    }
  }, [resetToken, navigate]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
    setApiError('');
  };

  const validate = () => {
    const errs = {};
    if (!form.newPassword) {
      errs.newPassword = 'New password is required.';
    } else if (form.newPassword.length < 8 || form.newPassword.length > 72) {
      errs.newPassword = 'Password must be between 8 and 72 characters.';
    }

    if (!form.confirmPassword) {
      errs.confirmPassword = 'Confirm password is required.';
    } else if (form.newPassword !== form.confirmPassword) {
      errs.confirmPassword = 'Passwords do not match.';
    }

    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;

    if (!resetToken) {
      setApiError('Session expired. Please request a new password reset.');
      return;
    }

    setSubmitting(true);
    setApiError('');

    try {
      const res = await api.post('/api/v1/auth/forgot-password/reset', {
        resetToken,
        newPassword: form.newPassword,
      });

      if (res.success) {
        sessionStorage.removeItem('cc_reset_token');
        navigate('/login', {
          state: {
            successMessage: 'Password reset successfully. Please log in with your new password.',
          },
          replace: true,
        });
      }
    } catch (err) {
      const msg = err?.message || err?.error?.detail || 'Failed to reset password. Please request a new link/code.';
      setApiError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="rp-page">
      {/* ─── Header ─── */}
      <header className="rp-header">
        <div className="rp-header__left">
          <Link to="/" className="rp-logo-slot" aria-label="CampusCart logo">
            <img src={campuscartSymbol} className="rp-logo-img" alt="CampusCart" />
            <span className="rp-logo-wordmark">
              Campus<span className="rp-logo-wordmark--bold">Cart</span>
            </span>
          </Link>
        </div>
        <div className="rp-header__right">
          <div className="fp-slogan">
            <div>&ldquo;Built by Students,</div>
            <div className="fp-slogan--highlight">For Students.&rdquo;</div>
          </div>
          <div className="rp-header-divider" />
          <div className="rp-cs-slot" aria-label="Campus By Students logo">
            <img src={csLogo} className="rp-cs-logo" alt="Campus By Students" />
          </div>
        </div>
      </header>

      {/* ─── Back Action Bar ─── */}
      <div className="rp-back-bar">
        <Link to="/login" className="rp-back-link">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          Back to Login
        </Link>
      </div>

      {/* ─── Main Content ─── */}
      <main className="rp-main">
        <div className="rp-card">
          <div className="rp-card-header">
            <div className="rp-icon-wrapper">
              <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                <polyline points="9 12 11 14 15 10" />
              </svg>
            </div>
            <h1 className="rp-title">Reset Your Password</h1>
            <p className="rp-subtitle">
              Create a new secure password for your CampusCart account.
            </p>
          </div>

          {/* Error Banner */}
          {apiError && (
            <div className="rp-alert rp-alert--error">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="16" x2="12" y2="12" />
                <line x1="12" y1="8" x2="12.01" y2="8" />
              </svg>
              <span>{apiError}</span>
            </div>
          )}

          <form className="rp-form" onSubmit={handleSubmit} noValidate>
            {/* New Password Field */}
            <div className="rp-field">
              <label className="rp-field__label" htmlFor="rp-new-password">
                New Password
              </label>
              <div className={`rp-field__row ${errors.newPassword ? 'rp-field__row--error' : ''}`}>
                <div className="rp-field__icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                </div>
                <input
                  id="rp-new-password"
                  name="newPassword"
                  type={showPassword ? 'text' : 'password'}
                  className="rp-field__input"
                  placeholder="Enter new password (min 8 chars)"
                  value={form.newPassword}
                  onChange={handleChange}
                  autoComplete="new-password"
                  autoFocus
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="rp-field__toggle"
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
              {errors.newPassword && <div className="rp-field__error">{errors.newPassword}</div>}
            </div>

            {/* Confirm Password Field */}
            <div className="rp-field">
              <label className="rp-field__label" htmlFor="rp-confirm-password">
                Confirm New Password
              </label>
              <div className={`rp-field__row ${errors.confirmPassword ? 'rp-field__row--error' : ''}`}>
                <div className="rp-field__icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                </div>
                <input
                  id="rp-confirm-password"
                  name="confirmPassword"
                  type={showConfirmPassword ? 'text' : 'password'}
                  className="rp-field__input"
                  placeholder="Re-enter your new password"
                  value={form.confirmPassword}
                  onChange={handleChange}
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="rp-field__toggle"
                  aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                >
                  {showConfirmPassword ? (
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
              {errors.confirmPassword && <div className="rp-field__error">{errors.confirmPassword}</div>}
            </div>

            {/* Submit Button */}
            <button type="submit" className="rp-submit-btn" disabled={submitting}>
              {submitting ? (
                <>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="animate-spin">
                    <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                  </svg>
                  Updating Password...
                </>
              ) : (
                'Reset Password'
              )}
            </button>
          </form>
        </div>
      </main>

      {/* ─── Footer ─── */}
      <footer className="rp-footer">
        &copy; {new Date().getFullYear()} CampusCart. Built by Students, For Students.
      </footer>
    </div>
  );
}
