import { create } from "zustand";

export interface CartItem {
  productId: string;
  name: string;
  /** For display only — the server always re-prices from the catalogue at checkout. */
  price: number;
  quantity: number;
}

const STORAGE_KEY = "cart";
const MAX_QUANTITY = 1000;

function save(items: CartItem[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  } catch {
    // storage unavailable (private mode, quota) — the cart just won't persist
  }
}

function isCartItem(value: unknown): value is CartItem {
  const v = value as Partial<CartItem> | null;
  return (
    !!v &&
    typeof v.productId === "string" &&
    typeof v.name === "string" &&
    typeof v.price === "number" &&
    typeof v.quantity === "number" &&
    Number.isInteger(v.quantity) &&
    v.quantity > 0
  );
}

interface CartState {
  items: CartItem[];
  init: () => void;
  /** Adds to the cart; max caps the resulting quantity (e.g. the stock on hand). */
  add: (item: Omit<CartItem, "quantity">, quantity: number, max?: number) => void;
  setQuantity: (productId: string, quantity: number) => void;
  remove: (productId: string) => void;
  clear: () => void;
}

// The initial state is empty on the server and the client alike; init() loads the saved cart after mount
export const useCartStore = create<CartState>((set, get) => ({
  items: [],

  init: () => {
    if (typeof window === "undefined") return;
    try {
      const parsed: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? "[]");
      if (Array.isArray(parsed)) set({ items: parsed.filter(isCartItem) });
    } catch {
      set({ items: [] });
    }
  },

  add: (item, quantity, max) => {
    const cap = Math.min(MAX_QUANTITY, max ?? MAX_QUANTITY);
    const items = get().items;
    const existing = items.find((i) => i.productId === item.productId);
    const next = existing
      ? items.map((i) =>
          i.productId === item.productId ? { ...i, quantity: Math.min(cap, i.quantity + quantity) } : i
        )
      : [...items, { ...item, quantity: Math.min(cap, quantity) }];
    save(next);
    set({ items: next });
  },

  setQuantity: (productId, quantity) => {
    const q = Math.min(MAX_QUANTITY, Math.floor(quantity));
    const next =
      q < 1
        ? get().items.filter((i) => i.productId !== productId)
        : get().items.map((i) => (i.productId === productId ? { ...i, quantity: q } : i));
    save(next);
    set({ items: next });
  },

  remove: (productId) => {
    const next = get().items.filter((i) => i.productId !== productId);
    save(next);
    set({ items: next });
  },

  clear: () => {
    save([]);
    set({ items: [] });
  },
}));
