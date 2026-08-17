import { useState, useRef, useEffect } from 'react';
import './SearchableSelect.css';

export default function SearchableSelect({
  id,
  name,
  value,
  onChange,
  options = [],
  placeholder = 'Select an option',
  searchPlaceholder = 'Type to search...',
  disabled = false,
  loading = false,
  hasError = false,
  icon = null,
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const containerRef = useRef(null);
  const searchInputRef = useRef(null);

  // Find selected option object
  const selectedOption = options.find((opt) => opt.id === value) || null;

  // Filter options based on query (case-insensitive)
  const filteredOptions = options.filter((opt) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase().trim();
    const nameMatch = opt.name?.toLowerCase().includes(q);
    const subMatch = opt.sublabel?.toLowerCase().includes(q);
    return nameMatch || subMatch;
  });

  // Close on outside click
  useEffect(() => {
    const handleOutsideClick = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setIsOpen(false);
        setSearchQuery('');
      }
    };
    document.addEventListener('mousedown', handleOutsideClick);
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick);
    };
  }, []);

  // Auto-focus search input when opening
  useEffect(() => {
    if (isOpen && searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, [isOpen]);

  const handleToggle = () => {
    if (disabled || loading) return;
    setIsOpen((prev) => {
      const next = !prev;
      if (!next) setSearchQuery('');
      return next;
    });
  };

  const handleSelect = (opt) => {
    onChange({
      target: {
        name,
        value: opt.id,
        label: opt.name,
      },
    });
    setIsOpen(false);
    setSearchQuery('');
  };

  const handleClear = (e) => {
    e.stopPropagation();
    onChange({
      target: {
        name,
        value: '',
        label: '',
      },
    });
    setSearchQuery('');
  };

  const handleKeyDown = (e) => {
    if (disabled || loading) return;
    if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') {
      if (!isOpen) {
        e.preventDefault();
        setIsOpen(true);
      }
    } else if (e.key === 'Escape') {
      setIsOpen(false);
      setSearchQuery('');
    }
  };

  return (
    <div
      ref={containerRef}
      className={`ss-container${disabled ? ' ss-container--disabled' : ''}${isOpen ? ' ss-container--open' : ''}`}
    >
      <div
        id={id}
        tabIndex={disabled ? -1 : 0}
        role="combobox"
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        aria-disabled={disabled}
        className={`ss-control${hasError ? ' ss-control--error' : ''}${isOpen ? ' ss-control--focused' : ''}`}
        onClick={handleToggle}
        onKeyDown={handleKeyDown}
      >
        {icon && <div className="ss-icon">{icon}</div>}

        <div className="ss-value-wrap">
          {selectedOption ? (
            <span className="ss-selected-label">
              {selectedOption.name}
              {selectedOption.sublabel ? `, ${selectedOption.sublabel}` : ''}
            </span>
          ) : (
            <span className="ss-placeholder">
              {loading ? 'Loading...' : placeholder}
            </span>
          )}
        </div>

        <div className="ss-actions">
          {selectedOption && !disabled && !loading && (
            <button
              type="button"
              className="ss-clear-btn"
              onClick={handleClear}
              title="Clear selection"
              aria-label="Clear selection"
            >
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          )}

          <div className={`ss-chevron${isOpen ? ' ss-chevron--open' : ''}`}>
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </div>
        </div>
      </div>

      {/* Dropdown Popover */}
      {isOpen && (
        <div className="ss-dropdown" role="listbox">
          <div className="ss-search-wrap">
            <svg className="ss-search-icon" viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="#94a3b8" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              ref={searchInputRef}
              type="text"
              className="ss-search-input"
              placeholder={searchPlaceholder}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onClick={(e) => e.stopPropagation()}
              onKeyDown={(e) => {
                if (e.key === 'Escape') {
                  setIsOpen(false);
                  setSearchQuery('');
                }
              }}
            />
            {searchQuery && (
              <button
                type="button"
                className="ss-search-clear"
                onClick={(e) => {
                  e.stopPropagation();
                  setSearchQuery('');
                  if (searchInputRef.current) searchInputRef.current.focus();
                }}
              >
                ✕
              </button>
            )}
          </div>

          <div className="ss-options-list">
            {filteredOptions.length > 0 ? (
              filteredOptions.map((opt) => {
                const isSelected = opt.id === value;
                return (
                  <div
                    key={opt.id}
                    role="option"
                    aria-selected={isSelected}
                    className={`ss-option${isSelected ? ' ss-option--selected' : ''}`}
                    onClick={() => handleSelect(opt)}
                  >
                    <div className="ss-option__text">
                      <span className="ss-option__name">{opt.name}</span>
                      {opt.sublabel && (
                        <span className="ss-option__sublabel">{opt.sublabel}</span>
                      )}
                    </div>
                    {isSelected && (
                      <svg className="ss-option__check" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#2563eb" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                        <polyline points="20 6 9 17 4 12" />
                      </svg>
                    )}
                  </div>
                );
              })
            ) : (
              <div className="ss-no-results">
                {loading
                  ? 'Loading options...'
                  : searchQuery
                  ? `No matching results found for "${searchQuery}"`
                  : 'No options available'}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
