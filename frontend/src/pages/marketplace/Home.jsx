import { useState, useEffect, useCallback, useId } from 'react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import SellModal from '../../components/marketplace/SellModal';
import api from '../../services/api';
import './Home.css';

// SVG Category Icons helper
function getCategoryIcon(slug, name) {
  const s = (slug || name || '').toLowerCase();
  if (s.includes('book') || s.includes('note') || s.includes('study') || s.includes('academic')) {
    return (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
      </svg>
    );
  }
  if (s.includes('elect') || s.includes('gadget') || s.includes('laptop') || s.includes('phone') || s.includes('tech')) {
    return (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
        <line x1="8" y1="21" x2="16" y2="21" />
        <line x1="12" y1="17" x2="12" y2="21" />
      </svg>
    );
  }
  if (s.includes('cloth') || s.includes('fash') || s.includes('wear') || s.includes('apparel')) {
    return (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M20.38 3.46L16 2a4 4 0 0 1-8 0L3.62 3.46a2 2 0 0 0-1.34 2.23l.58 3.47a1 1 0 0 0 .99.84H6v10c0 1.1.9 2 2 2h8a2 2 0 0 0 2-2V10h2.15a1 1 0 0 0 .99-.84l.58-3.47a2 2 0 0 0-1.34-2.23z" />
      </svg>
    );
  }
  if (s.includes('furn') || s.includes('dorm') || s.includes('chair') || s.includes('desk') || s.includes('bed')) {
    return (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M19 9V6a2 2 0 0 0-2-2H7a2 2 0 0 0-2 2v3" />
        <path d="M3 16a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v5z" />
        <path d="M5 18v3" />
        <path d="M19 18v3" />
      </svg>
    );
  }
  if (s.includes('sport') || s.includes('gym') || s.includes('fit') || s.includes('cycle') || s.includes('game')) {
    return (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="10" />
        <path d="M4.93 4.93l4.24 4.24" />
        <path d="M14.83 14.83l4.24 4.24" />
        <path d="M14.83 9.17l4.24-4.24" />
        <path d="M4.93 19.07l4.24-4.24" />
      </svg>
    );
  }
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="3" width="7" height="7" rx="1" />
      <rect x="14" y="3" width="7" height="7" rx="1" />
      <rect x="14" y="14" width="7" height="7" rx="1" />
      <rect x="3" y="14" width="7" height="7" rx="1" />
    </svg>
  );
}

