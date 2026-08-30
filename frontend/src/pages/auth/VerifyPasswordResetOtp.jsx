import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import api from '../../services/api';
import campuscartSymbol from '../../assets/campuscart_symbol.png';
import csLogo from '../../assets/cs_logo.png';
import './VerifyPasswordResetOtp.css';

export default function VerifyPasswordResetOtp() {
  const navigate = useNavigate();
  const location = useLocation();

  // Load pending password reset session from location state or localStorage
  const [session, setSession] = useState(() => {
    const fromLoc = location.state;
    if (fromLoc?.challengeId) {
      return fromLoc;
    }
    try {
      const saved = localStorage.getItem('cc_forgot_password_session');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [submitting, setSubmitting] = useState(false);
  const [resending, setResending] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Countdown timers
  const [expirySec, setExpirySec] = useState(0);
  const [cooldownSec, setCooldownSec] = useState(0);

  const inputRefs = useRef([]);

  // ─── Initialize Countdown Timers ───
  useEffect(() => {
    if (!session) return;

    const updateTimers = () => {
      const now = Date.now();
      if (session.expiresAt) {
        const exp = new Date(session.expiresAt).getTime();
        setExpirySec(Math.max(0, Math.floor((exp - now) / 1000)));
      }
      if (session.nextResendAt) {
        const resend = new Date(session.nextResendAt).getTime();
        setCooldownSec(Math.max(0, Math.floor((resend - now) / 1000)));
      }
    };

    updateTimers();
    const interval = setInterval(updateTimers, 1000);
    return () => clearInterval(interval);
  }, [session]);

  // ─── Format Expiration Timer (mm:ss) ───
  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // ─── Handle Single Digit Change ───
  const handleInputChange = (index, e) => {
    const val = e.target.value;
    setError('');
    setSuccess('');

    const digit = val.replace(/\D/g, '').slice(-1);
    const newOtp = [...otp];
    newOtp[index] = digit;
    setOtp(newOtp);

    if (digit && index < 5 && inputRefs.current[index + 1]) {
      inputRefs.current[index + 1].focus();
    }
  };

  // ─── Keyboard Navigation ───
  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace') {
      if (!otp[index] && index > 0 && inputRefs.current[index - 1]) {
        inputRefs.current[index - 1].focus();
      }
    } else if (e.key === 'ArrowLeft' && index > 0) {
      inputRefs.current[index - 1]?.focus();
    } else if (e.key === 'ArrowRight' && index < 5) {
      inputRefs.current[index + 1]?.focus();
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (otp.join('').length === 6 && !submitting) {
        handleVerify();
      }
    }
  };

  // ─── Paste Handler ───
  const handlePaste = (e) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (!pasted) return;

    setError('');
    setSuccess('');

    const newOtp = [...otp];
    for (let i = 0; i < 6; i++) {
      newOtp[i] = pasted[i] || '';
    }
    setOtp(newOtp);

    const nextIndex = Math.min(pasted.length, 5);
    inputRefs.current[nextIndex]?.focus();
  };

  // ─── Verify OTP Submission ───
  const handleVerify = useCallback(async () => {
    const code = otp.join('');
    if (code.length !== 6) {
      setError('Please enter all 6 digits of the verification code.');
      return;
    }
    if (!session?.challengeId) {
      setError('Invalid verification session. Please request a new code.');
      return;
    }

    setSubmitting(true);
    setError('');
    setSuccess('');

    try {
      const res = await api.post('/api/v1/auth/forgot-password/verify-otp', {
        challengeId: session.challengeId,
        code,
      });

      if (res.success && res.data) {
        const { resetToken, expiresAt } = res.data;
        localStorage.removeItem('cc_forgot_password_session');
        sessionStorage.setItem('cc_reset_token', resetToken);
        navigate('/forgot-password/reset', {
          state: { resetToken, expiresAt },
          replace: true,
        });
      }
    } catch (err) {
      const msg = err?.message || err?.error?.detail || 'Invalid verification code. Please try again.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  }, [otp, session, navigate]);

  // ─── Auto-submit on 6th digit ───
  useEffect(() => {
    if (otp.join('').length === 6 && !submitting) {
      handleVerify();
    }
  }, [otp, submitting, handleVerify]);

  // ─── Resend Code ───
  const handleResend = async () => {
    if (cooldownSec > 0 || resending || !session?.challengeId) return;

    setResending(true);
    setError('');
    setSuccess('');

    try {
      const res = await api.post('/api/v1/auth/forgot-password/resend-otp', {
        challengeId: session.challengeId,
      });

      if (res.success && res.data) {
        const updated = {
          ...session,
          challengeId: res.data.challengeId,
          expiresAt: res.data.expiresAt,
          nextResendAt: res.data.nextResendAt,
        };
        setSession(updated);
        localStorage.setItem('cc_forgot_password_session', JSON.stringify(updated));
        setOtp(['', '', '', '', '', '']);
        setSuccess('A fresh 6-digit verification code has been sent to your email.');
        inputRefs.current[0]?.focus();
      }
    } catch (err) {
      const msg = err?.message || err?.error?.detail || 'Failed to resend code. Please try again.';
      setError(msg);
    } finally {
      setResending(false);
    }
  };

  const maskedDestination = session?.destination || 'your email';

  return (
    <div className="fp-otp-page">
      {/* ─── Header ─── */}
      <header className="fp-otp-header">
        <div className="fp-otp-header__left">
          <Link to="/" className="fp-otp-logo-slot" aria-label="CampusCart logo">
            <img src={campuscartSymbol} className="fp-otp-logo-img" alt="CampusCart" />
            <span className="fp-otp-logo-wordmark">
              Campus<span className="fp-otp-logo-wordmark--bold">Cart</span>
            </span>
          </Link>
        </div>
        <div className="fp-otp-header__right">
          <div className="fp-otp-slogan">
            <div>&ldquo;Built by Students,</div>
            <div className="fp-otp-slogan--highlight">For Students.&rdquo;</div>
          </div>
          <div className="fp-otp-header-divider" />
          <div className="fp-otp-cs-slot" aria-label="Campus By Students logo">
            <img src={csLogo} className="fp-otp-cs-logo" alt="Campus By Students" />
          </div>
        </div>
      </header>

      {/* ─── Back Action Bar ─── */}
      <div className="fp-otp-back-bar">
        <Link to="/forgot-password" className="fp-otp-back-link">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          Back to Forgot Password
        </Link>
      </div>

      {/* ─── Main Content ─── */}
      <main className="fp-otp-main">
        <div className="fp-otp-card">
          <div className="fp-otp-card-header">
            <div className="fp-otp-icon-wrapper">
              <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="2" y="4" width="20" height="16" rx="2" />
                <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
              </svg>
            </div>
            <h1 className="fp-otp-title">Enter Verification Code</h1>
            <p className="fp-otp-subtitle">
              We&apos;ve sent a 6-digit code to{' '}
              <strong className="fp-otp-highlight">{maskedDestination}</strong>
            </p>
          </div>

          {/* Success Banner */}
          {success && (
            <div className="fp-otp-alert fp-otp-alert--success">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
              <span>{success}</span>
            </div>
          )}

          {/* Error Banner */}
          {error && (
            <div className="fp-otp-alert fp-otp-alert--error">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="16" x2="12" y2="12" />
                <line x1="12" y1="8" x2="12.01" y2="8" />
              </svg>
              <span>{error}</span>
            </div>
          )}

          {/* Expiration Timer Indicator */}
          <div className="fp-otp-timer-badge">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
            {expirySec > 0 ? (
              <span>Code expires in <strong className="fp-otp-timer-val">{formatTime(expirySec)}</strong></span>
            ) : (
              <span className="fp-otp-timer-expired">Code has expired. Please resend.</span>
            )}
          </div>

          {/* 6-box OTP Input Grid */}
          <div className="fp-otp-grid">
            {otp.map((digit, i) => (
              <input
                key={i}
                ref={(el) => (inputRefs.current[i] = el)}
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                maxLength={1}
                className={`fp-otp-box ${digit ? 'fp-otp-box--filled' : ''} ${error ? 'fp-otp-box--error' : ''}`}
                value={digit}
                onChange={(e) => handleInputChange(i, e)}
                onKeyDown={(e) => handleKeyDown(i, e)}
                onPaste={handlePaste}
                autoFocus={i === 0}
                disabled={submitting}
                aria-label={`Digit ${i + 1}`}
              />
            ))}
          </div>

          {/* Verify Button */}
          <button
            type="button"
            className="fp-otp-submit-btn"
            onClick={handleVerify}
            disabled={submitting || otp.join('').length !== 6 || expirySec === 0}
          >
            {submitting ? (
              <>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="animate-spin">
                  <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                </svg>
                Verifying...
              </>
            ) : (
              'Verify Code'
            )}
          </button>

          {/* Resend Action */}
          <div className="fp-otp-resend">
            Didn&apos;t receive the code?{' '}
            {cooldownSec > 0 ? (
              <span className="fp-otp-resend__cooldown">
                Resend in <strong>{cooldownSec}s</strong>
              </span>
            ) : (
              <button
                type="button"
                className="fp-otp-resend__btn"
                onClick={handleResend}
                disabled={resending}
              >
                {resending ? 'Sending...' : 'Resend Code'}
              </button>
            )}
          </div>
        </div>
      </main>

      {/* ─── Footer ─── */}
      <footer className="fp-otp-footer">
        &copy; {new Date().getFullYear()} CampusCart. Built by Students, For Students.
      </footer>
    </div>
  );
}
