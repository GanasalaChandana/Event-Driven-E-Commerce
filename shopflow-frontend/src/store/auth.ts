import { create } from "zustand";
import { AuthResponse, User } from "@/lib/types";

interface AuthState {
  token: string | null;
  user: User | null;
  isAdmin: boolean;
  hydrated: boolean;
  setAuth: (auth: AuthResponse) => void;
  logout: () => void;
  init: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  user: null,
  isAdmin: false,
  hydrated: false,

  init: () => {
    if (typeof window === "undefined") return;
    const token = localStorage.getItem("token");
    const raw = localStorage.getItem("user");
    if (token && raw) {
      const user: User = JSON.parse(raw);
      set({ token, user, isAdmin: user.role === "ADMIN", hydrated: true });
    } else {
      set({ hydrated: true });
    }
  },

  setAuth: (auth: AuthResponse) => {
    // Handle both flat and nested response formats
    const user: User = {
      id: auth.userId || auth.user?.id || "",
      name: auth.name || auth.user?.name || "",
      email: auth.email || auth.user?.email || "",
      role: auth.role || auth.user?.role || "USER",
      createdAt: auth.user?.createdAt || "",
    };
    localStorage.setItem("token", auth.token);
    localStorage.setItem("user", JSON.stringify(user));
    set({ token: auth.token, user, isAdmin: user.role === "ADMIN" });
  },

  logout: () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    set({ token: null, user: null, isAdmin: false });
  },
}));
