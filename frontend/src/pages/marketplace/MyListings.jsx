import { useState, useEffect, useCallback } from 'react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import ProductGrid from '../../components/marketplace/ProductGrid';
import EmptyMarketplace from '../../components/marketplace/EmptyMarketplace';
import SellModal from '../../components/marketplace/SellModal';
import api from '../../services/api';
import './MyListings.css';

export default function MyListings() {
  const [activeTab, setActiveTab] = useState('ALL'); // 'ALL', 'ACTIVE', 'SOLD', 'INACTIVE'
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Modals & Actions
  const [sellModalOpen, setSellModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [deleteProductTarget, setDeleteProductTarget] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [toastMessage, setToastMessage] = useState('');

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(''), 3500);
  };

  // Load categories
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

  // Fetch seller's own listings
  const fetchMyListings = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const params = new URLSearchParams();
      params.set('page', String(page));
      params.set('size', '20');
      params.set('sort', 'createdAt,desc');

      if (activeTab !== 'ALL') {
        params.set('status', activeTab);
      }

      const res = await api.get(`/api/v1/products/me?${params.toString()}`);
      if (res.success && res.data) {
        setProducts(res.data.content || []);
        setTotalPages(res.data.totalPages || 0);
        setTotalElements(res.data.totalElements || 0);
      } else {
        setProducts([]);
        setTotalPages(0);
        setTotalElements(0);
      }
    } catch (err) {
      const msg = err?.message || err?.error?.detail || 'Unable to load your listings.';
      setError(msg);
      setProducts([]);
    } finally {
      setLoading(false);
    }
  }, [page, activeTab]);

  useEffect(() => {
    fetchMyListings();
  }, [fetchMyListings]);

  // Handle Edit Action
  const handleEdit = (prod) => {
    setEditingProduct(prod);
    setSellModalOpen(true);
  };

  // Handle Mark Sold Action
  const handleMarkSold = async (prod) => {
    setActionLoading(true);
    try {
      const res = await api.post(`/api/v1/products/${prod.id}/sold`);
      if (res.success) {
        showToast('Listing marked as sold.');
        fetchMyListings();
      }
    } catch (err) {
      showToast(err?.message || 'Failed to mark listing as sold.');
    } finally {
      setActionLoading(false);
    }
  };

  // Handle Toggle Active/Inactive
  const handleToggleActive = async (prod) => {
    setActionLoading(true);
    const endpoint = prod.status === 'INACTIVE' ? 'activate' : 'deactivate';
    try {
      const res = await api.post(`/api/v1/products/${prod.id}/${endpoint}`);
      if (res.success) {
        showToast(`Listing ${endpoint}d successfully.`);
        fetchMyListings();
      }
    } catch (err) {
      showToast(err?.message || 'Failed to update listing status.');
    } finally {
      setActionLoading(false);
    }
  };

  // Handle Delete Confirmation
  const handleDeleteConfirm = async () => {
    if (!deleteProductTarget) return;
    setActionLoading(true);
    try {
      const res = await api.delete(`/api/v1/products/${deleteProductTarget.id}`);
      if (res.success) {
        showToast('Listing deleted successfully.');
        setDeleteProductTarget(null);
        fetchMyListings();
      }
    } catch (err) {
      showToast(err?.message || 'Failed to delete listing.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleModalClose = () => {
    setSellModalOpen(false);
    setEditingProduct(null);
  };

  const handleProductCreated = () => {
    showToast('New listing published!');
    fetchMyListings();
  };

  const handleProductUpdated = () => {
    showToast('Listing updated successfully!');
    fetchMyListings();
  };

  return (
    <DashboardLayout onOpenSell={() => { setEditingProduct(null); setSellModalOpen(true); }}>
      <div className="cc-mylistings-page">
        {/* Header */}
        <div className="cc-mylistings-header">
          <div>
            <h1 className="cc-mylistings-title">My Listings</h1>
            <p className="cc-mylistings-subtitle">
              Manage your products, track inventory, update pricing, and mark items as sold.
            </p>
          </div>
          <button
            type="button"
            className="cc-mylistings-create-btn"
            onClick={() => {
              setEditingProduct(null);
              setSellModalOpen(true);
            }}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>Create New Listing</span>
          </button>
        </div>

        {/* Toast Alert */}
        {toastMessage && (
          <div className="cc-mylistings-toast">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
            <span>{toastMessage}</span>
          </div>
        )}

        {/* Status Tabs */}
        <div className="cc-mylistings-tabs" role="tablist">
          {[
            { id: 'ALL', label: 'All Listings' },
            { id: 'ACTIVE', label: 'Active' },
            { id: 'SOLD', label: 'Sold' },
            { id: 'INACTIVE', label: 'Inactive / Draft' },
          ].map((tab) => {
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={isActive}
                className={`cc-mylistings-tab ${isActive ? 'cc-mylistings-tab--active' : ''}`}
                onClick={() => {
                  setActiveTab(tab.id);
                  setPage(0);
                }}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Error Alert */}
        {error && (
          <div className="cc-mylistings-error">
            <p>{error}</p>
            <button type="button" onClick={fetchMyListings}>Retry</button>
          </div>
        )}

        {/* Listings Grid */}
        <ProductGrid
          products={products}
          loading={loading}
          skeletonCount={6}
          showSellerActions={true}
          onEdit={handleEdit}
          onDelete={(prod) => setDeleteProductTarget(prod)}
          onMarkSold={handleMarkSold}
          onToggleActive={handleToggleActive}
        />

        {/* Empty State */}
        {!loading && !error && products.length === 0 && (
          <EmptyMarketplace
            title={
              activeTab === 'SOLD'
                ? 'No sold items yet'
                : activeTab === 'INACTIVE'
                ? 'No inactive listings'
                : 'You have no listings yet'
            }
            description={
              activeTab === 'SOLD'
                ? 'When an item sells, mark it as sold to keep your inventory updated.'
                : 'List your textbooks, notes, electronics, or dorm items to start selling to fellow students.'
            }
            actionLabel="List an Item Now"
            onAction={() => {
              setEditingProduct(null);
              setSellModalOpen(true);
            }}
          />
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

      {/* Create / Edit Modal */}
      <SellModal
        isOpen={sellModalOpen}
        onClose={handleModalClose}
        categories={categories}
        initialProduct={editingProduct}
        onProductCreated={handleProductCreated}
        onProductUpdated={handleProductUpdated}
      />

      {/* Delete Confirmation Modal */}
      {deleteProductTarget && (
        <div className="cc-modal-overlay" onClick={() => setDeleteProductTarget(null)}>
          <div className="cc-confirm-modal" onClick={(e) => e.stopPropagation()}>
            <h3>Delete Listing?</h3>
            <p>
              Are you sure you want to permanently delete &ldquo;{deleteProductTarget.title}&rdquo;?
            </p>
            <div className="cc-confirm-modal__actions">
              <button
                type="button"
                className="cc-confirm-modal__btn-cancel"
                onClick={() => setDeleteProductTarget(null)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="cc-confirm-modal__btn-delete"
                onClick={handleDeleteConfirm}
                disabled={actionLoading}
              >
                {actionLoading ? 'Deleting...' : 'Delete Listing'}
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
