import { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import api from '../../services/api';
import './OrderDetails.css';

export default function OrderDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [toastMessage, setToastMessage] = useState('');

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(''), 3500);
  };

  const fetchOrder = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await api.get(`/api/v1/orders/${id}`);
      if (res.success && res.data) {
        setOrder(res.data);
      } else {
        setError('Order not found.');
      }
    } catch (err) {
      setError(err?.message || err?.error?.detail || 'Unable to load order details.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchOrder();
  }, [fetchOrder]);

  const handleTransition = async (targetStatus) => {
    setActionLoading(true);
    try {
      let res;
      if (targetStatus === 'CANCELLED') {
        res = await api.post(`/api/v1/orders/${id}/cancel`);
      } else if (targetStatus === 'COMPLETED') {
        res = await api.post(`/api/v1/orders/${id}/complete`);
      } else {
        res = await api.patch(`/api/v1/orders/${id}/status`, { status: targetStatus });
      }

      if (res.success) {
        showToast(`Order updated to ${targetStatus}.`);
        fetchOrder();
      }
    } catch (err) {
      showToast(err?.message || 'Status transition failed.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleContactCounterparty = async (productId) => {
    if (!productId) return;
    try {
      const res = await api.post(`/api/v1/conversations?productId=${productId}`);
      if (res.success && res.data) {
        navigate(`/chat?conversationId=${res.data.id}`);
      }
    } catch (err) {
      showToast(err?.message || 'Unable to open chat.');
    }
  };

  const formatCurrency = (val) => {
    if (val === null || val === undefined) return '₹0';
    return `₹${Number(val).toLocaleString('en-IN')}`;
  };

  const formatDate = (iso) => {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Stepper logic
  const steps = [
    { key: 'PLACED', label: 'Order Placed' },
    { key: 'ACCEPTED', label: 'Accepted by Seller' },
    { key: 'SHIPPED', label: 'Ready / In Transit' },
    { key: 'DELIVERED', label: 'Handed Over' },
    { key: 'COMPLETED', label: 'Completed' },
  ];

  const getStepIndex = (status) => {
    switch (status) {
      case 'PLACED': return 0;
      case 'ACCEPTED': return 1;
      case 'SHIPPED': return 2;
      case 'DELIVERED': return 3;
      case 'COMPLETED': return 4;
      default: return -1;
    }
  };

  const currentStep = order ? getStepIndex(order.status) : -1;
  const isTerminalNegative = order && (order.status === 'CANCELLED' || order.status === 'REJECTED');

  return (
    <DashboardLayout>
      <div className="cc-order-details-page">
        {/* Breadcrumb & Navigation */}
        <div className="cc-order-details-breadcrumb">
          <Link to="/orders">&larr; Back to Orders</Link>
        </div>

        {/* Toast Alert */}
        {toastMessage && (
          <div className="cc-order-details-toast">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
            <span>{toastMessage}</span>
          </div>
        )}

        {/* Loading State */}
        {loading ? (
          <div className="cc-order-details-loading">
            <div className="cc-order-details-spinner" />
            <p>Loading order details...</p>
          </div>
        ) : error ? (
          <div className="cc-order-details-error">
            <p>{error}</p>
            <Link to="/orders" className="cc-order-btn-back">
              Back to Orders
            </Link>
          </div>
        ) : !order ? null : (
          <div className="cc-order-details-content">
            {/* Header Card */}
            <div className="cc-order-details-card">
              <div className="cc-order-details-card__top">
                <div>
                  <h1 className="cc-order-details-title">Order #{order.id.substring(0, 8)}</h1>
                  <span className="cc-order-details-placed">Placed on {formatDate(order.createdAt)}</span>
                </div>
                <div className="cc-order-details-badge-wrap">
                  <span className={`cc-order-status-pill cc-order-status-pill--${order.status.toLowerCase()}`}>
                    {order.status}
                  </span>
                </div>
              </div>

              {/* Progress Stepper */}
              {!isTerminalNegative ? (
                <div className="cc-order-stepper">
                  {steps.map((s, idx) => {
                    const isDone = idx <= currentStep;
                    const isCurrent = idx === currentStep;
                    return (
                      <div
                        key={s.key}
                        className={`cc-order-stepper__step ${isDone ? 'cc-order-stepper__step--done' : ''} ${isCurrent ? 'cc-order-stepper__step--current' : ''}`}
                      >
                        <div className="cc-order-stepper__circle">
                          {isDone ? (
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                              <polyline points="20 6 9 17 4 12" />
                            </svg>
                          ) : (
                            idx + 1
                          )}
                        </div>
                        <span className="cc-order-stepper__label">{s.label}</span>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="cc-order-terminal-notice">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="15" y1="9" x2="9" y2="15" />
                    <line x1="9" y1="9" x2="15" y2="15" />
                  </svg>
                  <span>This order has been {order.status === 'CANCELLED' ? 'cancelled' : 'declined'}. Any reserved inventory was restored.</span>
                </div>
              )}
            </div>

            {/* Layout Grid: Items vs Summary */}
            <div className="cc-order-details-grid">
              {/* Items Column */}
              <div className="cc-order-items-card">
                <h2>Purchased Items ({order.items.length})</h2>

                <div className="cc-order-items-list">
                  {order.items.map((item) => (
                    <div key={item.id} className="cc-order-detail-item">
                      <div className="cc-order-detail-item__thumb">
                        {item.imageUrl ? (
                          <img src={item.imageUrl} alt={item.productTitle} />
                        ) : (
                          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                            <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                            <circle cx="8.5" cy="8.5" r="1.5" />
                            <polyline points="21 15 16 10 5 21" />
                          </svg>
                        )}
                      </div>

                      <div className="cc-order-detail-item__info">
                        <Link to={`/products/${item.productId}`} className="cc-order-detail-item__title">
                          {item.productTitle}
                        </Link>
                        <span className="cc-order-detail-item__seller">
                          Seller: <strong>{item.sellerName || 'Campus Seller'}</strong>
                        </span>
                        <div className="cc-order-detail-item__price-breakdown">
                          <span>{item.quantity} &times; {formatCurrency(item.unitPrice)}</span>
                          <span className="cc-order-detail-item__line-total">{formatCurrency(item.lineTotal)}</span>
                        </div>
                      </div>

                      <button
                        type="button"
                        className="cc-order-detail-btn-chat"
                        onClick={() => handleContactCounterparty(item.productId)}
                        title="Chat about this item"
                      >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                        </svg>
                        <span>Chat</span>
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* Summary Column */}
              <div className="cc-order-summary-col">
                <div className="cc-order-details-card">
                  <h2>Payment & Fulfillment</h2>

                  <div className="cc-order-info-row">
                    <span>Payment Status</span>
                    <span className="cc-order-payment-status">{order.paymentStatus || 'PENDING'}</span>
                  </div>

                  <div className="cc-order-info-row">
                    <span>Campus Handover</span>
                    <span className="cc-order-free-pickup">Free on Campus</span>
                  </div>

                  <div className="cc-order-info-divider" />

                  <div className="cc-order-info-row cc-order-info-row--total">
                    <span>Total Amount</span>
                    <span className="cc-order-info-total">{formatCurrency(order.totalAmount)}</span>
                  </div>

                  {/* Actions */}
                  <div className="cc-order-detail-actions">
                    {order.status === 'PLACED' && (
                      <button
                        type="button"
                        className="cc-order-btn-action cc-order-btn-action--danger"
                        onClick={() => handleTransition('CANCELLED')}
                        disabled={actionLoading}
                      >
                        Cancel Order
                      </button>
                    )}

                    {order.status === 'DELIVERED' && (
                      <button
                        type="button"
                        className="cc-order-btn-action cc-order-btn-action--primary"
                        onClick={() => handleTransition('COMPLETED')}
                        disabled={actionLoading}
                      >
                        Confirm Received / Completed
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
