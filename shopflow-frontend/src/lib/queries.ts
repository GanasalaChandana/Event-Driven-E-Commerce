import { api } from "./api";
import { AuthResponse, Category, InventoryItem, Order, Page, Product, User } from "./types";

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

export const placeOrder = (items: object[]) =>
  api.post<Order>("/api/v1/orders", { items }).then((r) => r.data);

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
