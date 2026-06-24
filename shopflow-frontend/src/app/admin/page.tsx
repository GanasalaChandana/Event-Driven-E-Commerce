"use client";

import { useQuery } from "@tanstack/react-query";
import AdminLayout from "@/components/AdminLayout";
import { getAllOrders, getAllUsers, getProducts } from "@/lib/queries";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Package, ShoppingCart, Users, TrendingUp } from "lucide-react";

export default function AdminDashboard() {
  const { data: orders } = useQuery({ queryKey: ["admin-orders"], queryFn: () => getAllOrders() });
  const { data: users } = useQuery({ queryKey: ["admin-users"], queryFn: () => getAllUsers() });
  const { data: products } = useQuery({ queryKey: ["products", "", null, 0], queryFn: () => getProducts() });

  const stats = [
    { label: "Total Products", value: products?.totalElements ?? "—", icon: Package, color: "text-blue-600" },
    { label: "Total Orders", value: orders?.totalElements ?? "—", icon: ShoppingCart, color: "text-green-600" },
    { label: "Total Users", value: users?.totalElements ?? "—", icon: Users, color: "text-purple-600" },
    {
      label: "Confirmed Orders",
      value: orders?.content.filter((o) => o.status === "CONFIRMED").length ?? "—",
      icon: TrendingUp,
      color: "text-orange-600",
    },
  ];

  return (
    <AdminLayout>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900">Dashboard</h1>
        <p className="text-slate-500 mt-1">Welcome back, Admin</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {stats.map(({ label, value, icon: Icon, color }) => (
          <Card key={label}>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-slate-500">{label}</CardTitle>
              <Icon className={`h-5 w-5 ${color}`} />
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-bold text-slate-900">{value}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Recent orders */}
      <Card>
        <CardHeader>
          <CardTitle>Recent Orders</CardTitle>
        </CardHeader>
        <CardContent>
          {orders?.content.slice(0, 5).map((order) => (
            <div key={order.id} className="flex items-center justify-between py-3 border-b last:border-0">
              <div>
                <p className="text-sm font-mono font-medium">#{order.id.slice(0, 8).toUpperCase()}</p>
                <p className="text-xs text-slate-400">{order.userEmail}</p>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold">${order.totalAmount.toFixed(2)}</p>
                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                  order.status === "CONFIRMED" ? "bg-green-100 text-green-700" :
                  order.status === "PENDING" ? "bg-yellow-100 text-yellow-700" :
                  "bg-red-100 text-red-700"
                }`}>{order.status}</span>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </AdminLayout>
  );
}
