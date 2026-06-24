"use client";

import { useState, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import AdminLayout from "@/components/AdminLayout";
import { getAllOrders, updateOrderStatus } from "@/lib/queries";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { Package, X } from "lucide-react";
import { Order } from "@/lib/types";

const statusColor: Record<string, string> = {
  PENDING: "bg-yellow-100 text-yellow-700",
  CONFIRMED: "bg-green-100 text-green-700",
  CANCELLED: "bg-red-100 text-red-700",
};

export default function AdminOrdersPage() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(0);
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");

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

  // Client-side date filter
  const filtered = useMemo(() => {
    if (!data?.content) return [];
    return data.content.filter((order: Order) => {
      const created = new Date(order.createdAt);
      if (fromDate && created < new Date(fromDate)) return false;
      if (toDate && created > new Date(toDate + "T23:59:59")) return false;
      return true;
    });
  }, [data, fromDate, toDate]);

  const clearFilters = () => {
    setFromDate("");
    setToDate("");
    setStatusFilter("ALL");
    setPage(0);
  };

  const hasFilters = fromDate || toDate || statusFilter !== "ALL";

  return (
    <AdminLayout>
      {/* Filters bar */}
      <div className="flex flex-wrap items-end gap-3 mb-6">
        <div className="flex-1 min-w-0">
          <h1 className="text-3xl font-bold text-slate-900">Orders</h1>
        </div>

        {/* Status filter */}
        <div className="space-y-1">
          <Label className="text-xs text-slate-500">Status</Label>
          <Select value={statusFilter} onValueChange={(v) => { setStatusFilter(v); setPage(0); }}>
            <SelectTrigger className="w-36">
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

        {/* Date range */}
        <div className="space-y-1">
          <Label className="text-xs text-slate-500">From</Label>
          <Input
            type="date"
            className="w-36"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
          />
        </div>
        <div className="space-y-1">
          <Label className="text-xs text-slate-500">To</Label>
          <Input
            type="date"
            className="w-36"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
          />
        </div>

        {hasFilters && (
          <Button variant="ghost" size="sm" onClick={clearFilters} className="text-slate-500">
            <X className="h-4 w-4 mr-1" /> Clear
          </Button>
        )}
      </div>

      {/* Results count */}
      {data && (
        <p className="text-sm text-slate-500 mb-4">
          Showing {filtered.length} of {data.totalElements} orders
        </p>
      )}

      {isLoading ? (
        <div className="space-y-4">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-36 rounded-xl" />)}</div>
      ) : filtered.length === 0 ? (
        <p className="text-slate-400 text-center py-20">No orders match your filters</p>
      ) : (
        <div className="space-y-4">
          {filtered.map((order) => (
            <Card key={order.id}>
              <CardHeader className="pb-2">
                <div className="flex items-start justify-between">
                  <div>
                    <CardTitle className="text-sm font-mono">#{order.id.slice(0, 8).toUpperCase()}</CardTitle>
                    <p className="text-xs text-slate-400 mt-1">
                      {order.userEmail} · {new Date(order.createdAt).toLocaleDateString("en-US", {
                        year: "numeric", month: "short", day: "numeric"
                      })}
                    </p>
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
