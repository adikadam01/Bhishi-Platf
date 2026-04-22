// src/context/AuthContext.js
// ============================================================
// DIGITAL BHISHI PLATFORM — Auth Context
// Phase 8 | Provides global user state + JWT token to all
// components. Links to Phase 2 backend auth endpoints.
// ============================================================
import React, { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user,    setUser]    = useState(null);
  const [token,   setToken]   = useState(localStorage.getItem('bhishi_token'));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = localStorage.getItem('bhishi_user');
    if (stored && token) {
      try { setUser(JSON.parse(stored)); }
      catch { logout(); }
    }
    setLoading(false);
  }, []);

  const login = (userData, jwt) => {
    setUser(userData);
    setToken(jwt);
    localStorage.setItem('bhishi_token', jwt);
    localStorage.setItem('bhishi_user',  JSON.stringify(userData));
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('bhishi_token');
    localStorage.removeItem('bhishi_user');
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
