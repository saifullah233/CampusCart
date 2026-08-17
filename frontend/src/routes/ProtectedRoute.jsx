import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const ProtectedRoute = ({ children, adminOnly = false }) => {
  const { user, loading, isAuthenticated, isAdmin } = useAuth();
  const location = useLocation();

  if (loading) {
    return <div className="loading-placeholder">Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (adminOnly && !isAdmin) {
    return <Navigate to="/unauthorized" replace />;
  }

  // Handle checking if user has verification pending
  if (user?.status === 'PENDING_VERIFICATION' && location.pathname !== '/verify-otp') {
    return <Navigate to="/verify-otp" replace />;
  }

  return children;
};
