import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../../services/api';
import campuscartSymbol from '../../assets/campuscart_symbol.png';
import csLogo from '../../assets/cs_logo.png';
import './ForgotPassword.css';

export default function ForgotPassword() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const trimmed = email.trim();
    if (!trimmed) {
      setError('Email address is required.');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
      setError('Please enter a valid email address.');
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      const res = await api.post('/api/v1/auth/forgot-password', { email: trimmed });
      if (res.success && res.data) {
        const session = {
          challengeId: res.data.challengeId,
          destination: res.data.destination,
          expiresAt: res.data.expiresAt,
          nextResendAt: res.data.nextResendAt,
          email: trimmed,
          returnPath: '/forgot-password',
        };
        localStorage.setItem('cc_forgot_password_session', JSON.stringify(session));
        navigate('/forgot-password/verify-otp', { state: session });
      }
    } catch (err) {
      const msg = err?.message || err?.error?.detail || 'Failed to request password reset. Please try again.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fp-page">
      {/* ─── Header ─── */}
      <header className="fp-header">
        <div className="fp-header__left">
          <Link to="/" className="fp-logo-slot" aria-label="CampusCart logo">
            <img src={campuscartSymbol} className="fp-logo-img" alt="CampusCart" />
            <span className="fp-logo-wordmark">
              Campus<span className="fp-logo-wordmark--bold">Cart</span>
            </span>
          </Link>
        </div>
        <div className="fp-header__right">
          <div className="fp-slogan">
            <div>&ldquo;Built by Students,</div>
            <div className="fp-slogan--highlight">For Students.&rdquo;</div>
          </div>
          <div className="fp-header-divider" />
          <div className="fp-cs-slot" aria-label="Campus By Students logo">
            <img src={csLogo} className="fp-cs-logo" alt="Campus By Students" />
          </div>
        </div>
      </header>

      {/* ─── Back Action Bar ─── */}
      <div className="fp-back-bar">
        <Link to="/login" className="fp-back-link">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          Back to Login
        </Link>
      </div>

      {/* ─── Main Content ─── */}
      <main className="fp-main">
        <div className="fp-card">
          <div className="fp-card-header">
            <div className="fp-icon-wrapper">
              <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
            </div>
            <h1 className="fp-title">Forgot Password?</h1>
            <p className="fp-subtitle">
              Enter your registered email address and we&apos;ll send you a verification code.
            </p>
          </div>

          {/* Error Banner */}
          {error && (
            <div className="fp-alert fp-alert--error">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="16" x2="12" y2="12" />
                <line x1="12" y1="8" x2="12.01" y2="8" />
              </svg>
              <span>{error}</span>
            </div>
          )}

          <form className="fp-form" onSubmit={handleSubmit} noValidate>
            {/* Email Field */}
            <div className="fp-field">
              <label className="fp-field__label" htmlFor="fp-email">
                Registered Email Address
              </label>
              <div className={`fp-field__row ${error ? 'fp-field__row--error' : ''}`}>
                <div className="fp-field__icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="2" y="4" width="20" height="16" rx="2" />
                    <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
                  </svg>
                </div>
                <input
                  id="fp-email"
                  name="email"
                  type="email"
                  className="fp-field__input"
                  placeholder="e.g. you@example.com or student@college.edu.in"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    if (error) setError('');
                  }}
                  autoComplete="email"
                  autoFocus
                />
              </div>
            </div>

            {/* Submit Button */}
            <button type="submit" className="fp-submit-btn" disabled={submitting}>
              {submitting ? (
                <>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="animate-spin">
                    <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                  </svg>
                  Sending Code...
                </>
              ) : (
                'Send Verification Code'
              )}
            </button>
          </form>

          {/* Back to Login Link */}
          <div className="fp-switch">
            Remembered your password?{' '}
            <Link to="/login" className="fp-switch__link">
              Back to Login
            </Link>
          </div>
        </div>
      </main>

      {/* ─── Footer ─── */}
      <footer className="fp-footer">
        &copy; {new Date().getFullYear()} CampusCart. Built by Students, For Students.
      </footer>
    </div>
  );
}
