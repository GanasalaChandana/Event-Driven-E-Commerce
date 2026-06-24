"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import AdminLayout from "@/components/AdminLayout";
import { getAllOrders, updateOrderStatus } from "@/lib/queries";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { Package } from "lucide-react";

const statusColor: Record<string, string> = {
  PENDING: "bg-yellow-100 text-yellow-700",
  CONFIRMED: "bg-green-100 text-green-700",
  CANCELLED: "bg-red-100 text-red-700",
};

export default function AdminOrdersPage() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["admin-orders", statusFilter, page],
    queryFn: () => getAllOrders({ status: statusFilter === "ALL" ? undefined : statusFilter, page }),
  });

  const { mutate: updateStatus } = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) => updateOrderStatus(id, status),
    onSuccess: () => {
      toast.success("Order status updated");
      queryClient.invalidateQueries({ queryKey: ["admin-orders"] });
    },
    onError: () => toast.error("Failed to update order status"),
  });

  return (
    <AdminLayout>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold text-slate-900">Orders</h1>
        <Select value={statusFilter} onValueChange={(v) => { setStatusFilter(v); setPage(0); }}>
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Orders</SelectItem>
            <SelectItem value="PENDING">Pending</SelectItem>
            <SelectItem value="CONFIRMED">Confirmed</SelectItem>
            <SelectItem value="CANCELLED">Cancelled</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {isLoading ? (
        <div className="space-y-4">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-36 rounded-xl" />)}</div>
      ) : data?.content.length === 0 ? (
        <p className="text-slate-400 text-center py-20">No orders found</p>
      ) : (
        <div className="space-y-4">
          {data?.content.map((order) => (
            <Card key={order.id}>
              <CardHeader className="pb-2">
                <div className="flex items-start justify-between">
                  <div>
                    <CardTitle className="text-sm font-mono">#{order.id.slice(0, 8).toUpperCase()}</CardTitle>
                    <p className="text-xs text-slate-400 mt-1">{order.userEmail} · {new Date(order.createdAt).toLocaleDateString()}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={`text-xs px-2 py-1 rounded-full font-medium ${statusColor[order.status]}`}>
                      {order.status}
                    </span>
                    {order.status !== "CANCELLED" && (
                      <Select
                        value={order.status}
                        onValueChange={(status) => updateStatus({ id: order.id, status })}
                      >
                        <SelectTrigger className="h-8 w-36 text-xs">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="PENDING">Pending</SelectItem>
                          <SelectItem value="CONFIRMED">Confirmed</SelectItem>
                          <SelectItem value="CANCELLED">Cancelled</SelectItem>
                        </SelectContent>
                      </Select>
                    )}
                  </div>
                </div>
              </CardHeader>
              <Separator />
              <CardContent className="pt-3">
                <div className="space-y-1 mb-3">
                  {order.items.map((item, i) => (
                    <div key={i} className="flex items-center justify-between text-sm">
                      <div className="flex items-center gap-2 text-slate-600">
                        <Package className="h-3 w-3" />
                        {item.productName} × {item.quantity}
                      </div>
                      <span>${item.subtotal.toFixed(2)}</span>
                    </div>
                  ))}
                </div>
                <div className="flex justify-between font-semibold text-sm border-t pt-2">
                  <span>Total</span>
                  <span className="text-blue-600">${order.totalAmount.toFixed(2)}</span>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-6">
          <Button variant="outline" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
          <span className="flex items-center text-sm text-slate-500">Page {page + 1} of {data.totalPages}</span>
          <Button variant="outline" disabled={page >= data.totalPages - 1} onClick={() => setPage(page + 1)}>Next</Button>
        </div>
      )}
    </AdminLayout>
  );
}
