import { useState } from 'react';
import './MarketplaceFilters.css';

export default function MarketplaceFilters({
  categories = [],
  selectedCategory,
  onSelectCategory,
  productType,
  onProductTypeChange,
  sellingReach,
  onSellingReachChange,
  minPrice,
  maxPrice,
  onPriceChange,
  sort,
  onSortChange,
  onClearFilters,
  activeFilterCount = 0,
}) {
  const [localMin, setLocalMin] = useState(minPrice || '');
  const [localMax, setLocalMax] = useState(maxPrice || '');
  const [mobileExpanded, setMobileExpanded] = useState(false);

  const handlePriceApply = (e) => {
    e.preventDefault();
    onPriceChange({
      minPrice: localMin !== '' ? localMin : undefined,
      maxPrice: localMax !== '' ? localMax : undefined,
    });
  };

  const handleClear = () => {
    setLocalMin('');
    setLocalMax('');
    onClearFilters();
  };

  return (
    <div className="cc-filters-container">
      {/* Mobile filter toggle header */}
      <div className="cc-filters-mobile-header">
        <button
          type="button"
          className="cc-filters-toggle-btn"
          onClick={() => setMobileExpanded(!mobileExpanded)}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="4" y1="21" x2="4" y2="14" />
            <line x1="4" y1="10" x2="4" y2="3" />
            <line x1="12" y1="21" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12" y2="3" />
            <line x1="20" y1="21" x2="20" y2="16" />
            <line x1="20" y1="12" x2="20" y2="3" />
            <line x1="1" y1="14" x2="7" y2="14" />
            <line x1="9" y1="8" x2="15" y2="8" />
            <line x1="17" y1="16" x2="23" y2="16" />
          </svg>
          <span>Filters {activeFilterCount > 0 && `(${activeFilterCount})`}</span>
          <svg
            className={`cc-filters-arrow ${mobileExpanded ? 'cc-filters-arrow--open' : ''}`}
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </button>

        {/* Sort Select */}
        <div className="cc-sort-wrapper">
          <label htmlFor="sort-select" className="cc-sort-label">Sort:</label>
          <select
            id="sort-select"
            className="cc-sort-select"
            value={sort || 'createdAt,desc'}
            onChange={(e) => onSortChange(e.target.value)}
          >
            <option value="createdAt,desc">Newest First</option>
            <option value="price,asc">Price: Low to High</option>
            <option value="price,desc">Price: High to Low</option>
            <option value="title,asc">Name: A to Z</option>
          </select>
        </div>
      </div>

      {/* Main Filter Body */}
      <div className={`cc-filters-body ${mobileExpanded ? 'cc-filters-body--expanded' : ''}`}>
        {/* Category Horizontal Scroll Chips */}
        <div className="cc-filter-section cc-filter-section--categories">
          <div className="cc-category-chips">
            <button
              type="button"
              className={`cc-category-chip ${!selectedCategory ? 'cc-category-chip--active' : ''}`}
              onClick={() => onSelectCategory(null)}
            >
              All Categories
            </button>
            {categories.map((cat) => (
              <button
                key={cat.id}
                type="button"
                className={`cc-category-chip ${selectedCategory === cat.id ? 'cc-category-chip--active' : ''}`}
                onClick={() => onSelectCategory(cat.id)}
              >
                {cat.name}
              </button>
            ))}
          </div>
        </div>

        {/* Filter Controls Row */}
        <div className="cc-filters-row">
          {/* Product Condition / Type */}
          <div className="cc-filter-group">
            <span className="cc-filter-label">Condition:</span>
            <div className="cc-pill-group">
              <button
                type="button"
                className={`cc-pill ${!productType ? 'cc-pill--active' : ''}`}
                onClick={() => onProductTypeChange(null)}
              >
                All
              </button>
              <button
                type="button"
                className={`cc-pill ${productType === 'NEW' ? 'cc-pill--active' : ''}`}
                onClick={() => onProductTypeChange('NEW')}
              >
                New
              </button>
              <button
                type="button"
                className={`cc-pill ${productType === 'SECOND_HAND' ? 'cc-pill--active' : ''}`}
                onClick={() => onProductTypeChange('SECOND_HAND')}
              >
                Second Hand
              </button>
            </div>
          </div>

          {/* Selling Reach */}
          <div className="cc-filter-group">
            <span className="cc-filter-label">Reach:</span>
            <div className="cc-pill-group">
              <button
                type="button"
                className={`cc-pill ${!sellingReach ? 'cc-pill--active' : ''}`}
                onClick={() => onSellingReachChange(null)}
              >
                All
              </button>
              <button
                type="button"
                className={`cc-pill ${sellingReach === 'CAMPUS_ONLY' ? 'cc-pill--active' : ''}`}
                onClick={() => onSellingReachChange('CAMPUS_ONLY')}
              >
                Campus Only
              </button>
              <button
                type="button"
                className={`cc-pill ${sellingReach === 'OUTSIDE_CAMPUS' ? 'cc-pill--active' : ''}`}
                onClick={() => onSellingReachChange('OUTSIDE_CAMPUS')}
              >
                Public
              </button>
            </div>
          </div>

          {/* Price Range Form */}
          <form className="cc-price-filter-form" onSubmit={handlePriceApply}>
            <span className="cc-filter-label">Price (₹):</span>
            <div className="cc-price-inputs">
              <input
                type="number"
                min="0"
                placeholder="Min"
                className="cc-price-input"
                value={localMin}
                onChange={(e) => setLocalMin(e.target.value)}
              />
              <span className="cc-price-divider">-</span>
              <input
                type="number"
                min="0"
                placeholder="Max"
                className="cc-price-input"
                value={localMax}
                onChange={(e) => setLocalMax(e.target.value)}
              />
              <button type="submit" className="cc-price-apply-btn" title="Apply price range">
                Go
              </button>
            </div>
          </form>

          {/* Clear Filters Button */}
          {activeFilterCount > 0 && (
            <button
              type="button"
              className="cc-clear-filters-btn"
              onClick={handleClear}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
              Reset Filters
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
