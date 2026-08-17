import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import campuscartSymbol from '../../assets/campuscart_symbol.png';
import csLogo from '../../assets/cs_logo.png';
import './VerifyOtp.css';

export default function VerifyOtp() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  // Load pending verification session from location state or localStorage
  const [session, setSession] = useState(() => {
    const fromLoc = location.state;
    if (fromLoc?.emailChallenge || fromLoc?.challengeId || fromLoc?.otp) {
      return fromLoc;
    }
    try {
      const saved = localStorage.getItem('cc_pending_verification');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  const emailChallenge = session?.emailChallenge || session?.otp || (session?.channel === 'EMAIL' ? session : null);

  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [submitting, setSubmitting] = useState(false);
  const [resending, setResending] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Countdown timers
  const [expirySec, setExpirySec] = useState(0);
  const [cooldownSec, setCooldownSec] = useState(0);

  const inputRefs = useRef([]);
  const returnPath = session?.returnPath || (session?.flow === 'community' ? '/register/community' : '/register/student');

  // ─── Initialize Countdown Timers ───
  useEffect(() => {
    if (!emailChallenge) return;

    const updateTimers = () => {
      const now = Date.now();
      if (emailChallenge.expiresAt) {
        const exp = new Date(emailChallenge.expiresAt).getTime();
        setExpirySec(Math.max(0, Math.floor((exp - now) / 1000)));
      }
      if (emailChallenge.nextResendAt) {
        const resend = new Date(emailChallenge.nextResendAt).getTime();
        setCooldownSec(Math.max(0, Math.floor((resend - now) / 1000)));
      }
    };

    updateTimers();
    const interval = setInterval(updateTimers, 1000);
    return () => clearInterval(interval);
  }, [emailChallenge]);

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
    if (!emailChallenge?.challengeId) {
      setError('Invalid verification session. Please return to registration.');
      return;
    }

    setSubmitting(true);
    setError('');
    setSuccess('');

    try {
      const res = await api.post('/api/v1/otp/verify', {
        challengeId: emailChallenge.challengeId,
        code,
      });

      if (res.success && res.data) {
        const { tokens, user } = res.data;

        // Successful email verification activates the account and issues tokens
        if (tokens?.accessToken) {
          localStorage.removeItem('cc_pending_verification');
          login(user, tokens.accessToken, tokens.refreshToken);
          navigate('/products', { replace: true });
          return;
        }

        setSuccess('Email verified successfully!');
      }
    } catch (err) {
      const msg = err?.message || err?.error?.detail || 'Invalid OTP. Please check the code and try again.';
      setError(msg);
      inputRefs.current[0]?.focus();
    } finally {
      setSubmitting(false);
    }
  }, [otp, emailChallenge, login, navigate]);

  // ─── Resend OTP ───
  const handleResend = async () => {
    if (cooldownSec > 0 || resending || !emailChallenge?.challengeId) return;

    setResending(true);
    setError('');
    setSuccess('');

    try {
      const res = await api.post('/api/v1/otp/resend', {
        challengeId: emailChallenge.challengeId,
      });

      if (res.success && res.data) {
        const updatedChal = {
          ...emailChallenge,
          challengeId: res.data.challengeId,
          expiresAt: res.data.expiresAt,
          nextResendAt: res.data.nextResendAt,
          destination: res.data.destination || emailChallenge.destination,
        };

        const updatedSession = {
          ...session,
          emailChallenge: updatedChal,
          challengeId: updatedChal.challengeId,
        };

        setSession(updatedSession);
        localStorage.setItem('cc_pending_verification', JSON.stringify(updatedSession));

        setOtp(['', '', '', '', '', '']);
        setSuccess('New email OTP sent successfully.');
        inputRefs.current[0]?.focus();
      }
    } catch (err) {
      const msg = err?.message || err?.error?.detail || 'Unable to resend OTP right now. Please try again.';
      setError(msg);
    } finally {
      setResending(false);
    }
  };

  // ─── Go Back ───
  const handleGoBack = () => {
    localStorage.removeItem('cc_pending_verification');
    navigate(returnPath);
  };

  // Fallback View If No Session Found
  if (!session || !emailChallenge) {
    return (
      <div className="otp-page">
        <header className="otp-header">
          <div className="otp-header__left">
            <Link to="/" className="otp-logo-slot">
              <img src={campuscartSymbol} className="otp-logo-img" alt="CampusCart" />
              <span className="otp-logo-wordmark">
                Campus<span className="otp-logo-wordmark--bold">Cart</span>
              </span>
            </Link>
          </div>
        </header>
        <main className="otp-main">
          <div className="otp-card otp-fallback">
            <div className="otp-icon-wrapper">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect width="18" height="11" x="3" y="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </div>
            <h2 className="otp-fallback-title">No Active Session</h2>
            <p className="otp-fallback-desc">We could not find an active registration verification session.</p>
            <Link to="/" className="otp-submit-btn" style={{ textDecoration: 'none' }}>
              Go to Registration
            </Link>
          </div>
        </main>
        <footer className="otp-footer">
          &copy; {new Date().getFullYear()} CampusCart. Built by Students, For Students.
        </footer>
      </div>
    );
  }

  const isComplete = otp.join('').length === 6;

  return (
    <div className="otp-page">
      {/* ─── Top Header ─── */}
      <header className="otp-header">
        <div className="otp-header__left">
          <Link to="/" className="otp-logo-slot" aria-label="CampusCart logo">
            <img src={campuscartSymbol} className="otp-logo-img" alt="CampusCart" />
            <span className="otp-logo-wordmark">
              Campus<span className="otp-logo-wordmark--bold">Cart</span>
            </span>
          </Link>
        </div>
        <div className="otp-header__right">
          <div className="otp-slogan">
            <div>&ldquo;Built by Students,</div>
            <div className="otp-slogan--highlight">For Students.&rdquo;</div>
          </div>
          <div className="otp-header-divider" />
          <div className="otp-cs-slot" aria-label="Campus By Students logo">
            <img src={csLogo} className="otp-cs-logo" alt="Campus By Students" />
          </div>
        </div>
      </header>

      {/* ─── Main Card ─── */}
      <main className="otp-main">
        <div className="otp-card">
          <div className="otp-icon-wrapper">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect width="20" height="16" x="2" y="4" rx="2"/>
              <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/>
            </svg>
          </div>

          <h1 className="otp-title">Verify Your Email</h1>
          <p className="otp-subtitle">
            Enter the 6-digit OTP sent to your email.
          </p>

          {emailChallenge?.destination && (
            <div className="otp-destination-pill">
              <span>OTP sent to</span>
              <strong>{emailChallenge.destination}</strong>
            </div>
          )}

          {/* Success Banner */}
          {success && (
            <div className="otp-alert otp-alert--success">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
              <span>{success}</span>
            </div>
          )}

          {/* Error Banner */}
          {error && (
            <div className="otp-alert otp-alert--error">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <span>{error}</span>
            </div>
          )}

          {/* ─── 6 OTP Input Boxes ─── */}
          <div className="otp-inputs-grid" onPaste={handlePaste}>
            {otp.map((digit, idx) => (
              <input
                key={idx}
                ref={(el) => (inputRefs.current[idx] = el)}
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                maxLength={1}
                value={digit}
                onChange={(e) => handleInputChange(idx, e)}
                onKeyDown={(e) => handleKeyDown(idx, e)}
                className={`otp-input-box ${digit ? 'otp-input-box--filled' : ''} ${error ? 'otp-input-box--error' : ''}`}
                aria-label={`Digit ${idx + 1}`}
                autoComplete="one-time-code"
                disabled={submitting}
              />
            ))}
          </div>

          {/* ─── Expiry & Resend Meta ─── */}
          <div className="otp-meta">
            <div className="otp-expiry-timer">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              {expirySec > 0 ? (
                <span>OTP expires in {formatTime(expirySec)}</span>
              ) : (
                <span className="otp-expiry-timer--expired">OTP has expired.</span>
              )}
            </div>

            <button
              type="button"
              onClick={handleResend}
              disabled={cooldownSec > 0 || resending}
              className="otp-resend-btn"
            >
              {resending ? 'Sending...' : cooldownSec > 0 ? `Resend in ${cooldownSec}s` : 'Resend OTP'}
            </button>
          </div>

          {/* ─── Verify CTA Button ─── */}
          <button
            type="button"
            onClick={handleVerify}
            disabled={!isComplete || submitting}
            className="otp-submit-btn"
          >
            {submitting ? (
              <>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="animate-spin">
                  <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                </svg>
                Verifying...
              </>
            ) : (
              'Verify Email OTP'
            )}
          </button>
        </div>
      </main>

      {/* ─── Footer ─── */}
      <footer className="otp-footer">
        &copy; {new Date().getFullYear()} CampusCart. Built by Students, For Students.
      </footer>
    </div>
  );
}
