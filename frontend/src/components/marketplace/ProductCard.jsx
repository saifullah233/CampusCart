import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';
import './ProductCard.css';

export default function ProductCard({
  product,
  isWishlisted = false,
  onWishlistToggle,
  showSellerActions = false,
  onEdit,
  onDelete,
  onMarkSold,
  onToggleActive,
}) {
  const navigate = useNavigate();
  const [wishlisted, setWishlisted] = useState(isWishlisted);
  const [wishlistLoading, setWishlistLoading] = useState(false);

  useEffect(() => {
    setWishlisted(isWishlisted);
  }, [isWishlisted]);

  if (!product) return null;

  const targetProductId = product.id || product.productId;

  // Derive cover image or first image
  const coverImage =
    product.imageUrl ||
    product.images?.find((img) => img.isCover)?.imageUrl ||
    product.images?.find((img) => img.isCover)?.url ||
    product.images?.[0]?.imageUrl ||
    product.images?.[0]?.url ||
    null;

  const handleCardClick = (e) => {
    // Avoid navigation if clicked inside an interactive button/action
    if (e.target.closest('button') || e.target.closest('a')) {
      return;
    }
    navigate(`/products/${targetProductId}`);
  };

  const handleWishlistClick = async (e) => {
    e.stopPropagation();
    if (wishlistLoading) return;

    // Check if user is logged in
    const token = localStorage.getItem('token') || localStorage.getItem('accessToken');
    if (!token) {
      navigate('/login', { state: { from: window.location.pathname } });
      return;
    }

    const nextState = !wishlisted;
    setWishlisted(nextState);
    setWishlistLoading(true);

    try {
      if (nextState) {
        await api.post(`/api/v1/wishlist/${targetProductId}`);
      } else {
        await api.delete(`/api/v1/wishlist/${targetProductId}`);
      }
      window.dispatchEvent(new CustomEvent('campuscart-wishlist-updated'));
      if (onWishlistToggle) {
        onWishlistToggle(targetProductId, nextState);
      }
    } catch {
      // Revert on failure
      setWishlisted(!nextState);
    } finally {
      setWishlistLoading(false);
    }
  };

  const formatPrice = (price) => {
    if (price === null || price === undefined) return '₹0';
    return `₹${Number(price).toLocaleString('en-IN')}`;
  };

  const isSold = product.status === 'SOLD';
  const isInactive = product.status === 'INACTIVE';
  const isSecondHand = product.productType === 'SECOND_HAND';

  return (
    <div
      className={`cc-product-card ${isSold ? 'cc-product-card--sold' : ''} ${isInactive ? 'cc-product-card--inactive' : ''}`}
      onClick={handleCardClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          handleCardClick(e);
        }
      }}
    >
      {/* Image Container */}
      <div className="cc-product-card__image-box">
        {coverImage ? (
          <img
            src={coverImage}
            alt={product.title}
            className="cc-product-card__img"
            loading="lazy"
          />
        ) : (
          <div className="cc-product-card__img-fallback">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <polyline points="21 15 16 10 5 21" />
            </svg>
            <span>No Image</span>
          </div>
        )}

        {/* Top Badges */}
        <div className="cc-product-card__badges-top">
          {isSold ? (
            <span className="cc-badge cc-badge--sold">SOLD</span>
          ) : isInactive ? (
            <span className="cc-badge cc-badge--inactive">INACTIVE</span>
          ) : (
            <span className={`cc-badge ${isSecondHand ? 'cc-badge--second-hand' : 'cc-badge--new'}`}>
              {isSecondHand ? 'SECOND HAND' : 'NEW'}
            </span>
          )}

          {product.sellingReach === 'CAMPUS_ONLY' && (
            <span className="cc-badge cc-badge--campus">Campus Only</span>
          )}
        </div>

        {/* Wishlist Button (hidden in seller actions mode) */}
        {!showSellerActions && (
          <button
            type="button"
            className={`cc-product-card__wishlist-btn ${wishlisted ? 'cc-product-card__wishlist-btn--active' : ''}`}
            onClick={handleWishlistClick}
            aria-label={wishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
            title={wishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill={wishlisted ? '#e11d48' : 'none'} stroke={wishlisted ? '#e11d48' : 'currentColor'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
            </svg>
          </button>
        )}
      </div>

      {/* Content Area */}
      <div className="cc-product-card__content">
        {/* Category & Date */}
        <div className="cc-product-card__meta-top">
          <span className="cc-product-card__category">{product.categoryName || 'General'}</span>
          {product.quantity > 1 && (
            <span className="cc-product-card__qty">{product.quantity} left</span>
          )}
        </div>

        {/* Title */}
        <h3 className="cc-product-card__title" title={product.title}>
          {product.title}
        </h3>

        {/* Price & Location */}
        <div className="cc-product-card__price-row">
          <span className="cc-product-card__price">{formatPrice(product.price)}</span>
        </div>

        {/* Footer: College / Seller / City */}
        <div className="cc-product-card__footer">
          <div className="cc-product-card__location" title={product.collegeName || product.cityName || 'Campus'}>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
              <circle cx="12" cy="10" r="3" />
            </svg>
            <span className="cc-product-card__location-text">
              {product.collegeName || product.cityName || 'Campus'}
            </span>
          </div>

          {product.sellerName && (
            <div className="cc-product-card__seller" title={`Seller: ${product.sellerName}`}>
              <span className="cc-product-card__seller-text">{product.sellerName}</span>
            </div>
          )}
        </div>

        {/* Seller Management Actions */}
        {showSellerActions && (
          <div className="cc-product-card__seller-actions" onClick={(e) => e.stopPropagation()}>
            <button
              type="button"
              className="cc-seller-act-btn cc-seller-act-btn--edit"
              onClick={() => onEdit && onEdit(product)}
            >
              Edit
            </button>
            {!isSold && (
              <button
                type="button"
                className="cc-seller-act-btn cc-seller-act-btn--sold"
                onClick={() => onMarkSold && onMarkSold(product)}
              >
                Mark Sold
              </button>
            )}
            <button
              type="button"
              className="cc-seller-act-btn cc-seller-act-btn--toggle"
              onClick={() => onToggleActive && onToggleActive(product)}
            >
              {isInactive ? 'Activate' : 'Deactivate'}
            </button>
            <button
              type="button"
              className="cc-seller-act-btn cc-seller-act-btn--delete"
              onClick={() => onDelete && onDelete(product)}
              aria-label="Delete listing"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="3 6 5 6 21 6" />
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              </svg>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
