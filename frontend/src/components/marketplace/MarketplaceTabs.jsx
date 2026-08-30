import { useAuth } from '../../context/AuthContext';
import './MarketplaceTabs.css';

export default function MarketplaceTabs({ activeScope = 'ALL_PRODUCTS', onScopeChange }) {
  const { user } = useAuth();
  const isStudent = user?.accountType === 'STUDENT' && !!user?.collegeId;

  const scopes = [
    {
      id: 'ALL_PRODUCTS',
      label: 'All Products',
      icon: (
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="2" y1="12" x2="22" y2="12" />
          <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10z" />
        </svg>
      ),
    },
    ...(isStudent
      ? [
          {
            id: 'MY_COLLEGE',
            label: user?.collegeName ? `${user.collegeName}` : 'My College',
            shortLabel: 'My College',
            icon: (
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
                <path d="M6 12v5c0 2 2 3 6 3s6-1 6-3v-5" />
              </svg>
            ),
          },
        ]
      : []),
    {
      id: 'NEARBY_COLLEGES',
      label: user?.cityName ? `Nearby in ${user.cityName}` : 'Nearby Colleges',
      shortLabel: 'Nearby Colleges',
      icon: (
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
          <circle cx="12" cy="10" r="3" />
        </svg>
      ),
    },
    {
      id: 'COMMUNITY_MARKETPLACE',
      label: 'Community Market',
      icon: (
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
          <path d="M16 3.13a4 4 0 0 1 0 7.75" />
        </svg>
      ),
    },
  ];

  return (
    <div className="cc-scope-tabs-container">
      <div className="cc-scope-tabs" role="tablist" aria-label="Marketplace Scope">
        {scopes.map((scope) => {
          const isActive = activeScope === scope.id;
          return (
            <button
              key={scope.id}
              type="button"
              role="tab"
              aria-selected={isActive}
              className={`cc-scope-tab ${isActive ? 'cc-scope-tab--active' : ''}`}
              onClick={() => onScopeChange(scope.id)}
            >
              <span className="cc-scope-tab__icon">{scope.icon}</span>
              <span className="cc-scope-tab__label">{scope.label}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
