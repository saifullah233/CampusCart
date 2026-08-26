import { useState, useRef, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import './SellModal.css';

const MAX_IMAGES = 5;
const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

export default function SellModal({ isOpen, onClose, categories, onProductCreated }) {
  const { user } = useAuth();
  const isCommunity = user?.accountType === 'COMMUNITY' || !user?.collegeId;
  const fileInputRef = useRef(null);

  const [form, setForm] = useState({
    title: '',
    categoryId: categories && categories.length > 0 ? categories[0].id : '',
    price: '',
    description: '',
    productType: 'NEW',
    sellingReach: isCommunity ? 'OUTSIDE_CAMPUS' : 'CAMPUS_ONLY',
    quantity: 1,
  });

  const [selectedImages, setSelectedImages] = useState([]); // [{ id, file, previewUrl }]
  const [imageError, setImageError] = useState('');
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Clean up object URLs when modal unmounts or closes
  useEffect(() => {
    return () => {
      selectedImages.forEach((img) => {
        if (img.previewUrl) {
          URL.revokeObjectURL(img.previewUrl);
        }
      });
    };
  }, [selectedImages]);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
    setApiError('');
  };

  const handleFileChange = (e) => {
    const files = Array.from(e.target.files || []);
    if (!files.length) return;

    setImageError('');
    setApiError('');

    if (selectedImages.length + files.length > MAX_IMAGES) {
      setImageError(`You can add up to ${MAX_IMAGES} photos only.`);
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    const newImages = [];
    for (const file of files) {
      if (!ALLOWED_TYPES.includes(file.type)) {
        setImageError(`"${file.name}" is not supported. Only JPG, PNG, and WEBP are allowed.`);
        if (fileInputRef.current) fileInputRef.current.value = '';
        return;
      }
      if (file.size > MAX_FILE_SIZE_BYTES) {
        setImageError(`"${file.name}" exceeds the 5 MB maximum size limit.`);
        if (fileInputRef.current) fileInputRef.current.value = '';
        return;
      }
      newImages.push({
        id: `${file.name}-${Date.now()}-${Math.random()}`,
        file,
        previewUrl: URL.createObjectURL(file),
      });
    }

    setSelectedImages((prev) => [...prev, ...newImages]);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleRemoveImage = (indexToRemove) => {
    setSelectedImages((prev) => {
      const removed = prev[indexToRemove];
      if (removed?.previewUrl) {
        URL.revokeObjectURL(removed.previewUrl);
      }
      return prev.filter((_, idx) => idx !== indexToRemove);
    });
    setImageError('');
  };

  const handleClose = () => {
    selectedImages.forEach((img) => {
      if (img.previewUrl) URL.revokeObjectURL(img.previewUrl);
    });
    setSelectedImages([]);
    setImageError('');
    setErrors({});
    setApiError('');
    onClose();
  };

  const validate = () => {
    const errs = {};
    if (!form.title.trim()) {
      errs.title = 'Title is required.';
    }
    if (!form.categoryId) {
      errs.categoryId = 'Category is required.';
    }
    if (!form.price || isNaN(Number(form.price)) || Number(form.price) <= 0) {
      errs.price = 'Enter a valid price greater than 0.';
    }
    if (!form.description.trim()) {
      errs.description = 'Description is required.';
    }
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;

    setSubmitting(true);
    setApiError('');

    try {
      const formData = new FormData();
      formData.append('title', form.title.trim());
      formData.append('categoryId', form.categoryId);
      formData.append('description', form.description.trim());
      formData.append('price', Number(form.price));
      formData.append('productType', form.productType);
      formData.append('sellingReach', isCommunity ? 'OUTSIDE_CAMPUS' : form.sellingReach);
      formData.append('quantity', Number(form.quantity) || 1);

      selectedImages.forEach((item) => {
        formData.append('images', item.file);
      });

      const res = await api.post('/api/v1/products', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      if (res.success && res.data) {
        selectedImages.forEach((img) => {
          if (img.previewUrl) URL.revokeObjectURL(img.previewUrl);
        });
        setSelectedImages([]);
        if (onProductCreated) {
          onProductCreated(res.data);
        }
        onClose();
      }
    } catch (err) {
      const msg = err?.message || err?.error?.detail || 'Failed to create listing. Please try again.';
      setApiError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="sell-modal-overlay" onClick={handleClose}>
      <div className="sell-modal" onClick={(e) => e.stopPropagation()}>
        <div className="sell-modal__header">
          <h2 className="sell-modal__title">{isCommunity ? 'Sell an Item in Marketplace' : 'Sell an Item on Campus'}</h2>
          <button
            type="button"
            className="sell-modal__close-btn"
            onClick={handleClose}
            aria-label="Close modal"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="sell-modal__body">
            {apiError && <div className="sell-modal__alert-error">{apiError}</div>}

            {/* Title */}
            <div className="sell-modal__field">
              <label className="sell-modal__label" htmlFor="sell-title">Listing Title</label>
              <input
                id="sell-title"
                name="title"
                type="text"
                className="sell-modal__input"
                placeholder="e.g. Discrete Mathematics Textbook, Lab Coat, Calculator"
                value={form.title}
                onChange={handleChange}
              />
              {errors.title && <div className="sell-modal__error">{errors.title}</div>}
            </div>

            {/* Category and Price */}
            <div className="sell-modal__row">
              <div className="sell-modal__field">
                <label className="sell-modal__label" htmlFor="sell-category">Category</label>
                <select
                  id="sell-category"
                  name="categoryId"
                  className="sell-modal__select"
                  value={form.categoryId}
                  onChange={handleChange}
                >
                  <option value="">Select a category</option>
                  {categories.map((cat) => (
                    <option key={cat.id} value={cat.id}>
                      {cat.name}
                    </option>
                  ))}
                </select>
                {errors.categoryId && <div className="sell-modal__error">{errors.categoryId}</div>}
              </div>

              <div className="sell-modal__field">
                <label className="sell-modal__label" htmlFor="sell-price">Price (₹)</label>
                <input
                  id="sell-price"
                  name="price"
                  type="number"
                  step="10"
                  min="0"
                  className="sell-modal__input"
                  placeholder="e.g. 99"
                  value={form.price}
                  onChange={handleChange}
                />
                {errors.price && <div className="sell-modal__error">{errors.price}</div>}
              </div>
            </div>

            {/* Product Type & Reach */}
            <div className="sell-modal__row">
              <div className="sell-modal__field">
                <label className="sell-modal__label" htmlFor="sell-type">Product Type</label>
                <select
                  id="sell-type"
                  name="productType"
                  className="sell-modal__select"
                  value={form.productType}
                  onChange={handleChange}
                >
                  <option value="NEW">Brand New / Unused</option>
                  <option value="SECOND_HAND">Second Hand / Used</option>
                </select>
              </div>

              <div className="sell-modal__field">
                <label className="sell-modal__label" htmlFor="sell-reach">Selling Reach</label>
                <select
                  id="sell-reach"
                  name="sellingReach"
                  className="sell-modal__select"
                  value={form.sellingReach}
                  onChange={handleChange}
                  disabled={isCommunity}
                >
                  {!isCommunity && <option value="CAMPUS_ONLY">My Campus Only</option>}
                  <option value="OUTSIDE_CAMPUS">Outside Campus</option>
                </select>
              </div>
            </div>

            {/* Description */}
            <div className="sell-modal__field">
              <label className="sell-modal__label" htmlFor="sell-desc">Description</label>
              <textarea
                id="sell-desc"
                name="description"
                className="sell-modal__textarea"
                placeholder="Provide details about condition, pickup location on campus, edition, etc."
                value={form.description}
                onChange={handleChange}
              />
              {errors.description && <div className="sell-modal__error">{errors.description}</div>}
            </div>

            {/* Product Images Section */}
            <div className="sell-modal__field">
              <div className="sell-modal__images-header">
                <div>
                  <label className="sell-modal__label" htmlFor="sell-images-input">Product Images</label>
                  <p className="sell-modal__images-hint">
                    You can add up to 5 photos &bull; First photo will be the cover image
                  </p>
                </div>
                <span className="sell-modal__images-count">
                  {selectedImages.length}/{MAX_IMAGES}
                </span>
              </div>

              <input
                ref={fileInputRef}
                id="sell-images-input"
                type="file"
                accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
                multiple
                style={{ display: 'none' }}
                onChange={handleFileChange}
              />

              <div className="sell-modal__images-grid">
                {selectedImages.map((img, idx) => (
                  <div key={img.id} className="sell-modal__preview-card">
                    <img
                      src={img.previewUrl}
                      alt={`Product preview ${idx + 1}`}
                      className="sell-modal__preview-img"
                    />
                    {idx === 0 && (
                      <span className="sell-modal__cover-badge">
                        Cover
                      </span>
                    )}
                    <button
                      type="button"
                      className="sell-modal__remove-img-btn"
                      onClick={() => handleRemoveImage(idx)}
                      title="Remove image"
                      aria-label={`Remove photo ${idx + 1}`}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                        <line x1="18" y1="6" x2="6" y2="18" />
                        <line x1="6" y1="6" x2="18" y2="18" />
                      </svg>
                    </button>
                  </div>
                ))}

                {selectedImages.length < MAX_IMAGES && (
                  <button
                    type="button"
                    className="sell-modal__add-img-btn"
                    onClick={() => fileInputRef.current?.click()}
                    aria-label="Add photos"
                  >
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="12" y1="5" x2="12" y2="19" />
                      <line x1="5" y1="12" x2="19" y2="12" />
                    </svg>
                    <span>Add Photos</span>
                  </button>
                )}
              </div>

              {imageError && <div className="sell-modal__error">{imageError}</div>}
            </div>
          </div>

          <div className="sell-modal__footer">
            <button
              type="button"
              className="sell-modal__btn-cancel"
              onClick={handleClose}
              disabled={submitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="sell-modal__btn-submit"
              disabled={submitting}
            >
              {submitting ? 'Creating...' : 'Post Listing'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
