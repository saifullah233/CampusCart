import { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import DashboardLayout from '../../components/layout/DashboardLayout';
import ProductImageGallery from '../../components/marketplace/ProductImageGallery';
import SellModal from '../../components/marketplace/SellModal';
import api from '../../services/api';
import './ProductDetails.css';

export default function ProductDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [product, setProduct] = useState(null);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Interaction states
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [wishlistLoading, setWishlistLoading] = useState(false);
  const [cartAdding, setCartAdding] = useState(false);
  const [cartSuccess, setCartSuccess] = useState(false);
  const [chatStarting, setChatStarting] = useState(false);

  // Seller Action states
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);

  const fetchProduct = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await api.get(`/api/v1/products/${id}`);
      if (res.success && res.data) {
        setProduct(res.data);
      } else {
        setError('Product not found or unavailable in your marketplace scope.');
      }
    } catch (err) {
      const msg = err?.message || err?.error?.detail || 'Unable to load product details.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchProduct();
  }, [fetchProduct]);

  // Check wishlist status
  useEffect(() => {
    let cancelled = false;
    if (id) {
      api.get(`/api/v1/wishlist/check/${id}`)
        .then((res) => {
          if (!cancelled && res.success && typeof res.data === 'boolean') {
            setIsWishlisted(res.data);
          }
        })
        .catch(() => {});
    }
    return () => { cancelled = true; };
  }, [id]);

  // Load categories for edit modal
  useEffect(() => {
    let cancelled = false;
    api.get('/api/v1/categories')
      .then((res) => {
        if (!cancelled && res.success && Array.isArray(res.data)) {
          setCategories(res.data);
        }
      })
      .catch(() => {});
    return () => { cancelled = true; };
  }, []);

  const isOwner = user?.id && product?.sellerId && user.id === product.sellerId;

  // Wishlist toggle
  const handleWishlistToggle = async () => {
    if (!product || wishlistLoading) return;
    const next = !isWishlisted;
    setIsWishlisted(next);
    setWishlistLoading(true);
    try {
      if (next) {
        await api.post(`/api/v1/wishlist/${product.id}`);
      } else {
        await api.delete(`/api/v1/wishlist/${product.id}`);
      }
    } catch {
      setIsWishlisted(!next);
    } finally {
      setWishlistLoading(false);
    }
  };

  // Add to cart
  const handleAddToCart = async () => {
    if (!product || cartAdding) return;
    setCartAdding(true);
    setCartSuccess(false);
    setActionError('');
    try {
      const res = await api.post('/api/v1/cart/items', {
        productId: product.id,
        quantity: 1,
      });
      if (res.success) {
        setCartSuccess(true);
        setTimeout(() => setCartSuccess(false), 3500);
      }
    } catch (err) {
      const msg = err?.message || err?.error?.detail || 'Failed to add item to cart.';
      setActionError(msg);
    } finally {
      setCartAdding(false);
    }
  };

  // Chat with Seller
  const handleChatSeller = async () => {
    if (!product || chatStarting) return;
    setChatStarting(true);
    setActionError('');
    try {
      const res = await api.post(`/api/v1/conversations?productId=${product.id}`);
      if (res.success && res.data) {
        navigate(`/chat?conversationId=${res.data.id || res.data.conversationId}`);
      } else {
        navigate('/chat');
      }
    } catch (err) {
      const msg = err?.message || err?.error?.detail || 'Unable to open chat conversation.';
      setActionError(msg);
    } finally {
      setChatStarting(false);
    }
  };

  // Seller: Mark as Sold
  const handleMarkSold = async () => {
    setActionLoading(true);
    setActionError('');
    try {
      const res = await api.post(`/api/v1/products/${product.id}/sold`);
      if (res.success && res.data) {
        setProduct(res.data);
        setActionSuccess('Product marked as sold.');
      }
    } catch (err) {
      setActionError(err?.message || 'Failed to update product status.');
    } finally {
      setActionLoading(false);
    }
  };

  // Seller: Toggle Active / Deactive
  const handleToggleActive = async () => {
    setActionLoading(true);
    setActionError('');
    const endpoint = product.status === 'INACTIVE' ? 'activate' : 'deactivate';
    try {
      const res = await api.post(`/api/v1/products/${product.id}/${endpoint}`);
      if (res.success && res.data) {
        setProduct(res.data);
        setActionSuccess(`Product ${endpoint}d successfully.`);
      }
    } catch (err) {
      setActionError(err?.message || 'Failed to update product status.');
    } finally {
      setActionLoading(false);
    }
  };

  // Seller: Delete Listing
  const handleDelete = async () => {
    setActionLoading(true);
    setActionError('');
    try {
      const res = await api.delete(`/api/v1/products/${product.id}`);
      if (res.success) {
        navigate('/my-listings');
      }
    } catch (err) {
      setActionError(err?.message || 'Failed to delete listing.');
      setDeleteConfirmOpen(false);
    } finally {
      setActionLoading(false);
    }
  };

  const handleProductUpdated = (updated) => {
    setProduct(updated);
    setActionSuccess('Listing updated successfully.');
  };

  if (loading) {
    return (
      <DashboardLayout>
        <div className="cc-details-loading">
          <div className="cc-details-spinner" />
          <p>Loading product details...</p>
        </div>
      </DashboardLayout>
    );
  }

  if (error || !product) {
    return (
      <DashboardLayout>
        <div className="cc-details-error-card">
          <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <h2>Product Not Found</h2>
          <p>{error || 'This listing may have been removed or is outside your current college/marketplace reach.'}</p>
          <Link to="/browse" className="cc-details-btn-back">
            &larr; Back to Marketplace
          </Link>
        </div>
      </DashboardLayout>
    );
  }

  const isSold = product.status === 'SOLD';
  const isInactive = product.status === 'INACTIVE';
  const isSecondHand = product.productType === 'SECOND_HAND';

  return (
    <DashboardLayout>
      <div className="cc-details-page">
        {/* Breadcrumb Navigation */}
        <nav className="cc-details-breadcrumbs" aria-label="Breadcrumb">
          <Link to="/browse" className="cc-breadcrumb-link">Marketplace</Link>
          <span className="cc-breadcrumb-separator">/</span>
          {product.categoryName && (
            <>
              <Link to={`/browse?categoryId=${product.categoryId}`} className="cc-breadcrumb-link">
                {product.categoryName}
              </Link>
              <span className="cc-breadcrumb-separator">/</span>
            </>
          )}
          <span className="cc-breadcrumb-current">{product.title}</span>
        </nav>

        {/* Action Alerts */}
        {actionSuccess && (
          <div className="cc-details-alert cc-details-alert--success">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
            <span>{actionSuccess}</span>
          </div>
        )}

        {actionError && (
          <div className="cc-details-alert cc-details-alert--error">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <span>{actionError}</span>
          </div>
        )}

        {/* Main Grid: Gallery Left, Details Right */}
        <div className="cc-details-grid">
          {/* Left: Gallery */}
          <div className="cc-details-left">
            <ProductImageGallery images={product.images} title={product.title} />
          </div>

          {/* Right: Info and Actions */}
          <div className="cc-details-right">
            {/* Header Badges */}
            <div className="cc-details-badges">
              <span className="cc-details-category-badge">{product.categoryName || 'General'}</span>
              <span className={`cc-details-condition-badge ${isSecondHand ? 'cc-details-condition-badge--second-hand' : 'cc-details-condition-badge--new'}`}>
                {isSecondHand ? 'Second Hand' : 'Brand New'}
              </span>
              {isSold && <span className="cc-details-status-badge cc-details-status-badge--sold">SOLD</span>}
              {isInactive && <span className="cc-details-status-badge cc-details-status-badge--inactive">INACTIVE</span>}
              {product.sellingReach === 'CAMPUS_ONLY' && (
                <span className="cc-details-reach-badge">Campus Only</span>
              )}
            </div>

            {/* Title */}
            <h1 className="cc-details-title">{product.title}</h1>

            {/* Price Row */}
            <div className="cc-details-price-row">
              <span className="cc-details-price">
                ₹{Number(product.price).toLocaleString('en-IN')}
              </span>
              {product.quantity > 1 && (
                <span className="cc-details-stock-badge">
                  {product.quantity} in stock
                </span>
              )}
            </div>

            {/* Location & College metadata */}
            <div className="cc-details-meta-card">
              <div className="cc-details-meta-item">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
                  <path d="M6 12v5c0 2 2 3 6 3s6-1 6-3v-5" />
                </svg>
                <div className="cc-details-meta-text">
                  <span className="cc-details-meta-label">Campus / College</span>
                  <span className="cc-details-meta-val">{product.collegeName || 'Inter-College Community'}</span>
                </div>
              </div>

              {product.cityName && (
                <div className="cc-details-meta-item">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                    <circle cx="12" cy="10" r="3" />
                  </svg>
                  <div className="cc-details-meta-text">
                    <span className="cc-details-meta-label">City</span>
                    <span className="cc-details-meta-val">{product.cityName}</span>
                  </div>
                </div>
              )}
            </div>

            {/* Seller Information Card */}
            <div className="cc-details-seller-card">
              <div className="cc-details-seller-avatar">
                {product.sellerName ? product.sellerName.charAt(0).toUpperCase() : 'S'}
              </div>
              <div className="cc-details-seller-info">
                <span className="cc-details-seller-name">{product.sellerName || 'Verified Seller'}</span>
                <span className="cc-details-seller-sub">
                  {isOwner ? 'You are the seller of this listing' : 'Verified CampusCart Seller'}
                </span>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="cc-details-actions">
              {isOwner ? (
                /* Seller's Own Controls */
                <div className="cc-details-owner-actions">
                  <button
                    type="button"
                    className="cc-owner-btn cc-owner-btn--edit"
                    onClick={() => setEditModalOpen(true)}
                    disabled={actionLoading}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                    </svg>
                    Edit Listing
                  </button>

                  {!isSold && (
                    <button
                      type="button"
                      className="cc-owner-btn cc-owner-btn--sold"
                      onClick={handleMarkSold}
                      disabled={actionLoading}
                    >
                      Mark as Sold
                    </button>
                  )}

                  <button
                    type="button"
                    className="cc-owner-btn cc-owner-btn--toggle"
                    onClick={handleToggleActive}
                    disabled={actionLoading}
                  >
                    {isInactive ? 'Activate Listing' : 'Deactivate Listing'}
                  </button>

                  <button
                    type="button"
                    className="cc-owner-btn cc-owner-btn--delete"
                    onClick={() => setDeleteConfirmOpen(true)}
                    disabled={actionLoading}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="3 6 5 6 21 6" />
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                    </svg>
                    Delete
                  </button>
                </div>
              ) : (
                /* Buyer Controls */
                <div className="cc-details-buyer-actions">
                  <button
                    type="button"
                    className={`cc-details-btn-cart ${cartSuccess ? 'cc-details-btn-cart--success' : ''}`}
                    onClick={handleAddToCart}
                    disabled={cartAdding || isSold || isInactive}
                  >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <circle cx="9" cy="21" r="1" />
                      <circle cx="20" cy="21" r="1" />
                      <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
                    </svg>
                    <span>
                      {cartSuccess
                        ? 'Added to Cart ✓'
                        : cartAdding
                        ? 'Adding...'
                        : isSold
                        ? 'Item Sold'
                        : isInactive
                        ? 'Listing Inactive'
                        : 'Add to Cart'}
                    </span>
                  </button>

                  <button
                    type="button"
                    className="cc-details-btn-chat"
                    onClick={handleChatSeller}
                    disabled={chatStarting}
                  >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                    </svg>
                    <span>{chatStarting ? 'Connecting...' : 'Chat with Seller'}</span>
                  </button>

                  <button
                    type="button"
                    className={`cc-details-btn-wishlist ${isWishlisted ? 'cc-details-btn-wishlist--active' : ''}`}
                    onClick={handleWishlistToggle}
                    disabled={wishlistLoading}
                    aria-label={isWishlisted ? 'Remove from wishlist' : 'Save to wishlist'}
                  >
                    <svg width="20" height="20" viewBox="0 0 24 24" fill={isWishlisted ? '#e11d48' : 'none'} stroke={isWishlisted ? '#e11d48' : 'currentColor'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                    </svg>
                  </button>
                </div>
              )}
            </div>

            {/* Description Section */}
            <div className="cc-details-desc-section">
              <h2 className="cc-details-desc-title">Description</h2>
              <div className="cc-details-desc-body">
                {product.description || 'No additional description provided by the seller.'}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Edit Modal */}
      {editModalOpen && (
        <SellModal
          isOpen={editModalOpen}
          onClose={() => setEditModalOpen(false)}
          categories={categories}
          initialProduct={product}
          onProductUpdated={handleProductUpdated}
        />
      )}

      {/* Delete Confirmation Modal */}
      {deleteConfirmOpen && (
        <div className="cc-modal-overlay" onClick={() => setDeleteConfirmOpen(false)}>
          <div className="cc-confirm-modal" onClick={(e) => e.stopPropagation()}>
            <h3>Delete Listing?</h3>
            <p>Are you sure you want to delete &ldquo;{product.title}&rdquo;? This action cannot be undone.</p>
            <div className="cc-confirm-modal__actions">
              <button
                type="button"
                className="cc-confirm-modal__btn-cancel"
                onClick={() => setDeleteConfirmOpen(false)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="cc-confirm-modal__btn-delete"
                onClick={handleDelete}
                disabled={actionLoading}
              >
                {actionLoading ? 'Deleting...' : 'Yes, Delete Listing'}
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
