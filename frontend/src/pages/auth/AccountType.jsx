import { useNavigate, Link } from 'react-router-dom';
import campuscartSymbol from '../../assets/campuscart_symbol.png';
import csLogo from '../../assets/cs_logo.png';
import './AccountType.css';

export default function AccountType() {
  const navigate = useNavigate();

  return (
    <div className="at-page">
      {/* ─── Header ─── */}
      <header className="at-header">
        <div className="at-header__left">
          <div className="at-logo-slot" aria-label="CampusCart logo">
            <img src={campuscartSymbol} className="at-logo-img" alt="CampusCart logo" />
            <span className="at-logo-wordmark">
              Campus<span className="at-logo-wordmark--bold">Cart</span>
            </span>
          </div>
        </div>

        <div className="at-header__right">
          <div className="at-slogan">
            <div>&ldquo;Built by Students,</div>
            <div className="at-slogan--highlight">For Students.&rdquo;</div>
          </div>
          <div className="at-header-divider" />
          <div className="at-cs-slot" aria-label="Campus By Students logo">
            <img src={csLogo} className="at-cs-logo" alt="Campus By Students logo" />
          </div>
        </div>
      </header>

      {/* ─── Main Content ─── */}
      <main className="at-main">
        {/* Left Panel — Desktop only */}
        <section className="at-left">
          <div className="at-left__content">
            <h1 className="at-welcome">
              Welcome to
              <br />
              <span className="at-welcome--brand">CampusCart</span>
            </h1>
            <p className="at-tagline">Buy • Sell • Connect on Campus</p>

            <ul className="at-features">
              <li className="at-feature">
                <div className="at-feature__icon">
                  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#2563EB" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                    <polyline points="9 12 11 14 15 10" />
                  </svg>
                </div>
                <div>
                  <h3 className="at-feature__title">Secure</h3>
                  <p className="at-feature__desc">
                    Your data is safe with us. We use advanced security to protect your information.
                  </p>
                </div>
              </li>

              <li className="at-feature">
                <div className="at-feature__icon">
                  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#2563EB" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                    <polyline points="22 4 12 14.01 9 11.01" />
                  </svg>
                </div>
                <div>
                  <h3 className="at-feature__title">Verified</h3>
                  <p className="at-feature__desc">
                    Every user and college is verified for a trusted marketplace experience.
                  </p>
                </div>
              </li>

              <li className="at-feature">
                <div className="at-feature__icon">
                  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#2563EB" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
                  </svg>
                </div>
                <div>
                  <h3 className="at-feature__title">Fast &amp; Easy</h3>
                  <p className="at-feature__desc">
                    Simple steps to get started and explore amazing opportunities.
                  </p>
                </div>
              </li>
            </ul>
          </div>

          {/* Campus skyline illustration area */}
          <div className="at-illustration">
            {/* ASSET SLOT: Campus buildings / skyline vector illustration.
                Place the actual image here later.
                The container is sized/positioned to match the reference. */}
            <svg className="at-illustration__svg" viewBox="0 0 520 140" preserveAspectRatio="xMidYMax meet" fill="none">
              {/* Ground line */}
              <rect x="0" y="120" width="520" height="20" rx="0" fill="#DBEAFE" opacity="0.5" />
              {/* Trees */}
              <circle cx="30" cy="100" r="16" fill="#93C5FD" opacity="0.6" />
              <rect x="28" y="112" width="4" height="14" rx="1" fill="#60A5FA" />
              <circle cx="80" cy="105" r="12" fill="#BFDBFE" opacity="0.7" />
              <rect x="78" y="114" width="4" height="10" rx="1" fill="#93C5FD" />
              <circle cx="440" cy="100" r="14" fill="#93C5FD" opacity="0.6" />
              <rect x="438" y="112" width="4" height="14" rx="1" fill="#60A5FA" />
              <circle cx="490" cy="105" r="12" fill="#BFDBFE" opacity="0.7" />
              <rect x="488" y="114" width="4" height="10" rx="1" fill="#93C5FD" />
              {/* Left buildings */}
              <rect x="110" y="80" width="50" height="40" rx="2" fill="#DBEAFE" />
              <rect x="115" y="86" width="10" height="8" rx="1" fill="#fff" />
              <rect x="130" y="86" width="10" height="8" rx="1" fill="#fff" />
              <rect x="145" y="86" width="10" height="8" rx="1" fill="#fff" />
              <rect x="115" y="100" width="10" height="8" rx="1" fill="#fff" />
              <rect x="130" y="100" width="10" height="8" rx="1" fill="#fff" />
              <rect x="145" y="100" width="10" height="8" rx="1" fill="#fff" />
              {/* Center building (college/tower) */}
              <rect x="220" y="55" width="80" height="65" rx="2" fill="#BFDBFE" />
              <polygon points="260,28 230,55 290,55" fill="#3B82F6" />
              <circle cx="260" cy="72" r="8" fill="#fff" />
              <circle cx="260" cy="72" r="3" fill="#3B82F6" />
              <rect x="230" y="80" width="12" height="10" rx="1" fill="#fff" />
              <rect x="248" y="80" width="12" height="10" rx="1" fill="#fff" />
              <rect x="266" y="80" width="12" height="10" rx="1" fill="#fff" />
              <rect x="248" y="96" width="12" height="10" rx="1" fill="#fff" />
              <rect x="253" y="108" width="14" height="12" rx="1" fill="#EFF6FF" />
              {/* Right buildings */}
              <rect x="360" y="80" width="50" height="40" rx="2" fill="#DBEAFE" />
              <rect x="365" y="86" width="10" height="8" rx="1" fill="#fff" />
              <rect x="380" y="86" width="10" height="8" rx="1" fill="#fff" />
              <rect x="395" y="86" width="10" height="8" rx="1" fill="#fff" />
              <rect x="365" y="100" width="10" height="8" rx="1" fill="#fff" />
              <rect x="380" y="100" width="10" height="8" rx="1" fill="#fff" />
              <rect x="395" y="100" width="10" height="8" rx="1" fill="#fff" />
              {/* Birds */}
              <path d="M150 30 Q155 24 160 30 Q165 24 170 30" stroke="#BFDBFE" strokeWidth="1.5" fill="none" strokeLinecap="round" />
              <path d="M350 40 Q354 35 358 40 Q362 35 366 40" stroke="#BFDBFE" strokeWidth="1.5" fill="none" strokeLinecap="round" />
              <path d="M410 20 Q413 16 416 20 Q419 16 422 20" stroke="#BFDBFE" strokeWidth="1.5" fill="none" strokeLinecap="round" />
            </svg>
          </div>
        </section>

        {/* Right Panel — Selection cards */}
        <section className="at-right">
          {/* Mobile-only welcome */}
          <div className="at-mobile-welcome">
            <h1 className="at-welcome at-welcome--mobile">
              Welcome to <span className="at-welcome--brand">CampusCart</span>
            </h1>
            <p className="at-tagline at-tagline--mobile">Buy • Sell • Connect on Campus</p>
          </div>

          <div className="at-cards">
            {/* Student card */}
            <button
              type="button"
              className="at-card"
              onClick={() => navigate('/register/student')}
            >
              <div className="at-card__icon">
                <svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="#2563EB" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
                  <path d="M6 12v5c0 2 2 3 6 3s6-1 6-3v-5" />
                </svg>
              </div>
              <div className="at-card__body">
                <span className="at-card__label">Continue as</span>
                <h2 className="at-card__title">College Student</h2>
                <p className="at-card__desc">
                  Verify with your official college email
                  <br />
                  and access your campus marketplace.
                </p>
              </div>
              <div className="at-card__arrow">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="5" y1="12" x2="19" y2="12" />
                  <polyline points="12 5 19 12 12 19" />
                </svg>
              </div>
            </button>

            {/* Community card */}
            <button
              type="button"
              className="at-card"
              onClick={() => navigate('/register/community')}
            >
              <div className="at-card__icon">
                <svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="#2563EB" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="2" y1="12" x2="22" y2="12" />
                  <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
                </svg>
              </div>
              <div className="at-card__body">
                <span className="at-card__label">Continue as</span>
                <h2 className="at-card__title">Community User</h2>
                <p className="at-card__desc">
                  Buy products and sell
                  <br />
                  your own handmade items.
                </p>
              </div>
              <div className="at-card__arrow">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="5" y1="12" x2="19" y2="12" />
                  <polyline points="12 5 19 12 12 19" />
                </svg>
              </div>
            </button>
          </div>

          {/* Security trust banner */}
          <div className="at-trust">
            <div className="at-trust__shield">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                <polyline points="9 12 11 14 15 10" />
              </svg>
            </div>
            <p className="at-trust__text">
              CampusCart protects every user with
              <br />
              <span className="at-trust__link">secure verification</span> &amp;{' '}
              <span className="at-trust__link">trusted marketplace.</span>
            </p>
          </div>

          <div className="at-login-prompt">
            Already have an account?{' '}
            <Link to="/login" className="at-login-link">
              Log in
            </Link>
          </div>
        </section>
      </main>

      {/* ─── Footer ─── */}
      <footer className="at-footer">
        <div className="at-footer__links">
          <a href="#tos" className="at-footer__link">Terms of Service</a>
          <span className="at-footer__dot">•</span>
          <a href="#privacy" className="at-footer__link">Privacy Policy</a>
        </div>
        <p className="at-footer__copy">© CampusCart 2025. All Rights Reserved.</p>
      </footer>
    </div>
  );
}
