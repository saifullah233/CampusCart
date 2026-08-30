import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    try {
      const storedUser = localStorage.getItem('cc_user');
      const token = localStorage.getItem('cc_accessToken');
      if (storedUser && token) {
        return JSON.parse(storedUser);
      }
    } catch (e) {
      console.error('Error loading auth user', e);
    }
    return null;
  });

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const handleLogout = () => {
      localStorage.removeItem('cc_accessToken');
      localStorage.removeItem('cc_refreshToken');
      localStorage.removeItem('cc_user');
      setUser(null);
      setLoading(false);
    };

    window.addEventListener('auth-logout', handleLogout);
    return () => {
      window.removeEventListener('auth-logout', handleLogout);
    };
  }, []);

  const login = (userData, accessToken, refreshToken) => {
    localStorage.setItem('cc_user', JSON.stringify(userData));
    localStorage.setItem('cc_accessToken', accessToken);
    localStorage.setItem('cc_refreshToken', refreshToken);
    setUser(userData);
  };

  const logout = () => {
    localStorage.removeItem('cc_accessToken');
    localStorage.removeItem('cc_refreshToken');
    localStorage.removeItem('cc_user');
    setUser(null);
    setLoading(false);
  };

  const value = {
    user,
    loading,
    login,
    logout,
    isAuthenticated: !!user,
    isAdmin: user?.role === 'ADMIN',
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
