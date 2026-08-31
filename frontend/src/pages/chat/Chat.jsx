import { useState, useEffect, useCallback, useRef } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import api from '../../services/api';
import './Chat.css';

export default function Chat() {
  const [searchParams, setSearchParams] = useSearchParams();
  const targetConvId = searchParams.get('conversationId');

  const [conversations, setConversations] = useState([]);
  const [selectedConv, setSelectedConv] = useState(null);
  const [messages, setMessages] = useState([]);
  const [loadingConvs, setLoadingConvs] = useState(true);
  const [loadingMsgs, setLoadingMsgs] = useState(false);
  const [sendingMsg, setSendingMsg] = useState(false);
  const [inputText, setInputText] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [productDetails, setProductDetails] = useState(null);

  // Image modal & upload state
  const [uploadingImage, setUploadingImage] = useState(false);
  const [previewImage, setPreviewImage] = useState(null);
  const fileInputRef = useRef(null);
  const messagesEndRef = useRef(null);

  // Get current user id from localStorage or auth
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

  // Scroll to bottom of message thread
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // Fetch all conversations
  const fetchConversations = useCallback(async () => {
    try {
      const res = await api.get('/api/v1/conversations?page=0&size=50');
      if (res.success && res.data) {
        const list = res.data.content || [];
        setConversations(list);
        return list;
      }
    } catch {
      // Ignore
    } finally {
      setLoadingConvs(false);
    }
    return [];
  }, []);

  // Fetch messages for active conversation
  const fetchMessages = useCallback(async (convId) => {
    if (!convId) return;
    try {
      const res = await api.get(`/api/v1/conversations/${convId}/messages?page=0&size=100`);
      if (res.success && res.data) {
        setMessages(res.data.content || []);
      }
    } catch {
      // Ignore
    }
  }, []);

  // Fetch product info for pinned header
  const fetchProductForConv = useCallback(async (productId) => {
    if (!productId) {
      setProductDetails(null);
      return;
    }
    try {
      const res = await api.get(`/api/v1/products/${productId}`);
      if (res.success && res.data) {
        setProductDetails(res.data);
      }
    } catch {
      setProductDetails(null);
    }
  }, []);

  // Mark conversation as read
  const markAsRead = async (convId) => {
    try {
      await api.post(`/api/v1/conversations/${convId}/read`);
      window.dispatchEvent(new CustomEvent('campuscart-unread-updated'));
      setConversations((prev) =>
        prev.map((c) => (c.id === convId ? { ...c, unreadCount: 0 } : c))
      );
    } catch {
      // Ignore
    }
  };

  // Initial load
  useEffect(() => {
    fetchConversations().then((list) => {
      if (targetConvId && list.length > 0) {
        const match = list.find((c) => c.id === targetConvId);
        if (match) {
          setSelectedConv(match);
        }
      } else if (!selectedConv && list.length > 0) {
        setSelectedConv(list[0]);
      }
    });
  }, [fetchConversations, targetConvId]);

  // When selected conversation changes
  useEffect(() => {
    if (!selectedConv) return;
    setLoadingMsgs(true);
    fetchMessages(selectedConv.id).finally(() => {
      setLoadingMsgs(false);
      scrollToBottom();
    });
    fetchProductForConv(selectedConv.productId);
    markAsRead(selectedConv.id);
  }, [selectedConv, fetchMessages, fetchProductForConv]);

  // Polling for live incoming messages every 4s
  useEffect(() => {
    if (!selectedConv) return;
    const interval = setInterval(() => {
      fetchMessages(selectedConv.id);
    }, 4000);
    return () => clearInterval(interval);
  }, [selectedConv, fetchMessages]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Handle select conversation
  const handleSelectConv = (conv) => {
    setSelectedConv(conv);
    setSearchParams({ conversationId: conv.id });
  };

  // Send Text Message
  const handleSendText = async (e) => {
    if (e) e.preventDefault();
    if (!inputText.trim() || !selectedConv || sendingMsg) return;

    const textToSend = inputText.trim();
    setInputText('');
    setSendingMsg(true);

    try {
      const res = await api.post(`/api/v1/conversations/${selectedConv.id}/messages`, {
        content: textToSend,
      });
      if (res.success && res.data) {
        setMessages((prev) => [...prev, res.data]);
        fetchConversations();
      }
    } catch (err) {
      alert(err?.message || 'Failed to send message.');
      setInputText(textToSend);
    } finally {
      setSendingMsg(false);
    }
  };

  // Send Image Message
  const handleImageSelected = async (e) => {
    const file = e.target.files?.[0];
    if (!file || !selectedConv) return;

    if (file.size > 5 * 1024 * 1024) {
      alert('Image size exceeds 5MB limit.');
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    setUploadingImage(true);
    try {
      const res = await api.post(
        `/api/v1/conversations/${selectedConv.id}/messages/image`,
        formData,
        {
          headers: { 'Content-Type': 'multipart/form-data' },
        }
      );
      if (res.success && res.data) {
        setMessages((prev) => [...prev, res.data]);
        fetchConversations();
      }
    } catch (err) {
      alert(err?.message || 'Failed to upload and send image.');
    } finally {
      setUploadingImage(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  // Counterparty helper
  const getCounterpartyName = (conv) => {
    if (!conv) return 'Student';
    if (currentUserId && conv.buyerId === currentUserId) {
      return conv.sellerName || 'Seller';
    }
    return conv.buyerName || 'Buyer';
  };

  const formatMessageTime = (iso) => {
    if (!iso) return '';
    return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const filteredConversations = conversations.filter((c) => {
    const term = searchTerm.toLowerCase();
    return (
      (c.sellerName && c.sellerName.toLowerCase().includes(term)) ||
      (c.buyerName && c.buyerName.toLowerCase().includes(term)) ||
      (c.productTitle && c.productTitle.toLowerCase().includes(term))
    );
  });

  return (
    <DashboardLayout>
      <div className="cc-chat-page">
        {/* Chat Layout Container */}
        <div className="cc-chat-container">
          {/* ─── Left Sidebar: Conversations List ─── */}
          <div className="cc-chat-sidebar">
            <div className="cc-chat-sidebar__header">
              <h2>Messages</h2>
              <div className="cc-chat-search-box">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="8" />
                  <line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
                <input
                  type="text"
                  placeholder="Search chats..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>

            <div className="cc-chat-conversations-list">
              {loadingConvs ? (
                <div className="cc-chat-convs-loading">
                  <div className="cc-chat-mini-spinner" />
                  <span>Loading conversations...</span>
                </div>
              ) : filteredConversations.length === 0 ? (
                <div className="cc-chat-convs-empty">
                  <p>No conversations yet.</p>
                  <span>Click &ldquo;Chat with Seller&rdquo; on any listing to start.</span>
                </div>
              ) : (
                filteredConversations.map((conv) => {
                  const isSelected = selectedConv && selectedConv.id === conv.id;
                  const otherPartyName = getCounterpartyName(conv);
                  const initial = (otherPartyName || 'C').substring(0, 1).toUpperCase();

                  return (
                    <div
                      key={conv.id}
                      className={`cc-chat-conv-item ${isSelected ? 'cc-chat-conv-item--active' : ''}`}
                      onClick={() => handleSelectConv(conv)}
                    >
                      <div className="cc-chat-conv-avatar">{initial}</div>

                      <div className="cc-chat-conv-info">
                        <div className="cc-chat-conv-top">
                          <span className="cc-chat-conv-name">{otherPartyName}</span>
                          {conv.unreadCount > 0 && (
                            <span className="cc-chat-unread-badge">{conv.unreadCount}</span>
                          )}
                        </div>

                        <span className="cc-chat-conv-product" title={conv.productTitle}>
                          Item: {conv.productTitle || 'Campus Listing'}
                        </span>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>

          {/* ─── Right Pane: Active Chat ─── */}
          <div className="cc-chat-main">
            {selectedConv ? (
              <>
                {/* Active Chat Header */}
                <div className="cc-chat-main-header">
                  <div className="cc-chat-header-user">
                    <div className="cc-chat-header-avatar">
                      {(getCounterpartyName(selectedConv) || 'C').substring(0, 1).toUpperCase()}
                    </div>
                    <div>
                      <h3 className="cc-chat-header-name">{getCounterpartyName(selectedConv)}</h3>
                      <span className="cc-chat-header-status">Campus Member &bull; Online</span>
                    </div>
                  </div>
                </div>

                {/* Pinned Product Card */}
                {selectedConv.productId && (
                  <div className="cc-chat-pinned-product">
                    <div className="cc-chat-pinned-info">
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
                        <line x1="3" y1="6" x2="21" y2="6" />
                      </svg>
                      <div>
                        <strong>{selectedConv.productTitle}</strong>
                        {productDetails && (
                          <span className="cc-chat-pinned-price">
                            &bull; ₹{Number(productDetails.price).toLocaleString('en-IN')}
                          </span>
                        )}
                      </div>
                    </div>
                    <Link
                      to={`/products/${selectedConv.productId}`}
                      className="cc-chat-pinned-btn"
                    >
                      View Listing &rarr;
                    </Link>
                  </div>
                )}

                {/* Messages Stream */}
                <div className="cc-chat-messages-area">
                  {loadingMsgs ? (
                    <div className="cc-chat-msgs-loading">
                      <div className="cc-chat-mini-spinner" />
                      <span>Loading messages...</span>
                    </div>
                  ) : messages.length === 0 ? (
                    <div className="cc-chat-msgs-empty">
                      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="1.5">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                      </svg>
                      <p>Start your conversation regarding &ldquo;{selectedConv.productTitle}&rdquo;.</p>
                      <span>Ask questions about condition, negotiate price, or arrange on-campus pickup.</span>
                    </div>
                  ) : (
                    messages.map((msg) => {
                      const isMe = currentUserId ? msg.senderId === currentUserId : false;

                      return (
                        <div
                          key={msg.id}
                          className={`cc-chat-msg-row ${isMe ? 'cc-chat-msg-row--me' : 'cc-chat-msg-row--them'}`}
                        >
                          <div className={`cc-chat-bubble ${isMe ? 'cc-chat-bubble--me' : 'cc-chat-bubble--them'}`}>
                            {/* Text Message */}
                            {msg.messageType === 'TEXT' && (
                              <p className="cc-chat-msg-text">{msg.content}</p>
                            )}

                            {/* Image Message */}
                            {msg.messageType === 'IMAGE' && (
                              <div className="cc-chat-msg-image-wrap">
                                <img
                                  src={msg.imageUrl}
                                  alt="Attachment"
                                  className="cc-chat-msg-image"
                                  onClick={() => setPreviewImage(msg.imageUrl)}
                                />
                              </div>
                            )}

                            {/* Product Share Message */}
                            {msg.messageType === 'PRODUCT' && (
                              <div className="cc-chat-msg-product-card">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                  <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
                                </svg>
                                <span>Shared Product Listing</span>
                              </div>
                            )}

                            <span className="cc-chat-msg-time">
                              {formatMessageTime(msg.createdAt)}
                            </span>
                          </div>
                        </div>
                      );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </div>

                {/* Message Input Bar */}
                <form className="cc-chat-input-bar" onSubmit={handleSendText}>
                  {/* Image Attachment Button */}
                  <input
                    type="file"
                    ref={fileInputRef}
                    style={{ display: 'none' }}
                    accept="image/jpeg,image/png,image/webp"
                    onChange={handleImageSelected}
                  />
                  <button
                    type="button"
                    className="cc-chat-btn-attach"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploadingImage || sendingMsg}
                    title="Send image (max 5MB)"
                  >
                    {uploadingImage ? (
                      <div className="cc-chat-mini-spinner" />
                    ) : (
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                        <circle cx="8.5" cy="8.5" r="1.5" />
                        <polyline points="21 15 16 10 5 21" />
                      </svg>
                    )}
                  </button>

                  {/* Input field */}
                  <input
                    type="text"
                    className="cc-chat-input"
                    placeholder="Type a message... (Press Enter to send)"
                    value={inputText}
                    onChange={(e) => setInputText(e.target.value)}
                    disabled={sendingMsg || uploadingImage}
                  />

                  {/* Send Button */}
                  <button
                    type="submit"
                    className="cc-chat-btn-send"
                    disabled={!inputText.trim() || sendingMsg || uploadingImage}
                  >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="22" y1="2" x2="11" y2="13" />
                      <polygon points="22 2 15 22 11 13 2 9 22 2" />
                    </svg>
                  </button>
                </form>
              </>
            ) : (
              /* No Conversation Selected */
              <div className="cc-chat-no-selection">
                <div className="cc-chat-no-selection__icon">
                  <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="1.5">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                  </svg>
                </div>
                <h2>CampusCart Messaging</h2>
                <p>Select an ongoing conversation from the sidebar or click &ldquo;Chat with Seller&rdquo; on any marketplace listing.</p>
              </div>
            )}
          </div>
        </div>

        {/* Full Image Preview Modal */}
        {previewImage && (
          <div className="cc-modal-overlay" onClick={() => setPreviewImage(null)}>
            <div className="cc-chat-image-preview-modal" onClick={(e) => e.stopPropagation()}>
              <img src={previewImage} alt="Attachment Full View" />
              <button
                type="button"
                className="cc-chat-preview-close"
                onClick={() => setPreviewImage(null)}
              >
                &times;
              </button>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
