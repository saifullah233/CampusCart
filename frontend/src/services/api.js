import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Request Interceptor: Inject Access Token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('cc_accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Handle API envelope & Token Refresh
api.interceptors.response.use(
  (response) => {
    // Return the response data envelope directly for ease of use
    return response.data;
  },
  async (error) => {
    const originalRequest = error.config;

    // Standard Backend ApiError parsing
    const apiError = error.response?.data || {
      success: false,
      message: error.message,
      error: { code: 'NETWORK_ERROR', detail: error.message },
    };

    // If 401 and we are not already trying to refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = localStorage.getItem('cc_refreshToken');
      if (!refreshToken) {
        isRefreshing = false;
        window.dispatchEvent(new Event('auth-logout'));
        return Promise.reject(apiError);
      }

      try {
        const response = await axios.post(`${API_URL}/api/v1/auth/refresh`, {
          refreshToken,
        }, {
          headers: {
            'Content-Type': 'application/json',
          }
        });

        const { success, data } = response.data;
        if (success && data?.accessToken) {
          localStorage.setItem('cc_accessToken', data.accessToken);
          if (data.refreshToken) {
            localStorage.setItem('cc_refreshToken', data.refreshToken);
          }
          api.defaults.headers.common['Authorization'] = `Bearer ${data.accessToken}`;
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
          processQueue(null, data.accessToken);
          isRefreshing = false;
          return api(originalRequest);
        } else {
          throw new Error('Refresh failed');
        }
      } catch (refreshError) {
        processQueue(refreshError, null);
        isRefreshing = false;
        localStorage.removeItem('cc_accessToken');
        localStorage.removeItem('cc_refreshToken');
        localStorage.removeItem('cc_user');
        window.dispatchEvent(new Event('auth-logout'));
        return Promise.reject(apiError);
      }
    }

    return Promise.reject(apiError);
  }
);

export default api;
