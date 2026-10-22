export interface User {
  id: string;
  name: string;
  email: string;
  role: "USER" | "ADMIN";
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  // flat fields (new backend)
  userId?: string;
  email?: string;
  name?: string;
  role?: "USER" | "ADMIN";
  // nested user object (old backend format)
  user?: {
    id?: string;
    email?: string;
    name?: string;
    role?: "USER" | "ADMIN";
    createdAt?: string;
  };
}

export interface Category {
  id: string;
  name: string;
  description: string;
}

export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  sku: string;
  category: Category | null;
  imageUrl: string | null;
  active: boolean;
  createdAt: string;
}

export interface InventoryItem {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
}

export interface OrderItem {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface ShippingAddress {
  fullName: string;
  line1: string;
  line2?: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  phone?: string | null;
}

export interface Address extends ShippingAddress {
  id: string;
  defaultAddress: boolean;
}

export interface Order {
  id: string;
  userId: string;
  userEmail: string;
  status: "PENDING" | "CONFIRMED" | "CANCELLED";
  totalAmount: number;
  items: OrderItem[];
  shippingAddress?: ShippingAddress | null;
  createdAt: string;
  updatedAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
