import ProductCard from './ProductCard';
import './ProductGrid.css';

export default function ProductGrid({
  products = [],
  loading = false,
  skeletonCount = 8,
  wishlistIds = new Set(),
  onWishlistToggle,
  showSellerActions = false,
  onEdit,
  onDelete,
  onMarkSold,
  onToggleActive,
}) {
  if (loading) {
    return (
      <div className="cc-product-grid">
        {Array.from({ length: skeletonCount }).map((_, idx) => (
          <div key={idx} className="cc-product-card-skeleton">
            <div className="cc-skeleton-box cc-skeleton-box--img" />
            <div className="cc-skeleton-body">
              <div className="cc-skeleton-box cc-skeleton-box--badge" />
              <div className="cc-skeleton-box cc-skeleton-box--title" />
              <div className="cc-skeleton-box cc-skeleton-box--title-short" />
              <div className="cc-skeleton-box cc-skeleton-box--price" />
              <div className="cc-skeleton-box cc-skeleton-box--footer" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="cc-product-grid">
      {products.map((product) => (
        <ProductCard
          key={product.id}
          product={product}
          isWishlisted={wishlistIds.has(product.id)}
          onWishlistToggle={onWishlistToggle}
          showSellerActions={showSellerActions}
          onEdit={onEdit}
          onDelete={onDelete}
          onMarkSold={onMarkSold}
          onToggleActive={onToggleActive}
        />
      ))}
    </div>
  );
}
