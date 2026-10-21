import axios from "axios";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "https://event-driven-e-commerce.onrender.com";

export const api = axios.create({
  baseURL: API_URL,
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    const token = localStorage.getItem("token");
    if (token) config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    // A 401 from login/register just means wrong credentials — let the form show its own error
    const isAuthCall = String(err.config?.url ?? "").includes("/api/v1/auth/");
    if (err.response?.status === 401 && !isAuthCall && typeof window !== "undefined") {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.href = "/login";
    }
    return Promise.reject(err);
  }
);
