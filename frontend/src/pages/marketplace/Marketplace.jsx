import { useState, useEffect, useCallback, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import MarketplaceTabs from '../../components/marketplace/MarketplaceTabs';
import MarketplaceFilters from '../../components/marketplace/MarketplaceFilters';
import ProductGrid from '../../components/marketplace/ProductGrid';
import EmptyMarketplace from '../../components/marketplace/EmptyMarketplace';
import SellModal from '../../components/marketplace/SellModal';
import api from '../../services/api';
import './Marketplace.css';

export default function Marketplace() {
  const [searchParams, setSearchParams] = useSearchParams();

  // Filter & Search states (hydrated from searchParams where applicable)
  const initialCategory = searchParams.get('categoryId') || null;
  const initialScope = searchParams.get('scope') || 'ALL_PRODUCTS';
  const initialKeyword = searchParams.get('keyword') || searchParams.get('q') || '';
  const initialProductType = searchParams.get('productType') || null;

  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(initialCategory);
  const [activeScope, setActiveScope] = useState(initialScope);
  const [productType, setProductType] = useState(initialProductType);
  const [sellingReach, setSellingReach] = useState(null);
  const [priceRange, setPriceRange] = useState({ minPrice: undefined, maxPrice: undefined });
  const [sort, setSort] = useState('createdAt,desc');
  const [searchQuery, setSearchQuery] = useState(initialKeyword);
  const [debouncedKeyword, setDebouncedKeyword] = useState(initialKeyword);

  // Pagination states
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Data states
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [wishlistIds, setWishlistIds] = useState(new Set());

  // Modal state
  const [sellModalOpen, setSellModalOpen] = useState(false);

  // Debounce search query input (380ms)
  const debounceTimerRef = useRef(null);
  const handleSearchChange = (val) => {
    setSearchQuery(val);
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }
    debounceTimerRef.current = setTimeout(() => {
      setDebouncedKeyword(val.trim());
      setPage(0); // Reset to page 0 on search
    }, 380);
  };

  // Load categories once
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

  // Load wishlist IDs
  useEffect(() => {
    let cancelled = false;
    api.get('/api/v1/wishlist?page=0&size=100')
      .then((res) => {
        if (!cancelled && res.success && res.data?.content) {
          const ids = new Set(res.data.content.map((item) => item.product?.id || item.productId).filter(Boolean));
          setWishlistIds(ids);
        }
      })
      .catch(() => {});
    return () => { cancelled = true; };
  }, []);

  // Fetch products whenever filters / scope / search / page change
  const fetchProducts = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const params = new URLSearchParams();
      params.set('page', String(page));
      params.set('size', String(pageSize));
      params.set('sort', sort);
      params.set('scope', activeScope);

      if (debouncedKeyword) params.set('keyword', debouncedKeyword);
      if (selectedCategory) params.set('categoryId', selectedCategory);
      if (productType) params.set('productType', productType);
      if (sellingReach) params.set('sellingReach', sellingReach);
      if (priceRange.minPrice !== undefined && priceRange.minPrice !== '') {
        params.set('minPrice', priceRange.minPrice);
      }
      if (priceRange.maxPrice !== undefined && priceRange.maxPrice !== '') {
        params.set('maxPrice', priceRange.maxPrice);
      }

      const res = await api.get(`/api/v1/products?${params.toString()}`);
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
      const msg = err?.message || err?.error?.detail || 'Unable to load products. Please check your connection.';
      setError(msg);
      setProducts([]);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, sort, activeScope, debouncedKeyword, selectedCategory, productType, sellingReach, priceRange]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  // Wishlist local toggle
  const handleWishlistToggle = (productId, nextState) => {
    setWishlistIds((prev) => {
      const copy = new Set(prev);
      if (nextState) {
        copy.add(productId);
      } else {
        copy.delete(productId);
      }
      return copy;
    });
  };

  // Filter resets
  const handleClearFilters = () => {
    setSelectedCategory(null);
    setProductType(null);
    setSellingReach(null);
    setPriceRange({ minPrice: undefined, maxPrice: undefined });
    setSearchQuery('');
    setDebouncedKeyword('');
    setPage(0);
  };

  const countActiveFilters = () => {
    let count = 0;
    if (selectedCategory) count++;
    if (productType) count++;
    if (sellingReach) count++;
    if (priceRange.minPrice || priceRange.maxPrice) count++;
    if (debouncedKeyword) count++;
    return count;
  };

  const handleProductCreated = () => {
    fetchProducts();
  };

  return (
    <DashboardLayout
      onSearch={handleSearchChange}
      searchQuery={searchQuery}
      onOpenSell={() => setSellModalOpen(true)}
    >
      <div className="cc-marketplace-page">
        {/* Header Title & Sell CTA */}
        <div className="cc-marketplace-header">
          <div className="cc-marketplace-header__left">
            <h1 className="cc-marketplace-title">Browse Marketplace</h1>
            <p className="cc-marketplace-subtitle">
              Discover textbooks, electronics, dorm essentials and student gear across campus.
            </p>
          </div>

          <div className="cc-marketplace-header__right">
            <button
              type="button"
              className="cc-marketplace-sell-btn"
              onClick={() => setSellModalOpen(true)}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              <span>Sell an Item</span>
            </button>
          </div>
        </div>

        {/* Scope Tabs */}
        <MarketplaceTabs
          activeScope={activeScope}
          onScopeChange={(newScope) => {
            setActiveScope(newScope);
            setPage(0);
          }}
        />

        {/* Filters and Controls */}
        <MarketplaceFilters
          categories={categories}
          selectedCategory={selectedCategory}
          onSelectCategory={(catId) => {
            setSelectedCategory(catId);
            setPage(0);
          }}
          productType={productType}
          onProductTypeChange={(type) => {
            setProductType(type);
            setPage(0);
          }}
          sellingReach={sellingReach}
          onSellingReachChange={(reach) => {
            setSellingReach(reach);
            setPage(0);
          }}
          minPrice={priceRange.minPrice}
          maxPrice={priceRange.maxPrice}
          onPriceChange={(range) => {
            setPriceRange(range);
            setPage(0);
          }}
          sort={sort}
          onSortChange={(newSort) => {
            setSort(newSort);
            setPage(0);
          }}
          onClearFilters={handleClearFilters}
          activeFilterCount={countActiveFilters()}
        />

        {/* Results Count Bar */}
        {!loading && !error && (
          <div className="cc-results-count-bar">
            <span>
              Showing {products.length} of {totalElements} product{totalElements === 1 ? '' : 's'}
            </span>
            {debouncedKeyword && (
              <span className="cc-keyword-indicator">
                matching &ldquo;<strong>{debouncedKeyword}</strong>&rdquo;
              </span>
            )}
          </div>
        )}

        {/* Error Alert */}
        {error && (
          <div className="cc-marketplace-error">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <div className="cc-marketplace-error__content">
              <p className="cc-marketplace-error__msg">{error}</p>
              <button
                type="button"
                className="cc-marketplace-error__retry-btn"
                onClick={fetchProducts}
              >
                Retry
              </button>
            </div>
          </div>
        )}

        {/* Products Grid */}
        <ProductGrid
          products={products}
          loading={loading}
          skeletonCount={8}
          wishlistIds={wishlistIds}
          onWishlistToggle={handleWishlistToggle}
        />

        {/* Empty State */}
        {!loading && !error && products.length === 0 && (
          <EmptyMarketplace
            title="No listings found"
            description="We couldn't find any products matching your selected scope or filters. Try resetting filters or post an item for sale!"
            actionLabel="Reset All Filters"
            onAction={handleClearFilters}
            secondaryActionLabel="Post an Item"
            onSecondaryAction={() => setSellModalOpen(true)}
          />
        )}

        {/* Pagination Controls */}
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

            <div className="cc-pagination__pages">
              {Array.from({ length: totalPages }).map((_, idx) => {
                // Show first, last, and current +/- 2
                if (
                  idx === 0 ||
                  idx === totalPages - 1 ||
                  (idx >= page - 2 && idx <= page + 2)
                ) {
                  return (
                    <button
                      key={idx}
                      type="button"
                      className={`cc-pagination__page-num ${page === idx ? 'cc-pagination__page-num--active' : ''}`}
                      onClick={() => setPage(idx)}
                    >
                      {idx + 1}
                    </button>
                  );
                }
                if (idx === page - 3 || idx === page + 3) {
                  return (
                    <span key={idx} className="cc-pagination__ellipsis">
                      ...
                    </span>
                  );
                }
                return null;
              })}
            </div>

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

      {/* Sell Modal */}
      <SellModal
        isOpen={sellModalOpen}
        onClose={() => setSellModalOpen(false)}
        categories={categories}
        onProductCreated={handleProductCreated}
      />
    </DashboardLayout>
  );
}
