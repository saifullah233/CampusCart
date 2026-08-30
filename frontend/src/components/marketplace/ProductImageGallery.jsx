import { useState, useEffect } from 'react';
import './ProductImageGallery.css';

export default function ProductImageGallery({ images = [], title = 'Product' }) {
  const [selectedIndex, setSelectedIndex] = useState(0);

  // Reset selected index if images change
  useEffect(() => {
    setSelectedIndex(0);
  }, [images]);

  if (!images || images.length === 0) {
    return (
      <div className="cc-gallery-empty">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <polyline points="21 15 16 10 5 21" />
        </svg>
        <span>No product photos available</span>
      </div>
    );
  }

  const currentImage = images[selectedIndex] || images[0];
  const currentUrl = currentImage?.imageUrl || currentImage?.url;

  return (
    <div className="cc-gallery">
      {/* Main Active Image Box */}
      <div className="cc-gallery__main-box">
        <img
          src={currentUrl}
          alt={`${title} - Photo ${selectedIndex + 1}`}
          className="cc-gallery__main-img"
        />
        {currentImage.isCover && (
          <span className="cc-gallery__cover-badge">Cover Photo</span>
        )}
      </div>

      {/* Thumbnails Row */}
      {images.length > 1 && (
        <div className="cc-gallery__thumbs">
          {images.map((img, idx) => {
            const url = img.imageUrl || img.url;
            const isSelected = selectedIndex === idx;
            return (
              <button
                key={img.id || idx}
                type="button"
                className={`cc-gallery__thumb-btn ${isSelected ? 'cc-gallery__thumb-btn--active' : ''}`}
                onClick={() => setSelectedIndex(idx)}
                aria-label={`View photo ${idx + 1}`}
              >
                <img src={url} alt="" className="cc-gallery__thumb-img" />
                {img.isCover && <span className="cc-gallery__thumb-cover-dot" title="Cover photo" />}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
