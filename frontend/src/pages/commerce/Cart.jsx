import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import api from '../../services/api';
import './Cart.css';

export default function Cart() {
  const navigate = useNavigate();

  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [updatingId, setUpdatingId] = useState(null);
  const [checkoutLoading, setCheckoutLoading] = useState(false);
  const [checkoutError, setCheckoutError] = useState('');

  // Modals
  const [confirmModalOpen, setConfirmModalOpen] = useState(false);
  const [placedOrder, setPlacedOrder] = useState(null);

  // Fetch cart
  const fetchCart = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await api.get('/api/v1/cart');
      if (res.success && res.data) {
        setCart(res.data);
      } else {
        setCart({ items: [], totalAmount: 0, checkoutReady: false });
      }
    } catch (err) {
      setError(err?.message || err?.error?.detail || 'Unable to load your cart.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCart();
  }, [fetchCart]);

  // Notify navbar & other components of cart update
  const notifyCartChange = () => {
    window.dispatchEvent(new CustomEvent('campuscart-cart-updated'));
  };

  // Update item quantity
  const handleQuantityChange = async (item, newQuantity) => {
    if (newQuantity < 1) {
      handleRemoveItem(item.productId);
      return;
    }
    if (newQuantity > item.availableQuantity) return;

    setUpdatingId(item.productId);
    setCheckoutError('');
    try {
      const res = await api.patch(`/api/v1/cart/items/${item.productId}`, {
        quantity: newQuantity,
      });
      if (res.success) {
        await fetchCart();
        notifyCartChange();
      }
    } catch (err) {
      setCheckoutError(err?.message || 'Failed to update quantity.');
    } finally {
      setUpdatingId(null);
    }
  };

  // Remove item
  const handleRemoveItem = async (productId) => {
    setUpdatingId(productId);
    setCheckoutError('');
    try {
      const res = await api.delete(`/api/v1/cart/items/${productId}`);
      if (res.success) {
        await fetchCart();
        notifyCartChange();
      }
    } catch (err) {
      setCheckoutError(err?.message || 'Failed to remove item.');
    } finally {
      setUpdatingId(null);
    }
  };

  // Execute checkout
  const handleCheckout = async () => {
    setCheckoutLoading(true);
    setCheckoutError('');
    try {
      const res = await api.post('/api/v1/orders');
      if (res.success && res.data) {
        setPlacedOrder(res.data);
        setConfirmModalOpen(false);
        notifyCartChange();
        fetchCart();
      }
    } catch (err) {
      setCheckoutError(err?.message || err?.error?.detail || 'Checkout failed. Please check product availability.');
    } finally {
      setCheckoutLoading(false);
    }
  };

  const formatCurrency = (val) => {
    if (val === null || val === undefined) return '₹0';
    return `₹${Number(val).toLocaleString('en-IN')}`;
  };

  const hasItems = cart && cart.items && cart.items.length > 0;
  const hasUnavailableItems = hasItems && cart.items.some((it) => !it.available);

  return (
    <DashboardLayout>
      <div className="cc-cart-page">
        {/* Header */}
        <div className="cc-cart-header">
          <div>
            <h1 className="cc-cart-title">Shopping Cart</h1>
            <p className="cc-cart-subtitle">
              Review your items, adjust quantities, and proceed to checkout.
            </p>
          </div>
          {hasItems && (
            <Link to="/browse" className="cc-cart-continue-link">
              &larr; Continue Shopping
            </Link>
          )}
        </div>

        {/* Global Checkout Error */}
        {checkoutError && (
          <div className="cc-cart-alert cc-cart-alert--error">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <span>{checkoutError}</span>
          </div>
        )}

        {/* Loading State */}
        {loading ? (
          <div className="cc-cart-loading">
            <div className="cc-cart-spinner" />
            <p>Loading your shopping cart...</p>
          </div>
        ) : error ? (
          /* Error State */
          <div className="cc-cart-error-card">
            <p>{error}</p>
            <button type="button" className="cc-cart-btn-retry" onClick={fetchCart}>
              Retry
            </button>
          </div>
        ) : !hasItems ? (
          /* Empty Cart State */
          <div className="cc-cart-empty">
            <div className="cc-cart-empty__icon">
              <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="9" cy="21" r="1" />
                <circle cx="20" cy="21" r="1" />
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
              </svg>
            </div>
            <h2>Your cart is empty</h2>
            <p>Looks like you haven&apos;t added any textbooks, gadgets, or campus gear to your cart yet.</p>
            <Link to="/browse" className="cc-cart-btn-primary">
              Explore Marketplace
            </Link>
          </div>
        ) : (
          /* Cart Main Layout */
          <div className="cc-cart-layout">
            {/* Items Column */}
            <div className="cc-cart-items-column">
              {hasUnavailableItems && (
                <div className="cc-cart-alert cc-cart-alert--warning">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                    <line x1="12" y1="9" x2="12" y2="13" />
                    <line x1="12" y1="17" x2="12.01" y2="17" />
                  </svg>
                  <span>Some items in your cart are currently unavailable or out of stock. Please remove them to proceed.</span>
                </div>
              )}

              <div className="cc-cart-items-list">
                {cart.items.map((item) => {
                  const isUpdating = updatingId === item.productId;
                  return (
                    <div
                      key={item.productId}
                      className={`cc-cart-item ${!item.available ? 'cc-cart-item--unavailable' : ''}`}
                    >
                      {/* Product Thumbnail */}
                      <Link to={`/products/${item.productId}`} className="cc-cart-item__img-link">
                        {item.imageUrl ? (
                          <img src={item.imageUrl} alt={item.title} className="cc-cart-item__img" />
                        ) : (
                          <div className="cc-cart-item__img-fallback">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                              <circle cx="8.5" cy="8.5" r="1.5" />
                              <polyline points="21 15 16 10 5 21" />
                            </svg>
                          </div>
                        )}
                      </Link>

                      {/* Product Details */}
                      <div className="cc-cart-item__info">
                        <Link to={`/products/${item.productId}`} className="cc-cart-item__title">
                          {item.title}
                        </Link>
                        <span className="cc-cart-item__seller">
                          Sold by: <strong>{item.sellerName || 'Campus Seller'}</strong>
                        </span>

                        {!item.available ? (
                          <span className="cc-cart-item__badge-unavailable">
                            {item.status !== 'ACTIVE' ? 'Item Unavailable' : 'Out of Stock'}
                          </span>
                        ) : (
                          <span className="cc-cart-item__unit-price">
                            {formatCurrency(item.unitPrice)} each
                          </span>
                        )}
                      </div>

                      {/* Quantity Selector */}
                      <div className="cc-cart-item__qty-box">
                        <button
                          type="button"
                          className="cc-cart-qty-btn"
                          onClick={() => handleQuantityChange(item, item.quantity - 1)}
                          disabled={isUpdating || checkoutLoading}
                          aria-label="Decrease quantity"
                        >
                          -
                        </button>
                        <span className="cc-cart-qty-value">{item.quantity}</span>
                        <button
                          type="button"
                          className="cc-cart-qty-btn"
                          onClick={() => handleQuantityChange(item, item.quantity + 1)}
                          disabled={isUpdating || checkoutLoading || item.quantity >= item.availableQuantity}
                          aria-label="Increase quantity"
                        >
                          +
                        </button>
                      </div>

                      {/* Total Price */}
                      <div className="cc-cart-item__price-box">
                        <span className="cc-cart-item__line-total">
                          {formatCurrency(item.lineTotal)}
                        </span>
                      </div>

                      {/* Remove Button */}
                      <button
                        type="button"
                        className="cc-cart-item__remove-btn"
                        onClick={() => handleRemoveItem(item.productId)}
                        disabled={isUpdating || checkoutLoading}
                        aria-label="Remove item"
                        title="Remove item"
                      >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <polyline points="3 6 5 6 21 6" />
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                        </svg>
                      </button>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Order Summary Column */}
            <div className="cc-cart-summary-column">
              <div className="cc-cart-summary-card">
                <h2 className="cc-cart-summary-title">Order Summary</h2>

                <div className="cc-cart-summary-row">
                  <span>Subtotal ({cart.items.length} item{cart.items.length === 1 ? '' : 's'})</span>
                  <span>{formatCurrency(cart.totalAmount)}</span>
                </div>

                <div className="cc-cart-summary-row">
                  <span>Campus Pickup / Handover</span>
                  <span className="cc-cart-free-badge">FREE</span>
                </div>

                <div className="cc-cart-summary-divider" />

                <div className="cc-cart-summary-row cc-cart-summary-row--total">
                  <span>Total Amount</span>
                  <span className="cc-cart-total-price">{formatCurrency(cart.totalAmount)}</span>
                </div>

                <button
                  type="button"
                  className="cc-cart-btn-checkout"
                  onClick={() => setConfirmModalOpen(true)}
                  disabled={!cart.checkoutReady || checkoutLoading}
                >
                  {checkoutLoading ? 'Processing Checkout...' : 'Proceed to Checkout'}
                </button>

                <p className="cc-cart-summary-hint">
                  Secure checkout on CampusCart. You can inspect and verify the item upon campus handover before finalizing payment.
                </p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Checkout Confirmation Modal */}
      {confirmModalOpen && (
        <div className="cc-modal-overlay" onClick={() => !checkoutLoading && setConfirmModalOpen(false)}>
          <div className="cc-checkout-modal" onClick={(e) => e.stopPropagation()}>
            <div className="cc-checkout-modal__header">
              <h2>Confirm Your Order</h2>
              <button
                type="button"
                className="cc-checkout-modal__close-btn"
                onClick={() => setConfirmModalOpen(false)}
                disabled={checkoutLoading}
              >
                &times;
              </button>
            </div>

            <div className="cc-checkout-modal__body">
              <p className="cc-checkout-modal__desc">
                You are placing an order for <strong>{cart.items.length} item{cart.items.length === 1 ? '' : 's'}</strong> with a total of <strong>{formatCurrency(cart.totalAmount)}</strong>.
              </p>

              <div className="cc-checkout-modal__items">
                {cart.items.map((it) => (
                  <div key={it.productId} className="cc-checkout-mini-row">
                    <span className="cc-checkout-mini-title">{it.quantity}x {it.title}</span>
                    <span className="cc-checkout-mini-price">{formatCurrency(it.lineTotal)}</span>
                  </div>
                ))}
              </div>

              <div className="cc-checkout-modal__notice">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="16" x2="12" y2="12" />
                  <line x1="12" y1="8" x2="12.01" y2="8" />
                </svg>
                <span>The seller will be notified immediately to arrange on-campus pickup and handover.</span>
              </div>
            </div>

            <div className="cc-checkout-modal__footer">
              <button
                type="button"
                className="cc-checkout-btn-cancel"
                onClick={() => setConfirmModalOpen(false)}
                disabled={checkoutLoading}
              >
                Cancel
              </button>
              <button
                type="button"
                className="cc-checkout-btn-confirm"
                onClick={handleCheckout}
                disabled={checkoutLoading}
              >
                {checkoutLoading ? 'Placing Order...' : 'Confirm & Place Order'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Order Success Modal */}
      {placedOrder && (
        <div className="cc-modal-overlay">
          <div className="cc-success-modal">
            <div className="cc-success-modal__icon">
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#059669" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
            </div>
            <h2>Order Placed Successfully!</h2>
            <p>
              Your order has been recorded. The seller has been notified and will coordinate with you.
            </p>
            <div className="cc-success-modal__details">
              <span>Order ID:</span>
              <code>{placedOrder.id}</code>
            </div>
            <div className="cc-success-modal__actions">
              <button
                type="button"
                className="cc-success-btn-orders"
                onClick={() => navigate('/orders')}
              >
                View My Orders
              </button>
              <button
                type="button"
                className="cc-success-btn-browse"
                onClick={() => navigate('/browse')}
              >
                Continue Shopping
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
