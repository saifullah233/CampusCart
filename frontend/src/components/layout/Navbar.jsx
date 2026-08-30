import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import campuscartSymbol from '../../assets/campuscart_symbol.png';
import './Navbar.css';

export default function Navbar({ onToggleSidebar, onSearch, searchQuery }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [unreadNotifications, setUnreadNotifications] = useState(0);
  const [cartItemCount, setCartItemCount] = useState(0);
  const [wishlistCount, setWishlistCount] = useState(0);
  const searchInputRef = useRef(null);
  const dropdownRef = useRef(null);

  // Keyboard shortcut '/' to focus search input
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === '/' && document.activeElement !== searchInputRef.current) {
        e.preventDefault();
        searchInputRef.current?.focus();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Close dropdown on click outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Fetch real counts from backend
  const fetchCounts = () => {
    // Fetch unread notifications count
    api.get('/api/v1/notifications/unread-count')
      .then((res) => {
        if (res.success && typeof res.data === 'number') {
          setUnreadNotifications(res.data);
        }
      })
      .catch(() => {});

    // Fetch cart count
    api.get('/api/v1/cart')
      .then((res) => {
        if (res.success && res.data?.items) {
          setCartItemCount(res.data.items.length);
        }
      })
      .catch(() => {});

    // Fetch wishlist count
    api.get('/api/v1/wishlist?page=0&size=1')
      .then((res) => {
        if (res.success && typeof res.data?.totalElements === 'number') {
          setWishlistCount(res.data.totalElements);
        }
      })
      .catch(() => {});
  };

  useEffect(() => {
    fetchCounts();

    const handleCartUpdate = () => fetchCounts();
    const handleUnreadUpdate = () => fetchCounts();
    const handleWishlistUpdate = () => fetchCounts();

    window.addEventListener('campuscart-cart-updated', handleCartUpdate);
    window.addEventListener('campuscart-unread-updated', handleUnreadUpdate);
    window.addEventListener('campuscart-wishlist-updated', handleWishlistUpdate);

    return () => {
      window.removeEventListener('campuscart-cart-updated', handleCartUpdate);
      window.removeEventListener('campuscart-unread-updated', handleUnreadUpdate);
      window.removeEventListener('campuscart-wishlist-updated', handleWishlistUpdate);
    };
  }, []);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (onSearch) {
      onSearch(searchQuery);
    }
  };

  const getUserInitials = () => {
    if (user?.fullName) {
      const parts = user.fullName.trim().split(' ');
      if (parts.length >= 2) return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
      return parts[0].substring(0, 2).toUpperCase();
    }
    if (user?.email) {
      return user.email.substring(0, 2).toUpperCase();
    }
    return 'CC';
  };

  return (
    <header className="cc-navbar">
      {/* Left Logo and Mobile Toggle */}
      <div className="cc-navbar__left">
        <button
          type="button"
          className="cc-navbar__menu-btn"
          onClick={onToggleSidebar}
          aria-label="Toggle navigation menu"
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>

        <Link to="/products" className="cc-navbar__logo-link">
          <img src={campuscartSymbol} className="cc-navbar__logo-img" alt="CampusCart" />
          <span className="cc-navbar__logo-wordmark">
            Campus<span className="cc-navbar__logo-wordmark--bold">Cart</span>
          </span>
        </Link>
      </div>

      {/* Center Search Bar */}
      <div className="cc-navbar__center">
        <form className="cc-navbar__search" onSubmit={handleSearchSubmit}>
          <div className="cc-navbar__search-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </div>
          <input
            ref={searchInputRef}
            type="text"
            className="cc-navbar__search-input"
            placeholder="Search for products, categories or users..."
            value={searchQuery || ''}
            onChange={(e) => onSearch && onSearch(e.target.value)}
          />
          <kbd className="cc-navbar__search-badge">/</kbd>
        </form>
      </div>

      {/* Right Action Icons & Profile */}
      <div className="cc-navbar__right">
        {/* Wishlist */}
        <Link to="/marketplace/wishlist" className="cc-navbar__action-btn" aria-label="Wishlist" title="My Wishlist">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
          </svg>
          {wishlistCount > 0 && <span className="cc-navbar__badge">{wishlistCount}</span>}
        </Link>

        {/* Cart */}
        <Link to="/cart" className="cc-navbar__action-btn" aria-label="Shopping Cart">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="9" cy="21" r="1" />
            <circle cx="20" cy="21" r="1" />
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
          </svg>
          {cartItemCount > 0 && <span className="cc-navbar__badge">{cartItemCount}</span>}
        </Link>

        {/* Messages */}
        <Link to="/chat" className="cc-navbar__action-btn" aria-label="Messages">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
        </Link>

        {/* Notifications */}
        <button type="button" className="cc-navbar__action-btn" aria-label="Notifications">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.73 21a2 2 0 0 1-3.46 0" />
          </svg>
          {unreadNotifications > 0 && <span className="cc-navbar__badge">{unreadNotifications}</span>}
        </button>

        {/* User Profile */}
        <div className="cc-navbar__profile" ref={dropdownRef}>
          <button
            type="button"
            className="cc-navbar__profile-trigger"
            onClick={() => setDropdownOpen(!dropdownOpen)}
            aria-expanded={dropdownOpen}
            aria-label="User menu"
          >
            <div className="cc-navbar__avatar">
              {getUserInitials()}
            </div>
            <svg
              className={`cc-navbar__chevron ${dropdownOpen ? 'cc-navbar__chevron--open' : ''}`}
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </button>

          {dropdownOpen && (
            <div className="cc-navbar__dropdown">
              <div className="cc-navbar__dropdown-header">
                <div className="cc-navbar__dropdown-name">{user?.fullName || 'CampusCart User'}</div>
                <div className="cc-navbar__dropdown-email">{user?.email}</div>
              </div>

              <Link
                to="/marketplace/wishlist"
                className="cc-navbar__dropdown-item"
                onClick={() => setDropdownOpen(false)}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                </svg>
                My Wishlist
              </Link>

              <Link
                to="/orders"
                className="cc-navbar__dropdown-item"
                onClick={() => setDropdownOpen(false)}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
                  <line x1="3" y1="6" x2="21" y2="6" />
                  <path d="M16 10a4 4 0 0 1-8 0" />
                </svg>
                My Orders
              </Link>

              <Link
                to="/profile"
                className="cc-navbar__dropdown-item"
                onClick={() => setDropdownOpen(false)}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
                Profile
              </Link>

              <Link
                to="/settings"
                className="cc-navbar__dropdown-item"
                onClick={() => setDropdownOpen(false)}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="3" />
                  <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
                </svg>
                Settings
              </Link>

              <button
                type="button"
                className="cc-navbar__dropdown-item cc-navbar__dropdown-item--logout"
                onClick={() => {
                  setDropdownOpen(false);
                  logout();
                  navigate('/login', { replace: true });
                }}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                  <polyline points="16 17 21 12 16 7" />
                  <line x1="21" y1="12" x2="9" y2="12" />
                </svg>
                Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
