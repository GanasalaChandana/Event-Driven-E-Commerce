import axios from "axios";
import { toast } from "sonner";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "https://event-driven-e-commerce.onrender.com";

export const api = axios.create({
  baseURL: API_URL,
  headers: { "Content-Type": "application/json" },
});

// The free hosting tier sleeps when idle and the first request can take ~a minute. If any request
// is still pending after a few seconds, tell the user why instead of leaving a silent spinner.
const WAKE_TOAST_ID = "server-waking";
const WAKE_DELAY_MS = 6000;
let inFlight = 0;
let wakeTimer: ReturnType<typeof setTimeout> | undefined;

function requestStarted() {
  inFlight++;
  if (inFlight === 1 && typeof window !== "undefined") {
    wakeTimer = setTimeout(() => {
      toast.loading("Waking up the server — free hosting can take up to a minute on the first request…", {
        id: WAKE_TOAST_ID,
        duration: Infinity,
      });
    }, WAKE_DELAY_MS);
  }
}

function requestFinished() {
  inFlight = Math.max(0, inFlight - 1);
  if (inFlight === 0) {
    clearTimeout(wakeTimer);
    toast.dismiss(WAKE_TOAST_ID);
  }
}

api.interceptors.request.use((config) => {
  requestStarted();
  if (typeof window !== "undefined") {
    const token = localStorage.getItem("token");
    if (token) config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => {
    requestFinished();
    return res;
  },
  (err) => {
    requestFinished();
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
