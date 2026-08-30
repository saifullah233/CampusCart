import './EmptyMarketplace.css';

export default function EmptyMarketplace({
  title = 'No products found',
  description = 'Try adjusting your search query, scope, or filters to find what you are looking for.',
  actionLabel = 'Reset Filters',
  onAction,
  secondaryActionLabel,
  onSecondaryAction,
}) {
  return (
    <div className="cc-empty-marketplace">
      <div className="cc-empty-marketplace__icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
          <line x1="8" y1="11" x2="14" y2="11" />
        </svg>
      </div>
      <h3 className="cc-empty-marketplace__title">{title}</h3>
      <p className="cc-empty-marketplace__description">{description}</p>
      <div className="cc-empty-marketplace__actions">
        {onAction && actionLabel && (
          <button
            type="button"
            className="cc-empty-marketplace__btn-primary"
            onClick={onAction}
          >
            {actionLabel}
          </button>
        )}
        {onSecondaryAction && secondaryActionLabel && (
          <button
            type="button"
            className="cc-empty-marketplace__btn-secondary"
            onClick={onSecondaryAction}
          >
            {secondaryActionLabel}
          </button>
        )}
      </div>
    </div>
  );
}