export default function Home() {
  const [categories, setCategories] = useState([]);
  const [categoriesLoading, setCategoriesLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState(null);

  const [popularProducts, setPopularProducts] = useState([]);
  const [recentProducts, setRecentProducts] = useState([]);
  const [productsLoading, setProductsLoading] = useState(true);

  const [wishlistIds, setWishlistIds] = useState(new Set());
  const [searchQuery, setSearchQuery] = useState('');
  const [sellModalOpen, setSellModalOpen] = useState(false);

  const grad1Id = useId();
  const grad2Id = useId();

  // Initial Data Load
  useEffect(() => {
    let cancelled = false;

    const loadInitialData = async () => {
      try {
        const [catRes, prodRes, wishRes] = await Promise.allSettled([
          api.get('/api/v1/categories'),
          api.get('/api/v1/products?page=0&size=8&sort=createdAt,desc'),
          api.get('/api/v1/wishlist?page=0&size=50'),
        ]);

        if (!cancelled) {
          if (catRes.status === 'fulfilled' && catRes.value?.success && Array.isArray(catRes.value.data)) {
            setCategories(catRes.value.data);
          }
          if (prodRes.status === 'fulfilled' && prodRes.value?.success && prodRes.value.data?.content) {
            setPopularProducts(prodRes.value.data.content);
            setRecentProducts(prodRes.value.data.content.slice(0, 4));
          }
          if (wishRes.status === 'fulfilled' && wishRes.value?.success && wishRes.value.data?.content) {
            setWishlistIds(new Set(wishRes.value.data.content.map((w) => w.productId)));
          }
        }
      } catch {
        // Fallback
      } finally {
        if (!cancelled) {
          setCategoriesLoading(false);
          setProductsLoading(false);
        }
      }
    };

    loadInitialData();

    return () => {
      cancelled = true;
    };
  }, []);

  // Fetch Products on Category or Search Filter
  const fetchFilteredProducts = useCallback(async (catId, query) => {
    try {
      setProductsLoading(true);
      let url = '/api/v1/products?page=0&size=8&sort=createdAt,desc';
      if (catId) url += `&categoryId=${catId}`;
      if (query) url += `&keyword=${encodeURIComponent(query)}`;

      const res = await api.get(url);
      if (res.success && res.data?.content) {
        setPopularProducts(res.data.content);
        setRecentProducts(res.data.content.slice(0, 4));
      } else {
        setPopularProducts([]);
        setRecentProducts([]);
      }
    } catch {
      setPopularProducts([]);
      setRecentProducts([]);
    } finally {
      setProductsLoading(false);
    }
  }, []);

  // Handle Category Filter Click
  const handleCategoryClick = (catId) => {
    const nextCat = selectedCategory === catId ? null : catId;
    setSelectedCategory(nextCat);
    fetchFilteredProducts(nextCat, searchQuery);
  };

  // Handle Search Input Change
  const handleSearch = (q) => {
    setSearchQuery(q);
    fetchFilteredProducts(selectedCategory, q);
  };

  // Handle Wishlist Toggle
  const handleToggleWishlist = async (productId, e) => {
    e.stopPropagation();
    const isWishlisted = wishlistIds.has(productId);
    try {
      if (isWishlisted) {
        await api.delete(`/api/v1/wishlist/${productId}`);
        setWishlistIds((prev) => {
          const next = new Set(prev);
          next.delete(productId);
          return next;
        });
      } else {
        await api.post(`/api/v1/wishlist/${productId}`);
        setWishlistIds((prev) => new Set(prev).add(productId));
      }
    } catch {
      // Wishlist toggle error
    }
  };

  // Format currency
  const formatPrice = (val) => {
    if (val == null) return '₹0';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(val);
  };

  return (
    <DashboardLayout
      onSearch={handleSearch}
      searchQuery={searchQuery}
      onOpenSell={() => setSellModalOpen(true)}
    >
      <div className="home-container">
        {/* ─── Hero Banner ─── */}
        <section className="home-hero">
          <div className="home-hero__content">
            <h1 className="home-hero__title">
              Buy. Sell. Connect.
              <span className="home-hero__title-blue">All on Campus.</span>
            </h1>
            <p className="home-hero__subtitle">
              Join thousands of students buying and selling trusted items across your campus.
            </p>
            <div className="home-hero__actions">
              <button
                type="button"
                className="home-hero__btn-primary"
                onClick={() => {
                  const el = document.getElementById('marketplace-listings');
                  if (el) el.scrollIntoView({ behavior: 'smooth' });
                }}
              >
                Explore Now
              </button>
              <button
                type="button"
                className="home-hero__btn-secondary"
                onClick={() => setSellModalOpen(true)}
              >
                Sell an Item
              </button>
            </div>
          </div>

          <div className="home-hero__illustration">
            {/* Approved Clean Vector Campus Graphic */}
            <svg
              className="home-hero__svg"
              viewBox="0 0 420 240"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <defs>
                <linearGradient id={grad1Id} x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#bfdbfe" stopOpacity="0.4" />
                  <stop offset="100%" stopColor="#93c5fd" stopOpacity="0.2" />
                </linearGradient>
                <linearGradient id={grad2Id} x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stopColor="#2563eb" />
                  <stop offset="100%" stopColor="#1d4ed8" />
                </linearGradient>
              </defs>

              {/* Background Cloud & Soft Shapes */}
              <circle cx="90" cy="50" r="28" fill="#e0f2fe" opacity="0.6" />
              <circle cx="120" cy="45" r="36" fill="#e0f2fe" opacity="0.6" />
              <circle cx="150" cy="52" r="24" fill="#e0f2fe" opacity="0.6" />

              {/* Campus Building */}
              <rect x="230" y="70" width="180" height="150" rx="4" fill="#ffffff" stroke="#cbd5e1" strokeWidth="1.5" />
              <rect x="290" y="20" width="60" height="180" rx="3" fill="#ffffff" stroke="#cbd5e1" strokeWidth="1.5" />
              <polygon points="320,0 280,22 360,22" fill="#3b82f6" />
              <circle cx="320" cy="50" r="14" fill="#eff6ff" stroke="#3b82f6" strokeWidth="1.5" />
              <line x1="320" y1="50" x2="320" y2="42" stroke="#3b82f6" strokeWidth="2" strokeLinecap="round" />
              <line x1="320" y1="50" x2="328" y2="50" stroke="#3b82f6" strokeWidth="2" strokeLinecap="round" />

              {/* Windows Grid */}
              <rect x="245" y="85" width="18" height="14" rx="2" fill="#bfdbfe" />
              <rect x="270" y="85" width="18" height="14" rx="2" fill="#bfdbfe" />
              <rect x="352" y="85" width="18" height="14" rx="2" fill="#bfdbfe" />
              <rect x="378" y="85" width="18" height="14" rx="2" fill="#bfdbfe" />
              <rect x="245" y="115" width="18" height="14" rx="2" fill="#bfdbfe" />
              <rect x="270" y="115" width="18" height="14" rx="2" fill="#bfdbfe" />
              <rect x="352" y="115" width="18" height="14" rx="2" fill="#bfdbfe" />
              <rect x="378" y="115" width="18" height="14" rx="2" fill="#bfdbfe" />
              <rect x="306" y="170" width="28" height="50" rx="4" fill="#2563eb" />

              {/* Trees */}
              <circle cx="210" cy="180" r="26" fill="#86efac" />
              <rect x="207" y="196" width="6" height="30" fill="#a16207" />
              <circle cx="180" cy="190" r="20" fill="#bbf7d0" />
              <rect x="178" y="202" width="4" height="24" fill="#a16207" />

              {/* Student 1 (Left with backpack and phone) */}
              <circle cx="130" cy="130" r="12" fill="#fed7aa" />
              <path d="M124 122 Q130 116 136 122 Q140 128 136 130 Z" fill="#1e293b" />
              <rect x="122" y="144" width="20" height="42" rx="6" fill="#2563eb" />
              <rect x="114" y="148" width="10" height="26" rx="4" fill="#3b82f6" />
              <line x1="126" y1="186" x2="124" y2="226" stroke="#1e293b" strokeWidth="6" strokeLinecap="round" />
              <line x1="138" y1="186" x2="140" y2="226" stroke="#1e293b" strokeWidth="6" strokeLinecap="round" />
              <rect x="144" y="150" width="8" height="14" rx="2" fill="#0f172a" />

              {/* Student 2 (Right holding tablet) */}
              <circle cx="185" cy="136" r="11" fill="#fed7aa" />
              <path d="M178 128 Q185 120 192 128 Q195 138 186 142 Z" fill="#1e293b" />
              <rect x="177" y="148" width="18" height="38" rx="6" fill="#fbbf24" />
              <line x1="180" y1="186" x2="178" y2="226" stroke="#1e293b" strokeWidth="5" strokeLinecap="round" />
              <line x1="190" y1="186" x2="192" y2="226" stroke="#1e293b" strokeWidth="5" strokeLinecap="round" />
              <rect x="166" y="156" width="14" height="18" rx="2" fill="#1e293b" />
            </svg>
          </div>
        </section>

        {/* ─── Top Categories ─── */}
        <section className="home-section">
          <div className="home-section__header">
            <h2 className="home-section__title">Top Categories</h2>
            <button
              type="button"
              className="home-section__link"
              onClick={() => {
                setSelectedCategory(null);
                fetchFilteredProducts(null, searchQuery);
              }}
            >
              View all categories
            </button>
          </div>

          <div className="home-categories-grid">
            {categoriesLoading ? (
              Array.from({ length: 6 }).map((_, idx) => (
                <div key={idx} className="home-skeleton" style={{ height: '76px' }} />
              ))
            ) : categories.length > 0 ? (
              categories.map((cat) => {
                const isSelected = selectedCategory === cat.id;
                return (
                  <div
                    key={cat.id}
                    className={`home-category-card ${isSelected ? 'home-category-card--active' : ''}`}
                    onClick={() => handleCategoryClick(cat.id)}
                  >
                    <div className="home-category-card__icon-box">
                      {getCategoryIcon(cat.slug, cat.name)}
                    </div>
                    <div className="home-category-card__info">
                      <span className="home-category-card__name" title={cat.name}>
                        {cat.name}
                      </span>
                      <span className="home-category-card__count">    </span>
                    </div>
                  </div>
                );
              })
            ) : (
              <div className="home-category-card">
                <div className="home-category-card__icon-box">
                  {getCategoryIcon('all', 'All')}
                </div>
                <div className="home-category-card__info">
                  <span className="home-category-card__name">All Categories</span>
                  <span className="home-category-card__count">Active</span>
                </div>
              </div>
            )}
          </div>
        </section>

        {/* ─── Popular Listings ─── */}
        <section className="home-section" id="marketplace-listings">
          <div className="home-section__header">
            <h2 className="home-section__title">Popular Listings</h2>
            <button
              type="button"
              className="home-section__link"
              onClick={() => fetchFilteredProducts(selectedCategory, searchQuery)}
            >
              View all
            </button>
          </div>

          {productsLoading ? (
            <div className="home-products-grid">
              {Array.from({ length: 4 }).map((_, idx) => (
                <div key={idx} className="home-skeleton" style={{ height: '280px' }} />
              ))}
            </div>
          ) : popularProducts.length > 0 ? (
            <div className="home-products-grid">
              {popularProducts.map((product) => {
                const isWishlisted = wishlistIds.has(product.id);
                const hasImage = product.images && product.images.length > 0;
                return (
                  <div key={product.id} className="home-product-card">
                    <div className="home-product-card__image-container">
                      {hasImage ? (
                        <img
                          src={product.images[0].imageUrl || product.images[0].url}
                          alt={product.title}
                          className="home-product-card__image"
                        />
                      ) : (
                        <div className="home-product-card__placeholder-icon">
                          {getCategoryIcon(product.categorySlug, product.categoryName)}
                        </div>
                      )}
                      <span className="home-product-card__badge-new">NEW</span>
                      <button
                        type="button"
                        className={`home-product-card__wishlist-btn ${isWishlisted ? 'home-product-card__wishlist-btn--active' : ''}`}
                        onClick={(e) => handleToggleWishlist(product.id, e)}
                        aria-label="Add to wishlist"
                      >
                        <svg width="17" height="17" viewBox="0 0 24 24" fill={isWishlisted ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                        </svg>
                      </button>
                    </div>

                    <div className="home-product-card__body">
                      <h3 className="home-product-card__title" title={product.title}>
                        {product.title}
                      </h3>
                      <div className="home-product-card__price">
                        {formatPrice(product.price)}
                      </div>
                      <div className="home-product-card__location">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                          <circle cx="12" cy="10" r="3" />
                        </svg>
                        <span>{product.collegeName || product.cityName || 'Campus'}</span>
                      </div>

                      <div className="home-product-card__footer">
                        <div className="home-product-card__seller">
                          <div className="home-product-card__seller-avatar">
                            {(product.sellerName || 'S').substring(0, 1).toUpperCase()}
                          </div>
                          <span className="home-product-card__seller-name">
                            {product.sellerName || 'Seller'}
                          </span>
                        </div>
                        <div className="home-product-card__rating">
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="#eab308" stroke="#eab308" strokeWidth="1">
                            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                          </svg>
                          <span>5.0</span>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="home-empty-state">
              <div className="home-empty-state__icon-box">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
                  <line x1="8" y1="21" x2="16" y2="21" />
                  <line x1="12" y1="17" x2="12" y2="21" />
                </svg>
              </div>
              <h3 className="home-empty-state__title">No listings yet</h3>
              <p className="home-empty-state__desc">
                Be the first to sell something on CampusCart and reach students across your campus.
              </p>
              <button
                type="button"
                className="home-empty-state__btn"
                onClick={() => setSellModalOpen(true)}
              >
                Sell an Item
              </button>
            </div>
          )}
        </section>

        {/* ─── Recent Listings ─── */}
        <section className="home-section">
          <div className="home-section__header">
            <h2 className="home-section__title">Recent Listings</h2>
            <button
              type="button"
              className="home-section__link"
              onClick={() => fetchFilteredProducts(selectedCategory, searchQuery)}
            >
              View all
            </button>
          </div>

          {productsLoading ? (
            <div className="home-products-grid">
              {Array.from({ length: 4 }).map((_, idx) => (
                <div key={idx} className="home-skeleton" style={{ height: '280px' }} />
              ))}
            </div>
          ) : recentProducts.length > 0 ? (
            <div className="home-products-grid">
              {recentProducts.map((product) => {
                const isWishlisted = wishlistIds.has(product.id);
                const hasImage = product.images && product.images.length > 0;
                return (
                  <div key={product.id} className="home-product-card">
                    <div className="home-product-card__image-container">
                      {hasImage ? (
                        <img
                          src={product.images[0].imageUrl || product.images[0].url}
                          alt={product.title}
                          className="home-product-card__image"
                        />
                      ) : (
                        <div className="home-product-card__placeholder-icon">
                          {getCategoryIcon(product.categorySlug, product.categoryName)}
                        </div>
                      )}
                      <span className="home-product-card__badge-new">NEW</span>
                      <button
                        type="button"
                        className={`home-product-card__wishlist-btn ${isWishlisted ? 'home-product-card__wishlist-btn--active' : ''}`}
                        onClick={(e) => handleToggleWishlist(product.id, e)}
                        aria-label="Add to wishlist"
                      >
                        <svg width="17" height="17" viewBox="0 0 24 24" fill={isWishlisted ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                        </svg>
                      </button>
                    </div>

                    <div className="home-product-card__body">
                      <h3 className="home-product-card__title" title={product.title}>
                        {product.title}
                      </h3>
                      <div className="home-product-card__price">
                        {formatPrice(product.price)}
                      </div>
                      <div className="home-product-card__location">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                          <circle cx="12" cy="10" r="3" />
                        </svg>
                        <span>{product.collegeName || product.cityName || 'Campus'}</span>
                      </div>

                      <div className="home-product-card__footer">
                        <div className="home-product-card__seller">
                          <div className="home-product-card__seller-avatar">
                            {(product.sellerName || 'S').substring(0, 1).toUpperCase()}
                          </div>
                          <span className="home-product-card__seller-name">
                            {product.sellerName || 'Seller'}
                          </span>
                        </div>
                        <div className="home-product-card__rating">
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="#eab308" stroke="#eab308" strokeWidth="1">
                            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                          </svg>
                          <span>5.0</span>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="home-empty-state">
              <h3 className="home-empty-state__title">No recent listings</h3>
              <p className="home-empty-state__desc">
                Check back soon for fresh deals from students near you.
              </p>
            </div>
          )}
        </section>
      </div>

      {/* Sell Item Modal */}
      <SellModal
        isOpen={sellModalOpen}
        onClose={() => setSellModalOpen(false)}
        categories={categories}
        onProductCreated={() => {
          fetchFilteredProducts(selectedCategory, searchQuery);
        }}
      />
    </DashboardLayout>
  );
}
