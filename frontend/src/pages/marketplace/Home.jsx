import { useState, useEffect, useCallback, useId } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import ProductGrid from '../../components/marketplace/ProductGrid';
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
  const navigate = useNavigate();

  const [categories, setCategories] = useState([]);
  const [categoriesLoading, setCategoriesLoading] = useState(true);

  const [popularProducts, setPopularProducts] = useState([]);
  const [recentProducts, setRecentProducts] = useState([]);
  const [productsLoading, setProductsLoading] = useState(true);

  const [wishlistIds, setWishlistIds] = useState(new Set());
  const [sellModalOpen, setSellModalOpen] = useState(false);

  const grad1Id = useId();
  const grad2Id = useId();

  // Load Initial Data
  const loadData = useCallback(async () => {
    try {
      setProductsLoading(true);
      const [catRes, prodRes, recentRes, wishRes] = await Promise.allSettled([
        api.get('/api/v1/categories'),
        api.get('/api/v1/products?page=0&size=8&sort=createdAt,desc'),
        api.get('/api/v1/products?page=0&size=4&sort=createdAt,desc'),
        api.get('/api/v1/wishlist?page=0&size=50'),
      ]);

      if (catRes.status === 'fulfilled' && catRes.value?.success && Array.isArray(catRes.value.data)) {
        setCategories(catRes.value.data);
      }
      if (prodRes.status === 'fulfilled' && prodRes.value?.success && prodRes.value.data?.content) {
        setPopularProducts(prodRes.value.data.content);
      }
      if (recentRes.status === 'fulfilled' && recentRes.value?.success && recentRes.value.data?.content) {
        setRecentProducts(recentRes.value.data.content);
      }
      if (wishRes.status === 'fulfilled' && wishRes.value?.success && wishRes.value.data?.content) {
        const ids = new Set(wishRes.value.data.content.map((w) => w.product?.id || w.productId).filter(Boolean));
        setWishlistIds(ids);
      }
    } catch {
      // Graceful fallback
    } finally {
      setCategoriesLoading(false);
      setProductsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Wishlist toggle handler
  const handleWishlistToggle = (productId, nextState) => {
    setWishlistIds((prev) => {
      const copy = new Set(prev);
      if (nextState) copy.add(productId);
      else copy.delete(productId);
      return copy;
    });
  };

  // Search from Navbar on Home navigates to /browse?keyword=...
  const handleNavbarSearch = (q) => {
    if (q && q.trim()) {
      navigate(`/browse?keyword=${encodeURIComponent(q.trim())}`);
    }
  };

  return (
    <DashboardLayout
      onSearch={handleNavbarSearch}
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
              Join thousands of students buying and selling trusted textbooks, electronics, and dorm essentials across campus.
            </p>
            <div className="home-hero__actions">
              <Link to="/browse" className="home-hero__btn-primary">
                Explore Marketplace
              </Link>
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
              <circle cx="310" cy="60" r="32" fill="#e0f2fe" opacity="0.5" />
              <circle cx="340" cy="50" r="40" fill="#e0f2fe" opacity="0.5" />

              {/* Ground Curve */}
              <path d="M0 210 Q 210 180, 420 210 L 420 240 L 0 240 Z" fill={`url(#${grad1Id})`} />

              {/* Campus Building Elements */}
              <rect x="50" y="90" width="80" height="110" rx="6" fill="#ffffff" stroke="#bfdbfe" strokeWidth="2" />
              <polygon points="50,90 90,60 130,90" fill={`url(#${grad2Id})`} />
              <circle cx="90" cy="78" r="7" fill="#ffffff" />
              <rect x="65" y="105" width="14" height="16" rx="2" fill="#dbeafe" />
              <rect x="95" y="105" width="14" height="16" rx="2" fill="#dbeafe" />
              <rect x="65" y="130" width="14" height="16" rx="2" fill="#dbeafe" />
              <rect x="95" y="130" width="14" height="16" rx="2" fill="#dbeafe" />
              <rect x="80" y="165" width="20" height="35" rx="3" fill="#2563eb" />

              {/* Central Marketplace Cart / Stall */}
              <g transform="translate(160, 80)">
                <path d="M10 50 L 110 50 L 100 110 L 20 110 Z" fill="#ffffff" stroke="#2563eb" strokeWidth="2.5" />
                <path d="M5 50 Q 60 20, 115 50" fill="none" stroke="#2563eb" strokeWidth="3" />
                <path d="M5 50 L 20 25 L 100 25 L 115 50" fill="#2563eb" opacity="0.9" />
                <path d="M20 25 L 35 50 M45 25 L 60 50 M75 25 L 90 50" stroke="#ffffff" strokeWidth="2" />
                <rect x="30" y="65" width="20" height="24" rx="3" fill="#f59e0b" />
                <rect x="60" y="60" width="28" height="20" rx="3" fill="#10b981" />
                <circle cx="35" cy="115" r="10" fill="#334155" />
                <circle cx="35" cy="115" r="4" fill="#ffffff" />
                <circle cx="85" cy="115" r="10" fill="#334155" />
                <circle cx="85" cy="115" r="4" fill="#ffffff" />
              </g>

              {/* Student Figure 1 */}
              <g transform="translate(295, 100)">
                <circle cx="16" cy="14" r="10" fill="#fbbf24" />
                <path d="M8 20 C 8 16, 24 16, 24 20" fill="#1e293b" />
                <rect x="8" y="26" width="16" height="32" rx="6" fill="#3b82f6" />
                <rect x="2" y="30" width="6" height="20" rx="3" fill="#fbbf24" />
                <rect x="24" y="30" width="6" height="18" rx="3" fill="#fbbf24" />
                <rect x="24" y="44" width="14" height="18" rx="3" fill="#f43f5e" />
                <line x1="12" y1="58" x2="10" y2="85" stroke="#1e293b" strokeWidth="4" strokeLinecap="round" />
                <line x1="20" y1="58" x2="22" y2="85" stroke="#1e293b" strokeWidth="4" strokeLinecap="round" />
              </g>

              {/* Floating Verified & Heart Badges */}
              <g transform="translate(30, 30)">
                <circle cx="16" cy="16" r="16" fill="#ffffff" filter="drop-shadow(0 4px 6px rgba(0,0,0,0.08))" />
                <path d="M11 16 L 14 19 L 21 12" stroke="#10b981" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
              </g>

              <g transform="translate(350, 20)">
                <circle cx="16" cy="16" r="16" fill="#ffffff" filter="drop-shadow(0 4px 6px rgba(0,0,0,0.08))" />
                <path d="M16 22 L 14.5 20.6 C 9.5 16 6 12.8 6 9 C 6 5.8 8.4 3.5 11.5 3.5 C 13.2 3.5 14.9 4.3 16 5.6 C 17.1 4.3 18.8 3.5 20.5 3.5 C 23.6 3.5 26 5.8 26 9 C 26 12.8 22.5 16 17.5 20.6 Z" fill="#ef4444" transform="scale(0.7) translate(3, 4)" />
              </g>
            </svg>
          </div>
        </section>

        {/* ─── Categories Section ─── */}
        <section className="home-section">
          <div className="home-section__header">
            <h2 className="home-section__title">Categories</h2>
            <Link to="/browse" className="home-section__link">
              View all
            </Link>
          </div>

          <div className="home-categories-grid">
            {categoriesLoading
              ? Array.from({ length: 6 }).map((_, idx) => (
                  <div key={idx} className="home-skeleton" style={{ height: '110px' }} />
                ))
              : categories.map((cat) => (
                  <button
                    key={cat.id}
                    type="button"
                    className="home-category-card"
                    onClick={() => navigate(`/browse?categoryId=${cat.id}`)}
                  >
                    <div className="home-category-card__icon-box">
                      {getCategoryIcon(cat.slug, cat.name)}
                    </div>
                    <span className="home-category-card__name">{cat.name}</span>
                  </button>
                ))}
          </div>
        </section>

        {/* ─── Popular Products ─── */}
        <section className="home-section" id="marketplace-listings">
          <div className="home-section__header">
            <h2 className="home-section__title">Featured Listings</h2>
            <Link to="/browse" className="home-section__link">
              View all
            </Link>
          </div>

          {productsLoading ? (
            <ProductGrid loading={true} skeletonCount={8} />
          ) : popularProducts.length > 0 ? (
            <ProductGrid
              products={popularProducts}
              wishlistIds={wishlistIds}
              onWishlistToggle={handleWishlistToggle}
            />
          ) : (
            <div className="home-empty-state">
              <div className="home-empty-state__icon-box">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
                  <line x1="8" y1="21" x2="16" y2="21" />
                  <line x1="12" y1="17" x2="12" y2="21" />
                </svg>
              </div>
              <h3 className="home-empty-state__title">No listings available yet</h3>
              <p className="home-empty-state__desc">
                Be the first to list a textbook, dorm item, or electronics to reach buyers across campus.
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
        {recentProducts.length > 0 && (
          <section className="home-section">
            <div className="home-section__header">
              <h2 className="home-section__title">Recent Listings</h2>
              <Link to="/browse" className="home-section__link">
                View all
              </Link>
            </div>

            <ProductGrid
              products={recentProducts}
              wishlistIds={wishlistIds}
              onWishlistToggle={handleWishlistToggle}
            />
          </section>
        )}
      </div>

      {/* Sell Item Modal */}
      <SellModal
        isOpen={sellModalOpen}
        onClose={() => setSellModalOpen(false)}
        categories={categories}
        onProductCreated={() => {
          loadData();
        }}
      />
    </DashboardLayout>
  );
}
