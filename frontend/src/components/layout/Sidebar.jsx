import { useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import campuscartSymbol from '../../assets/campuscart_symbol.png';
import './Sidebar.css';

export default function Sidebar({ isOpen, onClose, onOpenSell }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // Prevent background scrolling when mobile drawer is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isVerifiedStudent = user?.accountType === 'STUDENT' && (user?.collegeName || user?.collegeId);

  return (
    <>
      <div
        className={`cc-sidebar-overlay ${isOpen ? 'cc-sidebar-overlay--open' : ''}`}
        onClick={onClose}
        aria-hidden="true"
      />

      <aside className={`cc-sidebar ${isOpen ? 'cc-sidebar--open' : ''}`}>
        {/* Mobile-only header with logo and close button */}
        <div className="cc-sidebar__mobile-header">
          <div className="cc-sidebar__mobile-logo">
            <img src={campuscartSymbol} alt="CampusCart" className="cc-sidebar__mobile-logo-img" />
            <span className="cc-sidebar__mobile-wordmark">
              Campus<span>Cart</span>
            </span>
          </div>
          <button
            type="button"
            className="cc-sidebar__close-btn"
            onClick={onClose}
            aria-label="Close navigation menu"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <nav className="cc-sidebar__nav">
          {/* Main Navigation */}
          <NavLink
            to="/products"
            end
            className={({ isActive }) =>
              `cc-sidebar__item ${isActive ? 'cc-sidebar__item--active' : ''}`
            }
            onClick={onClose}
          >
            <div className="cc-sidebar__item-icon">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                <polyline points="9 22 9 12 15 12 15 22" />
              </svg>
            </div>
            <span>Home</span>
          </NavLink>

          <NavLink
            to="/browse"
            className={({ isActive }) =>
              `cc-sidebar__item ${isActive ? 'cc-sidebar__item--active' : ''}`
            }
            onClick={onClose}
          >
            <div className="cc-sidebar__item-icon">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </div>
            <span>Browse</span>
          </NavLink>

          <NavLink
            to="/orders"
            className={({ isActive }) =>
              `cc-sidebar__item ${isActive ? 'cc-sidebar__item--active' : ''}`
            }
            onClick={onClose}
          >
            <div className="cc-sidebar__item-icon">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
                <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
                <line x1="12" y1="22.08" x2="12" y2="12" />
              </svg>
            </div>
            <span>My Orders</span>
          </NavLink>

          <NavLink
            to="/wishlist"
            className={({ isActive }) =>
              `cc-sidebar__item ${isActive ? 'cc-sidebar__item--active' : ''}`
            }
            onClick={onClose}
          >
            <div className="cc-sidebar__item-icon">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
              </svg>
            </div>
            <span>Wishlist</span>
          </NavLink>

          <NavLink
            to="/chat"
            className={({ isActive }) =>
              `cc-sidebar__item ${isActive ? 'cc-sidebar__item--active' : ''}`
            }
            onClick={onClose}
          >
            <div className="cc-sidebar__item-icon">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              </svg>
            </div>
            <span>Messages</span>
          </NavLink>

          {/* Section: SELL */}
          <div className="cc-sidebar__section-title">SELL</div>

          <button
            type="button"
            className="cc-sidebar__item"
            onClick={() => {
              onClose();
              if (onOpenSell) {
                onOpenSell();
              } else {
                navigate('/sell');
              }
            }}
          >
            <div className="cc-sidebar__item-icon">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="16" />
                <line x1="8" y1="12" x2="16" y2="12" />
              </svg>
            </div>
            <span>Sell an Item</span>
          </button>

          <NavLink
            to="/my-listings"
            className={({ isActive }) =>
              `cc-sidebar__item ${isActive ? 'cc-sidebar__item--active' : ''}`
            }
            onClick={onClose}
          >
            <div className="cc-sidebar__item-icon">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" />
                <line x1="7" y1="7" x2="7.01" y2="7" />
              </svg>
            </div>
            <span>My Listings</span>
          </NavLink>

          {/* Section: ACCOUNT */}
          <div className="cc-sidebar__section-title">ACCOUNT</div>

          <NavLink
            to="/profile"
            className={({ isActive }) =>
              `cc-sidebar__item ${isActive ? 'cc-sidebar__item--active' : ''}`
            }
            onClick={onClose}
          >
            <div className="cc-sidebar__item-icon">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
            </div>
            <span>Profile</span>
          </NavLink>

          <NavLink
            to="/settings"
            className={({ isActive }) =>
              `cc-sidebar__item ${isActive ? 'cc-sidebar__item--active' : ''}`
            }
            onClick={onClose}
          >
            <div className="cc-sidebar__item-icon">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="3" />
                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
              </svg>
            </div>
            <span>Settings</span>
          </NavLink>

          <button
            type="button"
            className="cc-sidebar__item cc-sidebar__item--logout"
            onClick={() => {
              onClose();
              handleLogout();
            }}
          >
            <div className="cc-sidebar__item-icon">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
              </svg>
            </div>
            <span>Logout</span>
          </button>
        </nav>

        {/* Bottom Verification Card */}
        <div className="cc-sidebar__bottom">
          {isVerifiedStudent ? (
            <div className="cc-sidebar__verified-badge">
              <div className="cc-sidebar__verified-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                  <polyline points="22 4 12 14.01 9 11.01" />
                </svg>
              </div>
              <div className="cc-sidebar__verified-info">
                <div className="cc-sidebar__verified-title">Verified Student</div>
                <div className="cc-sidebar__verified-college" title={user.collegeName}>
                  {user.collegeName || 'CampusCart'}
                </div>
              </div>
            </div>
          ) : (
            <div className="cc-sidebar__verify-card">
              <div className="cc-sidebar__verify-title">Are you a College Student?</div>
              <div className="cc-sidebar__verify-desc">
                Verify your college and unlock all campus features.
              </div>
              <button
                type="button"
                className="cc-sidebar__verify-btn"
                onClick={() => {
                  onClose();
                  navigate('/register/student');
                }}
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
                  <path d="M6 12v5c0 2 2 3 6 3s6-1 6-3v-5" />
                </svg>
                Verify Now
              </button>
            </div>
          )}
        </div>
      </aside>
    </>
  );
}
