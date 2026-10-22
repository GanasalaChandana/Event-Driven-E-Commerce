"use client";

import { useQuery } from "@tanstack/react-query";
import AdminLayout from "@/components/AdminLayout";
import { getEveryOrder, getAllUsers, getProducts } from "@/lib/queries";
import SalesChart from "@/components/SalesChart";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Package, ShoppingCart, Users, TrendingUp } from "lucide-react";

export default function AdminDashboard() {
  const { data: orders } = useQuery({ queryKey: ["admin-orders", "all"], queryFn: getEveryOrder });
  const { data: users } = useQuery({ queryKey: ["admin-users"], queryFn: () => getAllUsers() });
  const { data: products } = useQuery({ queryKey: ["products", "", null, 0], queryFn: () => getProducts() });

  const stats = [
    { label: "Total Products", value: products?.totalElements ?? "—", icon: Package, color: "text-blue-600" },
    { label: "Total Orders", value: orders?.length ?? "—", icon: ShoppingCart, color: "text-green-600" },
    { label: "Total Users", value: users?.totalElements ?? "—", icon: Users, color: "text-purple-600" },
    {
      label: "Confirmed Orders",
      value: orders?.filter((o) => o.status === "CONFIRMED").length ?? "—",
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

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Revenue — last 14 days</CardTitle>
            <p className="text-xs text-slate-400">Confirmed orders only. Hover a bar for details.</p>
          </CardHeader>
          <CardContent>{orders ? <SalesChart orders={orders} /> : <p className="text-sm text-slate-400">Loading…</p>}</CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Orders by status</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {(["CONFIRMED", "PENDING", "CANCELLED"] as const).map((status) => {
              const count = orders?.filter((o) => o.status === status).length ?? 0;
              const pct = orders?.length ? (count / orders.length) * 100 : 0;
              const bar = status === "CONFIRMED" ? "bg-green-500" : status === "PENDING" ? "bg-yellow-400" : "bg-red-400";
              return (
                <div key={status}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-slate-600">{status[0] + status.slice(1).toLowerCase()}</span>
                    <span className="font-medium">{count}</span>
                  </div>
                  <div className="h-2 rounded-full bg-slate-100">
                    <div className={`h-2 rounded-full ${bar}`} style={{ width: `${pct}%` }} />
                  </div>
                </div>
              );
            })}
            <div className="flex justify-between border-t pt-3 text-sm">
              <span className="text-slate-500">Revenue (all time)</span>
              <span className="font-semibold text-blue-600">
                ${(orders?.filter((o) => o.status === "CONFIRMED").reduce((s, o) => s + o.totalAmount, 0) ?? 0).toFixed(2)}
              </span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Recent orders */}
      <Card>
        <CardHeader>
          <CardTitle>Recent Orders</CardTitle>
        </CardHeader>
        <CardContent>
          {orders?.slice(-5).reverse().map((order) => (
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
