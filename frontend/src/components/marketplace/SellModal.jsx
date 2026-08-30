import { useState, useRef, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import './SellModal.css';

const MAX_IMAGES = 5;
const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

export default function SellModal({
  isOpen,
  onClose,
  categories = [],
  initialProduct = null,
  onProductCreated,
  onProductUpdated,
}) {
  const { user } = useAuth();
  const isCommunity = user?.accountType === 'COMMUNITY' || !user?.collegeId;
  const isEditing = Boolean(initialProduct);

  const fileInputRef = useRef(null);

  const [form, setForm] = useState({
    title: '',
    categoryId: '',
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

  // Initialize or populate form when modal opens or initialProduct changes
  useEffect(() => {
    if (isOpen) {
      if (initialProduct) {
        setForm({
          title: initialProduct.title || '',
          categoryId: initialProduct.categoryId || '',
          price: initialProduct.price ? String(initialProduct.price) : '',
          description: initialProduct.description || '',
          productType: initialProduct.productType || 'NEW',
          sellingReach: isCommunity ? 'OUTSIDE_CAMPUS' : (initialProduct.sellingReach || 'CAMPUS_ONLY'),
          quantity: initialProduct.quantity || 1,
        });
      } else {
        setForm({
          title: '',
          categoryId: categories.length > 0 ? categories[0].id : '',
          price: '',
          description: '',
          productType: 'NEW',
          sellingReach: isCommunity ? 'OUTSIDE_CAMPUS' : 'CAMPUS_ONLY',
          quantity: 1,
        });
      }
      setSelectedImages([]);
      setImageError('');
      setErrors({});
      setApiError('');
    }
  }, [isOpen, initialProduct, categories, isCommunity]);

  // Clean up object URLs on unmount
  useEffect(() => {
    return () => {
      selectedImages.forEach((img) => {
        if (img.previewUrl) URL.revokeObjectURL(img.previewUrl);
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
    } else if (form.title.trim().length < 3) {
      errs.title = 'Title must be at least 3 characters.';
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
    if (!form.productType) {
      errs.productType = 'Please select product condition.';
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
      if (isEditing) {
        // Edit flow
        const updatePayload = {
          title: form.title.trim(),
          categoryId: form.categoryId,
          description: form.description.trim(),
          price: Number(form.price),
          productType: form.productType,
          sellingReach: isCommunity ? 'OUTSIDE_CAMPUS' : form.sellingReach,
          quantity: Number(form.quantity) || 1,
        };

        const res = await api.patch(`/api/v1/products/${initialProduct.id}`, updatePayload);

        // Upload any newly selected images
        if (selectedImages.length > 0) {
          for (const item of selectedImages) {
            const imgFormData = new FormData();
            imgFormData.append('file', item.file);
            await api.post(`/api/v1/products/${initialProduct.id}/images`, imgFormData, {
              headers: { 'Content-Type': 'multipart/form-data' },
            });
          }
        }

        if (res.success && res.data) {
          if (onProductUpdated) onProductUpdated(res.data);
          handleClose();
        }
      } else {
        // Creation flow (Multipart POST)
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
          if (onProductCreated) onProductCreated(res.data);
          handleClose();
        }
      }
    } catch (err) {
      const msg = err?.message || err?.error?.detail || 'Failed to save listing. Please check your inputs.';
      setApiError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="sell-modal-overlay" onClick={handleClose}>
      <div className="sell-modal" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="sell-modal__header">
          <div className="sell-modal__title-group">
            <h2 className="sell-modal__title">
              {isEditing
                ? 'Edit Listing'
                : isCommunity
                ? 'Sell an Item in Marketplace'
                : 'Sell an Item on Campus'}
            </h2>
            <p className="sell-modal__subtitle">
              {isEditing
                ? 'Update your item details and pricing'
                : 'Fill in the information below to list your product for buyers.'}
            </p>
          </div>
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

        {/* Form Body */}
        <form onSubmit={handleSubmit} noValidate>
          <div className="sell-modal__body">
            {apiError && (
              <div className="sell-modal__alert-error">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <span>{apiError}</span>
              </div>
            )}

            {/* Title */}
            <div className="sell-modal__field">
              <label className="sell-modal__label" htmlFor="sell-title">
                Product Title <span className="sell-modal__req">*</span>
              </label>
              <input
                id="sell-title"
                name="title"
                type="text"
                className={`sell-modal__input ${errors.title ? 'sell-modal__input--error' : ''}`}
                placeholder="e.g. Engineering Mathematics Volume 1 (HK Dass)"
                value={form.title}
                onChange={handleChange}
                maxLength={120}
              />
              {errors.title && <div className="sell-modal__error-msg">{errors.title}</div>}
            </div>

            {/* Category & Price Row */}
            <div className="sell-modal__row">
              <div className="sell-modal__field">
                <label className="sell-modal__label" htmlFor="sell-category">
                  Category <span className="sell-modal__req">*</span>
                </label>
                <select
                  id="sell-category"
                  name="categoryId"
                  className={`sell-modal__select ${errors.categoryId ? 'sell-modal__input--error' : ''}`}
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
                {errors.categoryId && <div className="sell-modal__error-msg">{errors.categoryId}</div>}
              </div>

              <div className="sell-modal__field">
                <label className="sell-modal__label" htmlFor="sell-price">
                  Price (₹) <span className="sell-modal__req">*</span>
                </label>
                <div className="sell-modal__input-prefix-box">
                  <span className="sell-modal__prefix">₹</span>
                  <input
                    id="sell-price"
                    name="price"
                    type="number"
                    min="1"
                    step="any"
                    className={`sell-modal__input sell-modal__input--prefixed ${errors.price ? 'sell-modal__input--error' : ''}`}
                    placeholder="e.g. 450"
                    value={form.price}
                    onChange={handleChange}
                  />
                </div>
                {errors.price && <div className="sell-modal__error-msg">{errors.price}</div>}
              </div>
            </div>

            {/* Condition & Selling Reach Row */}
            <div className="sell-modal__row">
              <div className="sell-modal__field">
                <label className="sell-modal__label">
                  Condition <span className="sell-modal__req">*</span>
                </label>
                <div className="sell-modal__radio-group">
                  <label className={`sell-modal__radio-card ${form.productType === 'NEW' ? 'sell-modal__radio-card--active' : ''}`}>
                    <input
                      type="radio"
                      name="productType"
                      value="NEW"
                      checked={form.productType === 'NEW'}
                      onChange={handleChange}
                    />
                    <div className="sell-modal__radio-content">
                      <span className="sell-modal__radio-title">Brand New</span>
                      <span className="sell-modal__radio-desc">Unused / Original condition</span>
                    </div>
                  </label>

                  <label className={`sell-modal__radio-card ${form.productType === 'SECOND_HAND' ? 'sell-modal__radio-card--active' : ''}`}>
                    <input
                      type="radio"
                      name="productType"
                      value="SECOND_HAND"
                      checked={form.productType === 'SECOND_HAND'}
                      onChange={handleChange}
                    />
                    <div className="sell-modal__radio-content">
                      <span className="sell-modal__radio-title">Second Hand</span>
                      <span className="sell-modal__radio-desc">Pre-owned / Used item</span>
                    </div>
                  </label>
                </div>
              </div>

              <div className="sell-modal__field">
                <label className="sell-modal__label">
                  Selling Reach <span className="sell-modal__req">*</span>
                </label>
                {isCommunity ? (
                  <div className="sell-modal__reach-notice">
                    <span className="sell-modal__reach-badge">Public Marketplace</span>
                    <p className="sell-modal__reach-hint">
                      Community accounts list items visible across the public campus marketplace.
                    </p>
                  </div>
                ) : (
                  <div className="sell-modal__radio-group">
                    <label className={`sell-modal__radio-card ${form.sellingReach === 'CAMPUS_ONLY' ? 'sell-modal__radio-card--active' : ''}`}>
                      <input
                        type="radio"
                        name="sellingReach"
                        value="CAMPUS_ONLY"
                        checked={form.sellingReach === 'CAMPUS_ONLY'}
                        onChange={handleChange}
                      />
                      <div className="sell-modal__radio-content">
                        <span className="sell-modal__radio-title">My Campus Only</span>
                        <span className="sell-modal__radio-desc">Visible only to your college students</span>
                      </div>
                    </label>

                    <label className={`sell-modal__radio-card ${form.sellingReach === 'OUTSIDE_CAMPUS' ? 'sell-modal__radio-card--active' : ''}`}>
                      <input
                        type="radio"
                        name="sellingReach"
                        value="OUTSIDE_CAMPUS"
                        checked={form.sellingReach === 'OUTSIDE_CAMPUS'}
                        onChange={handleChange}
                      />
                      <div className="sell-modal__radio-content">
                        <span className="sell-modal__radio-title">Public Market</span>
                        <span className="sell-modal__radio-desc">Visible to nearby colleges & community</span>
                      </div>
                    </label>
                  </div>
                )}
              </div>
            </div>

            {/* Description */}
            <div className="sell-modal__field">
              <label className="sell-modal__label" htmlFor="sell-desc">
                Description <span className="sell-modal__req">*</span>
              </label>
              <textarea
                id="sell-desc"
                name="description"
                rows={3}
                className={`sell-modal__textarea ${errors.description ? 'sell-modal__input--error' : ''}`}
                placeholder="Mention condition, edition, reason for selling, pick-up location on campus..."
                value={form.description}
                onChange={handleChange}
              />
              {errors.description && <div className="sell-modal__error-msg">{errors.description}</div>}
            </div>

            {/* Photos Upload Section */}
            <div className="sell-modal__field">
              <div className="sell-modal__photos-header">
                <label className="sell-modal__label">
                  Photos {isEditing ? '(Add more photos)' : ''}
                </label>
                <span className="sell-modal__photos-count">
                  {selectedImages.length} / {MAX_IMAGES} photos
                </span>
              </div>

              {imageError && <div className="sell-modal__error-msg">{imageError}</div>}

              <div className="sell-modal__photos-grid">
                {/* Upload Trigger Tile */}
                {selectedImages.length < MAX_IMAGES && (
                  <button
                    type="button"
                    className="sell-modal__upload-tile"
                    onClick={() => fileInputRef.current?.click()}
                  >
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                      <polyline points="17 8 12 3 7 8" />
                      <line x1="12" y1="3" x2="12" y2="15" />
                    </svg>
                    <span>Add Photo</span>
                  </button>
                )}

                {/* Hidden File Input */}
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  multiple
                  style={{ display: 'none' }}
                  onChange={handleFileChange}
                />

                {/* Previews */}
                {selectedImages.map((img, idx) => (
                  <div key={img.id} className="sell-modal__preview-tile">
                    <img src={img.previewUrl} alt={`Upload ${idx + 1}`} className="sell-modal__preview-img" />
                    {idx === 0 && !isEditing && (
                      <span className="sell-modal__cover-tag">Cover</span>
                    )}
                    <button
                      type="button"
                      className="sell-modal__remove-photo-btn"
                      onClick={() => handleRemoveImage(idx)}
                      aria-label="Remove photo"
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                        <line x1="18" y1="6" x2="6" y2="18" />
                        <line x1="6" y1="6" x2="18" y2="18" />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
              <span className="sell-modal__photos-hint">
                Max 5 photos. JPG, PNG, WEBP up to 5MB each. First photo is used as the cover.
              </span>
            </div>
          </div>

          {/* Footer Actions */}
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
              {submitting ? (
                <span className="sell-modal__spinner-row">
                  <span className="sell-modal__spinner" />
                  {isEditing ? 'Saving Changes...' : 'Creating Listing...'}
                </span>
              ) : isEditing ? (
                'Save Changes'
              ) : (
                'Publish Listing'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
