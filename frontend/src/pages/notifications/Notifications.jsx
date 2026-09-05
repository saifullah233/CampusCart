import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import api from '../../services/api';
import './Notifications.css';

/** Map a notification type to an SVG icon element and CSS modifier. */
function NotificationIcon({ type }) {
  if (type === 'NEW_MESSAGE') {
    return (
      <div className="cc-notif-icon cc-notif-icon--message">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
        </svg>
      </div>
    );
  }
  if (type === 'ORDER_RECEIVED' || type === 'ORDER_UPDATE') {
    return (
      <div className="cc-notif-icon cc-notif-icon--order">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
          <line x1="3" y1="6" x2="21" y2="6" />
          <path d="M16 10a4 4 0 0 1-8 0" />
        </svg>
      </div>
    );
  }
  if (type === 'NEW_PRODUCT' || type === 'PRODUCT_LIKED' || type === 'WISHLIST_ADDED') {
    return (
      <div className="cc-notif-icon cc-notif-icon--product">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <polyline points="21 15 16 10 5 21" />
        </svg>
      </div>
    );
  }
  return (
    <div className="cc-notif-icon cc-notif-icon--default">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="10" />
        <line x1="12" y1="8" x2="12" y2="12" />
        <line x1="12" y1="16" x2="12.01" y2="16" />
      </svg>
    </div>
  );
}

/** Parse dataJson safely. Always returns an object. */
function parseData(dataJson) {
  if (!dataJson) return {};
  try {
    return typeof dataJson === 'string' ? JSON.parse(dataJson) : dataJson;
  } catch {
    return {};
  }
}

/** Derive the navigation target from a notification. Returns a path string or null. */
function resolveTarget(notification) {
  const data = parseData(notification.dataJson);
  switch (notification.type) {
    case 'NEW_MESSAGE':
      if (data.conversationId) return `/chat?conversationId=${data.conversationId}`;
      return null;
    case 'ORDER_RECEIVED':
    case 'ORDER_UPDATE':
      if (data.orderId) return `/orders/${data.orderId}`;
      return null;
    case 'NEW_PRODUCT':
    case 'PRODUCT_LIKED':
    case 'WISHLIST_ADDED':
      if (data.productId) return `/products/${data.productId}`;
      return null;
    default:
      return null;
  }
}

/** Relative-time label, e.g. "2m ago". */
function relativeTime(isoString) {
  if (!isoString) return '';
  const delta = Math.floor((Date.now() - new Date(isoString).getTime()) / 1000);
  if (delta < 60) return 'Just now';
  if (delta < 3600) return `${Math.floor(delta / 60)}m ago`;
  if (delta < 86400) return `${Math.floor(delta / 3600)}h ago`;
  if (delta < 604800) return `${Math.floor(delta / 86400)}d ago`;
  return new Date(isoString).toLocaleDateString([], { month: 'short', day: 'numeric' });
}

const PAGE_SIZE = 20;

