import { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import './SellModal.css';

export default function SellModal({ isOpen, onClose, categories, onProductCreated }) {
  const { user } = useAuth();
  const isCommunity = user?.accountType === 'COMMUNITY' || !user?.collegeId;

  const [form, setForm] = useState({
    title: '',
    categoryId: categories && categories.length > 0 ? categories[0].id : '',
    price: '',
    description: '',
    productType: 'PHYSICAL',
    sellingReach: isCommunity ? 'OUTSIDE_CAMPUS' : 'CAMPUS_ONLY',
    quantity: 1,
  });

  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
    setApiError('');
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
      const res = await api.post('/api/v1/products', {
        title: form.title.trim(),
        categoryId: form.categoryId,
        description: form.description.trim(),
        price: Number(form.price),
        productType: form.productType,
        sellingReach: isCommunity ? 'OUTSIDE_CAMPUS' : form.sellingReach,
        quantity: Number(form.quantity) || 1,
      });

      if (res.success && res.data) {
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
    <div className="sell-modal-overlay" onClick={onClose}>
      <div className="sell-modal" onClick={(e) => e.stopPropagation()}>
        <div className="sell-modal__header">
          <h2 className="sell-modal__title">{isCommunity ? 'Sell an Item in Marketplace' : 'Sell an Item on Campus'}</h2>
          <button
            type="button"
            className="sell-modal__close-btn"
            onClick={onClose}
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
                  <option value="PHYSICAL">Physical Item</option>
                  <option value="DIGITAL">Digital / Notes</option>
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
          </div>

          <div className="sell-modal__footer">
            <button
              type="button"
              className="sell-modal__btn-cancel"
              onClick={onClose}
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
