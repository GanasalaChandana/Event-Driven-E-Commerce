import { api } from "./api";
import { Address, AuthResponse, ShippingAddress, Category, InventoryItem, Order, Page, Product, User } from "./types";

// Auth
export const login = (email: string, password: string) =>
  api.post<AuthResponse>("/api/v1/auth/login", { email, password }).then((r) => r.data);

export const register = (name: string, email: string, password: string) =>
  api.post<AuthResponse>("/api/v1/auth/register", { name, email, password }).then((r) => r.data);

// Products
export const getProducts = (params?: { q?: string; categoryId?: string; page?: number; size?: number }) =>
  api.get<Page<Product>>("/api/v1/products", { params }).then((r) => r.data);

export const getProduct = (id: string) =>
  api.get<Product>(`/api/v1/products/${id}`).then((r) => r.data);

export const createProduct = (data: object) =>
  api.post<Product>("/api/v1/products", data).then((r) => r.data);

export const updateProduct = (id: string, data: object) =>
  api.put<Product>(`/api/v1/products/${id}`, data).then((r) => r.data);

export const deleteProduct = (id: string) =>
  api.delete(`/api/v1/products/${id}`);

// Categories
export const getCategories = () =>
  api.get<Category[]>("/api/v1/categories").then((r) => r.data);

export const createCategory = (data: object) =>
  api.post<Category>("/api/v1/categories", data).then((r) => r.data);

export const deleteCategory = (id: string) =>
  api.delete(`/api/v1/categories/${id}`);

// Inventory
export const getInventory = (productId: string) =>
  api.get<InventoryItem>(`/api/v1/inventory/${productId}`).then((r) => r.data);

export const addStock = (data: object) =>
  api.post<InventoryItem>("/api/v1/inventory", data).then((r) => r.data);

export const adjustStock = (productId: string, quantity: number) =>
  api.put<InventoryItem>(`/api/v1/inventory/${productId}`, { quantity }).then((r) => r.data);

// Orders
export const getMyOrders = (page = 0) =>
  api.get<Page<Order>>("/api/v1/orders", { params: { page, size: 10 } }).then((r) => r.data);

// Prices and names are looked up by the server — only product IDs and quantities are sent
export const placeOrder = (items: { productId: string; quantity: number }[], shippingAddress: ShippingAddress) =>
  api.post<Order>("/api/v1/orders", { items, shippingAddress }).then((r) => r.data);

/** Every order of the current user (pages through the list) — used for the profile statistics. */
export const getAllMyOrders = async () => {
  const all: Order[] = [];
  for (let page = 0; page < 50; page++) {
    const res = await api
      .get<Page<Order>>("/api/v1/orders", { params: { page, size: 100 } })
      .then((r) => r.data);
    all.push(...res.content);
    if (page + 1 >= res.totalPages) break;
  }
  return all;
};

export const getOrder = (id: string) =>
  api.get<Order>(`/api/v1/orders/${id}`).then((r) => r.data);

/** Starts payment for a PENDING order. url is null when the server has payments disabled. */
export const checkoutOrder = (id: string) =>
  api.post<{ url: string | null }>(`/api/v1/orders/${id}/checkout`).then((r) => r.data);

export const cancelOrder = (id: string) =>
  api.post<Order>(`/api/v1/orders/${id}/cancel`).then((r) => r.data);

// Admin - Orders
export const getAllOrders = (params?: { status?: string; page?: number }) =>
  api.get<Page<Order>>("/api/v1/admin/orders", { params }).then((r) => r.data);

export const updateOrderStatus = (id: string, status: string) =>
  api.patch<Order>(`/api/v1/admin/orders/${id}/status`, { status }).then((r) => r.data);

// Admin - Users
export const getAllUsers = (page = 0) =>
  api.get<Page<User>>("/api/v1/admin/users", { params: { page, size: 20 } }).then((r) => r.data);

export const getMe = () =>
  api.get<User>("/api/v1/users/me").then((r) => r.data);

// Address book
export type AddressInput = ShippingAddress & { defaultAddress?: boolean };

export const getAddresses = () =>
  api.get<Address[]>("/api/v1/users/me/addresses").then((r) => r.data);

export const createAddress = (data: AddressInput) =>
  api.post<Address>("/api/v1/users/me/addresses", data).then((r) => r.data);

export const updateAddress = (id: string, data: AddressInput) =>
  api.put<Address>(`/api/v1/users/me/addresses/${id}`, data).then((r) => r.data);

export const deleteAddress = (id: string) =>
  api.delete(`/api/v1/users/me/addresses/${id}`);