export default function Notifications() {
  const navigate = useNavigate();

  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [markingAll, setMarkingAll] = useState(false);
  const [toastMessage, setToastMessage] = useState(null);

  // Cached enrichment for older notifications lacking senderName/productTitle in dataJson
  const [enrichedData, setEnrichedData] = useState({});
  const fetchedConvsRef = useRef(new Set());

  // Current user id for counterparty determination
  const getCurrentUserId = () => {
    try {
      const userStr = localStorage.getItem('cc_user');
      if (userStr) {
        const u = JSON.parse(userStr);
        return u.id || u.userId;
      }
    } catch {
      // Fallback
    }
    return null;
  };
  const currentUserId = getCurrentUserId();

  const unreadCount = notifications.filter((n) => !n.read).length;

  /** Enrich older NEW_MESSAGE notifications that only contain conversationId */
  const enrichOldNotifications = useCallback(async (list) => {
    const missingConvs = list
      .filter((n) => n.type === 'NEW_MESSAGE')
      .map((n) => {
        const data = parseData(n.dataJson);
        return {
          notifId: n.id,
          convId: data.conversationId,
          hasSender: !!data.senderName,
          hasProduct: !!data.productTitle,
        };
      })
      .filter((item) => item.convId && (!item.hasSender || !item.hasProduct));

    for (const item of missingConvs) {
      if (fetchedConvsRef.current.has(item.convId)) continue;
      fetchedConvsRef.current.add(item.convId);

      try {
        const res = await api.get(`/api/v1/conversations/${item.convId}`);
        if (res.success && res.data) {
          const conv = res.data;
          const otherPartyName = currentUserId && conv.buyerId === currentUserId
            ? conv.sellerName
            : conv.buyerName;

          let price = null;
          if (conv.productId) {
            try {
              const pRes = await api.get(`/api/v1/products/${conv.productId}`);
              if (pRes.success && pRes.data) {
                price = pRes.data.price;
              }
            } catch {
              // Ignore product fetch failure
            }
          }

          setEnrichedData((prev) => ({
            ...prev,
            [item.convId]: {
              senderName: otherPartyName || 'Campus Member',
              productTitle: conv.productTitle,
              price: price,
            },
          }));
        }
      } catch {
        // Ignore conversation fetch failure
      }
    }
  }, [currentUserId]);

  /** Fetch a page of notifications and merge into state. */
  const fetchPage = useCallback(async (pageIndex, replace = false) => {
    try {
      const res = await api.get(`/api/v1/notifications?page=${pageIndex}&size=${PAGE_SIZE}`);
      if (res.success && res.data) {
        const content = res.data.content || [];
        setNotifications((prev) => (replace ? content : [...prev, ...content]));
        setHasMore(!res.data.last);
        setPage(pageIndex);
        enrichOldNotifications(content);
      }
    } catch {
      if (replace) setError('Failed to load notifications. Please try again.');
    }
  }, [enrichOldNotifications]);

  useEffect(() => {
    setLoading(true);
    fetchPage(0, true).finally(() => setLoading(false));
  }, [fetchPage]);

  /** Mark a single notification as read, then navigate to its target. */
  const handleNotificationClick = async (notification) => {
    // Optimistically mark read in local state
    if (!notification.read) {
      setNotifications((prev) =>
        prev.map((n) => (n.id === notification.id ? { ...n, read: true } : n))
      );
      try {
        await api.patch(`/api/v1/notifications/${notification.id}/read`);
        window.dispatchEvent(new CustomEvent('campuscart-unread-updated'));
      } catch {
        // Revert optimistic update on failure
        setNotifications((prev) =>
          prev.map((n) => (n.id === notification.id ? { ...n, read: false } : n))
        );
      }
    }

    const target = resolveTarget(notification);
    if (target) {
      navigate(target);
    } else {
      setToastMessage('This notification target is no longer available.');
      setTimeout(() => setToastMessage(null), 3500);
    }
  };

  /** Mark all as read. */
  const handleMarkAllRead = async () => {
    if (markingAll || unreadCount === 0) return;
    setMarkingAll(true);
    try {
      await api.post('/api/v1/notifications/read-all');
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      window.dispatchEvent(new CustomEvent('campuscart-unread-updated'));
    } catch {
      // Silent — user can retry
    } finally {
      setMarkingAll(false);
    }
  };

  /** Load the next page. */
  const handleLoadMore = async () => {
    if (loadingMore || !hasMore) return;
    setLoadingMore(true);
    await fetchPage(page + 1, false);
    setLoadingMore(false);
  };

  /** Helper to compute title, message content, and product line for a notification */
  const formatNotification = (notification) => {
    const data = parseData(notification.dataJson);
    const enriched = data.conversationId ? (enrichedData[data.conversationId] || {}) : {};

    if (notification.type === 'NEW_MESSAGE') {
      const senderName = data.senderName || enriched.senderName;
      const title = senderName
        ? `New message from ${senderName}`
        : (notification.title && notification.title !== 'New message' ? notification.title : 'New message');

      const messageContent = data.messageContent || notification.content || 'You have a new message.';
      const isCustomMessage = messageContent && messageContent !== 'You have a new message.';

      const productTitle = data.productTitle || enriched.productTitle;
      const price = data.price != null ? data.price : enriched.price;

      let productLine = null;
      if (productTitle) {
        productLine = `${productTitle}${price != null ? ` · ₹${Number(price).toLocaleString('en-IN')}` : ''}`;
      }

      return {
        title,
        content: isCustomMessage ? `"${messageContent}"` : messageContent,
        productLine,
      };
    }

    return {
      title: notification.title,
      content: notification.content,
      productLine: null,
    };
  };

  return (
    <DashboardLayout>
      <div className="cc-notif-page">
        <div className="cc-notif-container">
          {/* Header */}
          <div className="cc-notif-header">
            <h1 className="cc-notif-header__title">
              Notifications
              {unreadCount > 0 && (
                <span className="cc-notif-header__badge">{unreadCount}</span>
              )}
            </h1>
            {unreadCount > 0 && (
              <button
                type="button"
                className="cc-notif-btn-mark-all"
                onClick={handleMarkAllRead}
                disabled={markingAll}
              >
                {markingAll ? 'Marking…' : 'Mark all as read'}
              </button>
            )}
          </div>

          {/* Toast message for missing target */}
          {toastMessage && (
            <div className="cc-notif-toast" role="alert">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <span>{toastMessage}</span>
            </div>
          )}

          {/* Content */}
          {loading ? (
            <div className="cc-notif-loading">
              <div className="cc-notif-spinner" />
              <p>Loading notifications…</p>
            </div>
          ) : error ? (
            <div className="cc-notif-error">{error}</div>
          ) : notifications.length === 0 ? (
            <div className="cc-notif-empty">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="1.5">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
              <p>No notifications yet</p>
              <span>Activity from orders, messages, and listings will appear here.</span>
            </div>
          ) : (
            <>
              <div className="cc-notif-list">
                {notifications.map((notification) => {
                  const { title, content, productLine } = formatNotification(notification);

                  return (
                    <div
                      key={notification.id}
                      role="button"
                      tabIndex={0}
                      className={`cc-notif-item${!notification.read ? ' cc-notif-item--unread' : ''}`}
                      onClick={() => handleNotificationClick(notification)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          handleNotificationClick(notification);
                        }
                      }}
                      aria-label={`${title}: ${content}${!notification.read ? ' (unread)' : ''}`}
                    >
                      <NotificationIcon type={notification.type} />

                      <div className="cc-notif-body">
                        <div className="cc-notif-body__title">{title}</div>
                        <div className="cc-notif-body__content">{content}</div>

                        {productLine && (
                          <div className="cc-notif-body__product">
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                              <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
                              <line x1="3" y1="6" x2="21" y2="6" />
                            </svg>
                            <span>{productLine}</span>
                          </div>
                        )}

                        <div className="cc-notif-body__time">{relativeTime(notification.createdAt)}</div>
                      </div>

                      {!notification.read && <div className="cc-notif-dot" aria-hidden="true" />}
                    </div>
                  );
                })}
              </div>

              {hasMore && (
                <div className="cc-notif-load-more">
                  <button
                    type="button"
                    className="cc-notif-btn-load-more"
                    onClick={handleLoadMore}
                    disabled={loadingMore}
                  >
                    {loadingMore ? 'Loading…' : 'Load more'}
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
