import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import api from '../../services/api';
import './Wishlist.css';

export default function Wishlist() {
  const navigate = useNavigate();

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Per-item action state
  const [cartAddingId, setCartAddingId] = useState(null);
  const [removingId, setRemovingId] = useState(null);
  const [toastMessage, setToastMessage] = useState('');

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(''), 3500);
  };

  // Fetch wishlisted items
  const fetchWishlist = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await api.get(`/api/v1/wishlist?page=${page}&size=20`);
      if (res.success && res.data) {
        setItems(res.data.content || []);
        setTotalPages(res.data.totalPages || 0);
        setTotalElements(res.data.totalElements || 0);
      } else {
        setItems([]);
        setTotalPages(0);
        setTotalElements(0);
      }
    } catch (err) {
      setError(err?.message || err?.error?.detail || 'Unable to load your wishlist. Please try again.');
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    fetchWishlist();
  }, [fetchWishlist]);

  // Remove from wishlist
  const handleRemove = async (productId) => {
    setRemovingId(productId);
    try {
      const res = await api.delete(`/api/v1/wishlist/${productId}`);
      if (res.success) {
        showToast('Item removed from wishlist.');
        // Optimistic local state update
        setItems((prev) => prev.filter((it) => it.productId !== productId));
        setTotalElements((prev) => Math.max(0, prev - 1));
        window.dispatchEvent(new CustomEvent('campuscart-wishlist-updated'));
      }
    } catch (err) {
      showToast(err?.message || 'Failed to remove item.');
      fetchWishlist();
    } finally {
      setRemovingId(null);
    }
  };

  // Add to cart from wishlist
  const handleAddToCart = async (item) => {
    if (item.status !== 'ACTIVE' || item.availableQuantity < 1) {
      showToast('This product is currently unavailable.');
      return;
    }

    setCartAddingId(item.productId);
    try {
      const res = await api.post('/api/v1/cart/items', {
        productId: item.productId,
        quantity: 1,
      });
      if (res.success) {
        showToast(`"${item.title}" added to your cart!`);
        window.dispatchEvent(new CustomEvent('campuscart-cart-updated'));
      }
    } catch (err) {
      showToast(err?.message || 'Failed to add product to cart.');
    } finally {
      setCartAddingId(null);
    }
  };

  const formatPrice = (price) => {
    if (price === null || price === undefined) return '₹0';
    return `₹${Number(price).toLocaleString('en-IN')}`;
  };

  return (
    <DashboardLayout>
      <div className="cc-wishlist-page">
        {/* Header */}
        <div className="cc-wishlist-header">
          <div>
            <div className="cc-wishlist-title-row">
              <h1 className="cc-wishlist-title">My Wishlist</h1>
              {!loading && totalElements > 0 && (
                <span className="cc-wishlist-count-badge">
                  {totalElements} item{totalElements === 1 ? '' : 's'}
                </span>
              )}
            </div>
            <p className="cc-wishlist-subtitle">
              Saved items you want to keep an eye on, compare, or purchase later.
            </p>
          </div>

          <Link to="/browse" className="cc-wishlist-explore-link">
            &larr; Explore More Items
          </Link>
        </div>

        {/* Toast Alert */}
        {toastMessage && (
          <div className="cc-wishlist-toast">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
            <span>{toastMessage}</span>
          </div>
        )}

        {/* Loading State */}
        {loading ? (
          <div className="cc-wishlist-grid">
            {Array.from({ length: 6 }).map((_, idx) => (
              <div key={idx} className="cc-wishlist-skeleton" />
            ))}
          </div>
        ) : error ? (
          /* Error State */
          <div className="cc-wishlist-error-card">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#dc2626" strokeWidth="1.5">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <h3>Unable to load your wishlist</h3>
            <p>{error}</p>
            <button type="button" className="cc-wishlist-btn-retry" onClick={fetchWishlist}>
              Retry
            </button>
          </div>
        ) : items.length === 0 ? (
          /* Empty State */
          <div className="cc-wishlist-empty">
            <div className="cc-wishlist-empty__icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#2563eb" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
              </svg>
            </div>
            <h2>No saved products yet.</h2>
            <p>
              Browse our college and community marketplace to save your favorite textbooks, electronics, and campus gear.
            </p>
            <Link to="/browse" className="cc-wishlist-btn-primary">
              Explore Marketplace
            </Link>
          </div>
        ) : (
          /* Wishlist Grid */
          <div className="cc-wishlist-grid">
            {items.map((item) => {
              const isAvailable = item.status === 'ACTIVE' && item.availableQuantity > 0;
              const isAdding = cartAddingId === item.productId;
              const isRemoving = removingId === item.productId;

              return (
                <div
                  key={item.productId}
                  className={`cc-wishlist-card ${!isAvailable ? 'cc-wishlist-card--unavailable' : ''}`}
                >
                  {/* Image Container */}
                  <div
                    className="cc-wishlist-card__image-box"
                    onClick={() => navigate(`/products/${item.productId}`)}
                  >
                    {item.imageUrl ? (
                      <img src={item.imageUrl} alt={item.title} className="cc-wishlist-card__img" />
                    ) : (
                      <div className="cc-wishlist-card__img-fallback">
                        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2">
                          <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                          <circle cx="8.5" cy="8.5" r="1.5" />
                          <polyline points="21 15 16 10 5 21" />
                        </svg>
                        <span>No Image</span>
                      </div>
                    )}

                    {/* Top Badges */}
                    <div className="cc-wishlist-card__badges">
                      {!isAvailable ? (
                        <span className="cc-wbadge cc-wbadge--unavailable">Currently unavailable</span>
                      ) : (
                        <span className={`cc-wbadge ${item.productType === 'SECOND_HAND' ? 'cc-wbadge--second' : 'cc-wbadge--new'}`}>
                          {item.productType === 'SECOND_HAND' ? 'SECOND HAND' : 'NEW'}
                        </span>
                      )}

                      {item.sellingReach === 'CAMPUS_ONLY' && (
                        <span className="cc-wbadge cc-wbadge--campus">Campus Only</span>
                      )}
                    </div>

                    {/* Remove Icon Button */}
                    <button
                      type="button"
                      className="cc-wishlist-card__remove-icon-btn"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleRemove(item.productId);
                      }}
                      disabled={isRemoving}
                      title="Remove from wishlist"
                      aria-label="Remove from wishlist"
                    >
                      <svg width="17" height="17" viewBox="0 0 24 24" fill="#e11d48" stroke="#e11d48" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                      </svg>
                    </button>
                  </div>

                  {/* Content */}
                  <div className="cc-wishlist-card__body">
                    <span className="cc-wishlist-card__category">{item.categoryName || 'General'}</span>

                    <h3
                      className="cc-wishlist-card__title"
                      title={item.title}
                      onClick={() => navigate(`/products/${item.productId}`)}
                    >
                      {item.title}
                    </h3>

                    <div className="cc-wishlist-card__price">
                      {formatPrice(item.price)}
                    </div>

                    <div className="cc-wishlist-card__meta">
                      <div className="cc-wishlist-card__location">
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                          <circle cx="12" cy="10" r="3" />
                        </svg>
                        <span>{item.collegeName || item.cityName || 'Campus'}</span>
                      </div>

                      {item.sellerName && (
                        <span className="cc-wishlist-card__seller">
                          by <strong>{item.sellerName}</strong>
                        </span>
                      )}
                    </div>

                    {/* Actions */}
                    <div className="cc-wishlist-card__actions">
                      <button
                        type="button"
                        className="cc-wishlist-btn-cart"
                        onClick={() => handleAddToCart(item)}
                        disabled={!isAvailable || isAdding}
                      >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <circle cx="9" cy="21" r="1" />
                          <circle cx="20" cy="21" r="1" />
                          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
                        </svg>
                        <span>{isAdding ? 'Adding...' : isAvailable ? 'Add to Cart' : 'Unavailable'}</span>
                      </button>

                      <button
                        type="button"
                        className="cc-wishlist-btn-remove"
                        onClick={() => handleRemove(item.productId)}
                        disabled={isRemoving}
                      >
                        Remove
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Pagination */}
        {!loading && totalPages > 1 && (
          <div className="cc-pagination">
            <button
              type="button"
              className="cc-pagination__btn"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              &larr; Previous
            </button>
            <span className="cc-pagination__current-page">
              Page {page + 1} of {totalPages}
            </span>
            <button
              type="button"
              className="cc-pagination__btn"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            >
              Next &rarr;
            </button>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
