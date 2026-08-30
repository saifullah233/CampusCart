import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { PublicRoute } from './PublicRoute';
import AccountType from '../pages/auth/AccountType';
import LoginSelection from '../pages/auth/LoginSelection';
import CommunityRegister from '../pages/auth/CommunityRegister';
import StudentRegister from '../pages/auth/StudentRegister';
import VerifyOtp from '../pages/auth/VerifyOtp';
import Login from '../pages/auth/Login';
import ForgotPassword from '../pages/auth/ForgotPassword';
import VerifyPasswordResetOtp from '../pages/auth/VerifyPasswordResetOtp';
import ResetPassword from '../pages/auth/ResetPassword';
import Home from '../pages/marketplace/Home';
import Marketplace from '../pages/marketplace/Marketplace';
import ProductDetails from '../pages/marketplace/ProductDetails';
import MyListings from '../pages/marketplace/MyListings';
import Cart from '../pages/commerce/Cart';
import Orders from '../pages/commerce/Orders';
import OrderDetails from '../pages/commerce/OrderDetails';
import Chat from '../pages/chat/Chat';

// Minimal Placeholder Components for Routing Validation
const Placeholder = ({ name }) => (
  <div style={{ padding: '20px', fontFamily: 'sans-serif' }}>
    <h1>{name} Page</h1>
    <p>This is a technical foundation route placeholder. Under construction.</p>
  </div>
);

export const AppRoutes = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/" element={<AccountType />} />
      <Route
        path="/login"
        element={
          <PublicRoute>
            <LoginSelection />
          </PublicRoute>
        }
      />
      <Route
        path="/login/student"
        element={
          <PublicRoute>
            <Login accountType="STUDENT" />
          </PublicRoute>
        }
      />
      <Route
        path="/login/community"
        element={
          <PublicRoute>
            <Login accountType="COMMUNITY" />
          </PublicRoute>
        }
      />
      <Route
        path="/forgot-password"
        element={
          <PublicRoute>
            <ForgotPassword />
          </PublicRoute>
        }
      />
      <Route
        path="/forgot-password/verify-otp"
        element={
          <PublicRoute>
            <VerifyPasswordResetOtp />
          </PublicRoute>
        }
      />
      <Route
        path="/forgot-password/reset"
        element={
          <PublicRoute>
            <ResetPassword />
          </PublicRoute>
        }
      />
      <Route
        path="/register/student"
        element={
          <PublicRoute>
            <StudentRegister />
          </PublicRoute>
        }
      />
      <Route
        path="/register/student/verify-otp"
        element={
          <PublicRoute>
            <VerifyOtp />
          </PublicRoute>
        }
      />
      <Route
        path="/register/community"
        element={
          <PublicRoute>
            <CommunityRegister />
          </PublicRoute>
        }
      />
      <Route
        path="/register/community/verify-otp"
        element={
          <PublicRoute>
            <VerifyOtp />
          </PublicRoute>
        }
      />
      <Route
        path="/verify-otp"
        element={
          <PublicRoute>
            <VerifyOtp />
          </PublicRoute>
        }
      />
      <Route path="/unauthorized" element={<Placeholder name="Unauthorized Access" />} />
      <Route
        path="/home"
        element={
          <ProtectedRoute>
            <Home />
          </ProtectedRoute>
        }
      />
      <Route
        path="/products"
        element={
          <ProtectedRoute>
            <Home />
          </ProtectedRoute>
        }
      />
      <Route
        path="/browse"
        element={
          <ProtectedRoute>
            <Marketplace />
          </ProtectedRoute>
        }
      />
      <Route
        path="/marketplace"
        element={
          <ProtectedRoute>
            <Marketplace />
          </ProtectedRoute>
        }
      />
      <Route
        path="/products/:id"
        element={
          <ProtectedRoute>
            <ProductDetails />
          </ProtectedRoute>
        }
      />
      <Route
        path="/marketplace/product/:id"
        element={
          <ProtectedRoute>
            <ProductDetails />
          </ProtectedRoute>
        }
      />
      <Route
        path="/my-listings"
        element={
          <ProtectedRoute>
            <MyListings />
          </ProtectedRoute>
        }
      />
      <Route
        path="/marketplace/my-listings"
        element={
          <ProtectedRoute>
            <MyListings />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <ProtectedRoute>
            <Placeholder name="User Profile" />
          </ProtectedRoute>
        }
      />
      <Route
        path="/cart"
        element={
          <ProtectedRoute>
            <Cart />
          </ProtectedRoute>
        }
      />
      <Route
        path="/wishlist"
        element={
          <ProtectedRoute>
            <Placeholder name="Wishlist" />
          </ProtectedRoute>
        }
      />
      <Route
        path="/orders"
        element={
          <ProtectedRoute>
            <Orders />
          </ProtectedRoute>
        }
      />
      <Route
        path="/orders/:id"
        element={
          <ProtectedRoute>
            <OrderDetails />
          </ProtectedRoute>
        }
      />
      <Route
        path="/seller/orders"
        element={
          <ProtectedRoute>
            <Orders />
          </ProtectedRoute>
        }
      />
      <Route
        path="/chat"
        element={
          <ProtectedRoute>
            <Chat />
          </ProtectedRoute>
        }
      />

      {/* Admin Protected Routes */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute adminOnly>
            <Placeholder name="Admin Dashboard" />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/products"
        element={
          <ProtectedRoute adminOnly>
            <Placeholder name="Admin Product Moderation Queue" />
          </ProtectedRoute>
        }
      />

      {/* Fallback Catch-all */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};
