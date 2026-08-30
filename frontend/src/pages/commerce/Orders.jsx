import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import api from '../../services/api';
import './Orders.css';

export default function Orders() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // Active view: 'buyer' ("My Purchases") vs 'seller' ("Orders Received")
  const initialTab = searchParams.get('tab') === 'seller' ? 'seller' : 'buyer';
  const [activeTab, setActiveTab] = useState(initialTab);

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Status Action state
  const [actionLoading, setActionLoading] = useState(false);
  const [confirmTarget, setConfirmTarget] = useState(null); // { order, action: 'cancel' | 'reject' }
  const [toastMessage, setToastMessage] = useState('');

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(''), 3500);
  };

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setPage(0);
    setSearchParams(tab === 'seller' ? { tab: 'seller' } : {});
  };

  // Fetch orders based on active tab
  const fetchOrders = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const endpoint = activeTab === 'seller' ? '/api/v1/orders/seller' : '/api/v1/orders';
      const res = await api.get(`${endpoint}?page=${page}&size=15`);
      if (res.success && res.data) {
        setOrders(res.data.content || []);
        setTotalPages(res.data.totalPages || 0);
      } else {
        setOrders([]);
        setTotalPages(0);
      }
    } catch (err) {
      setError(err?.message || err?.error?.detail || 'Unable to load orders.');
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }, [activeTab, page]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  // Execute order status transition
  const handleTransition = async (orderId, targetStatus) => {
    setActionLoading(true);
    try {
      let res;
      if (targetStatus === 'CANCELLED') {
        res = await api.post(`/api/v1/orders/${orderId}/cancel`);
      } else if (targetStatus === 'COMPLETED') {
        res = await api.post(`/api/v1/orders/${orderId}/complete`);
      } else {
        res = await api.patch(`/api/v1/orders/${orderId}/status`, { status: targetStatus });
      }

      if (res.success) {
        showToast(`Order status updated to ${targetStatus}.`);
        setConfirmTarget(null);
        fetchOrders();
      }
    } catch (err) {
      showToast(err?.message || 'Failed to update order status.');
    } finally {
      setActionLoading(false);
    }
  };

  // Contact Seller / Buyer
  const handleContactUser = async (productId) => {
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

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PLACED':
        return <span className="cc-order-badge cc-order-badge--placed">Order Placed</span>;
      case 'ACCEPTED':
        return <span className="cc-order-badge cc-order-badge--accepted">Accepted</span>;
      case 'SHIPPED':
        return <span className="cc-order-badge cc-order-badge--shipped">Shipped / In Transit</span>;
      case 'DELIVERED':
        return <span className="cc-order-badge cc-order-badge--delivered">Delivered / Handed Over</span>;
      case 'COMPLETED':
        return <span className="cc-order-badge cc-order-badge--completed">Completed</span>;
      case 'CANCELLED':
        return <span className="cc-order-badge cc-order-badge--cancelled">Cancelled</span>;
      case 'REJECTED':
        return <span className="cc-order-badge cc-order-badge--rejected">Declined</span>;
      default:
        return <span className="cc-order-badge">{status}</span>;
    }
  };

  return (
    <DashboardLayout>
      <div className="cc-orders-page">
        {/* Header */}
        <div className="cc-orders-header">
          <div>
            <h1 className="cc-orders-title">Orders Management</h1>
            <p className="cc-orders-subtitle">
              Track your campus marketplace purchases and manage orders received for your listings.
            </p>
          </div>

          {/* View Tab Switcher */}
          <div className="cc-orders-tabs" role="tablist">
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === 'buyer'}
              className={`cc-orders-tab ${activeTab === 'buyer' ? 'cc-orders-tab--active' : ''}`}
              onClick={() => handleTabChange('buyer')}
            >
              My Purchases
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === 'seller'}
              className={`cc-orders-tab ${activeTab === 'seller' ? 'cc-orders-tab--active' : ''}`}
              onClick={() => handleTabChange('seller')}
            >
              Orders Received
            </button>
          </div>
        </div>

        {/* Toast Alert */}
        {toastMessage && (
          <div className="cc-orders-toast">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
            <span>{toastMessage}</span>
          </div>
        )}

        {/* Loading State */}
        {loading ? (
          <div className="cc-orders-loading">
            <div className="cc-orders-spinner" />
            <p>Loading {activeTab === 'seller' ? 'received orders' : 'your purchases'}...</p>
          </div>
        ) : error ? (
          /* Error State */
          <div className="cc-orders-error-card">
            <p>{error}</p>
            <button type="button" className="cc-orders-btn-retry" onClick={fetchOrders}>
              Retry
            </button>
          </div>
        ) : orders.length === 0 ? (
          /* Empty State */
          <div className="cc-orders-empty">
            <div className="cc-orders-empty__icon">
              <svg width="50" height="50" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round">
                <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
                <line x1="3" y1="6" x2="21" y2="6" />
                <path d="M16 10a4 4 0 0 1-8 0" />
              </svg>
            </div>
            <h2>{activeTab === 'seller' ? 'No orders received yet' : 'No purchase history yet'}</h2>
            <p>
              {activeTab === 'seller'
                ? 'When a student places an order for one of your listings, it will appear here for you to accept and fulfill.'
                : 'You have not placed any orders yet. Browse campus listings to find great student deals.'}
            </p>
            <Link to={activeTab === 'seller' ? '/my-listings' : '/browse'} className="cc-orders-empty-btn">
              {activeTab === 'seller' ? 'Manage My Listings' : 'Explore Marketplace'}
            </Link>
          </div>
        ) : (
          /* Orders List */
          <div className="cc-orders-list">
            {orders.map((order) => {
              const isBuyer = activeTab === 'buyer';
              const canCancel = isBuyer && (order.status === 'PLACED' || order.status === 'ACCEPTED');
              const canComplete = isBuyer && order.status === 'DELIVERED';
              const canAccept = !isBuyer && order.status === 'PLACED';
              const canShip = !isBuyer && order.status === 'ACCEPTED';
              const canDeliver = !isBuyer && order.status === 'SHIPPED';

              return (
                <div key={order.id} className="cc-order-card">
                  {/* Card Top */}
                  <div className="cc-order-card__header">
                    <div className="cc-order-card__meta">
                      <span className="cc-order-card__id">
                        Order <code>#{order.id.substring(0, 8)}</code>
                      </span>
                      <span className="cc-order-card__date">{formatDate(order.createdAt)}</span>
                    </div>

                    <div className="cc-order-card__badge-wrap">
                      {getStatusBadge(order.status)}
                    </div>
                  </div>

                  {/* Items List */}
                  <div className="cc-order-card__items">
                    {order.items.map((item) => (
                      <div key={item.id} className="cc-order-item-row">
                        {/* Thumbnail */}
                        <div className="cc-order-item-thumb">
                          {item.imageUrl ? (
                            <img src={item.imageUrl} alt={item.productTitle} />
                          ) : (
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                              <circle cx="8.5" cy="8.5" r="1.5" />
                              <polyline points="21 15 16 10 5 21" />
                            </svg>
                          )}
                        </div>

                        {/* Title & Seller/Buyer info */}
                        <div className="cc-order-item-info">
                          <Link to={`/products/${item.productId}`} className="cc-order-item-title">
                            {item.productTitle}
                          </Link>
                          <span className="cc-order-item-seller">
                            {isBuyer ? (
                              <>Seller: <strong>{item.sellerName || 'Campus Seller'}</strong></>
                            ) : (
                              <>Buyer: <strong>{order.buyerName || 'Campus Student'}</strong></>
                            )}
                          </span>
                        </div>

                        {/* Quantity & Unit Price */}
                        <div className="cc-order-item-qty">
                          <span>{item.quantity} &times; {formatCurrency(item.unitPrice)}</span>
                        </div>

                        {/* Line Total */}
                        <div className="cc-order-item-total">
                          <span>{formatCurrency(item.lineTotal)}</span>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Card Bottom / Actions */}
                  <div className="cc-order-card__footer">
                    <div className="cc-order-card__total-box">
                      <span className="cc-order-card__total-label">Total Amount:</span>
                      <span className="cc-order-card__total-value">{formatCurrency(order.totalAmount)}</span>
                    </div>

                    <div className="cc-order-card__actions">
                      {/* Contact Counterparty */}
                      {order.items[0] && (
                        <button
                          type="button"
                          className="cc-order-btn-chat"
                          onClick={() => handleContactUser(order.items[0].productId)}
                        >
                          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                          </svg>
                          <span>{isBuyer ? 'Chat with Seller' : 'Chat with Buyer'}</span>
                        </button>
                      )}

                      {/* Buyer Actions */}
                      {canCancel && (
                        <button
                          type="button"
                          className="cc-order-btn-cancel"
                          onClick={() => setConfirmTarget({ order, action: 'cancel' })}
                          disabled={actionLoading}
                        >
                          Cancel Order
                        </button>
                      )}

                      {canComplete && (
                        <button
                          type="button"
                          className="cc-order-btn-primary"
                          onClick={() => handleTransition(order.id, 'COMPLETED')}
                          disabled={actionLoading}
                        >
                          Confirm Received
                        </button>
                      )}

                      {/* Seller Actions */}
                      {canAccept && (
                        <>
                          <button
                            type="button"
                            className="cc-order-btn-reject"
                            onClick={() => setConfirmTarget({ order, action: 'reject' })}
                            disabled={actionLoading}
                          >
                            Decline
                          </button>
                          <button
                            type="button"
                            className="cc-order-btn-primary"
                            onClick={() => handleTransition(order.id, 'ACCEPTED')}
                            disabled={actionLoading}
                          >
                            Accept Order
                          </button>
                        </>
                      )}

                      {canShip && (
                        <button
                          type="button"
                          className="cc-order-btn-primary"
                          onClick={() => handleTransition(order.id, 'SHIPPED')}
                          disabled={actionLoading}
                        >
                          Mark as Shipped / Ready
                        </button>
                      )}

                      {canDeliver && (
                        <button
                          type="button"
                          className="cc-order-btn-primary"
                          onClick={() => handleTransition(order.id, 'DELIVERED')}
                          disabled={actionLoading}
                        >
                          Mark Handed Over / Delivered
                        </button>
                      )}

                      {/* View Order Details */}
                      <Link to={`/orders/${order.id}`} className="cc-order-btn-details">
                        View Details &rarr;
                      </Link>
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

      {/* Cancel / Reject Confirmation Modal */}
      {confirmTarget && (
        <div className="cc-modal-overlay" onClick={() => setConfirmTarget(null)}>
          <div className="cc-confirm-modal" onClick={(e) => e.stopPropagation()}>
            <h3>{confirmTarget.action === 'cancel' ? 'Cancel this order?' : 'Decline this order?'}</h3>
            <p>
              {confirmTarget.action === 'cancel'
                ? 'Are you sure you want to cancel your order? Any reserved inventory will be released.'
                : 'Are you sure you want to decline this order? The buyer will be notified.'}
            </p>
            <div className="cc-confirm-modal__actions">
              <button
                type="button"
                className="cc-confirm-modal__btn-cancel"
                onClick={() => setConfirmTarget(null)}
              >
                Go Back
              </button>
              <button
                type="button"
                className="cc-confirm-modal__btn-delete"
                onClick={() =>
                  handleTransition(
                    confirmTarget.order.id,
                    confirmTarget.action === 'cancel' ? 'CANCELLED' : 'REJECTED'
                  )
                }
                disabled={actionLoading}
              >
                {actionLoading
                  ? 'Processing...'
                  : confirmTarget.action === 'cancel'
                  ? 'Yes, Cancel Order'
                  : 'Yes, Decline Order'}
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
