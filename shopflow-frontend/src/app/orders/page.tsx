"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { getMyOrders, cancelOrder } from "@/lib/queries";
import { useAuthStore } from "@/store/auth";
import Navbar from "@/components/Navbar";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Package, X, Loader2, ShoppingBag } from "lucide-react";
import { Order } from "@/lib/types";
import { useEffect } from "react";
import Link from "next/link";

const statusColor: Record<string, string> = {
  PENDING: "bg-yellow-100 text-yellow-800",
  CONFIRMED: "bg-green-100 text-green-800",
  CANCELLED: "bg-red-100 text-red-800",
};

export default function OrdersPage() {
  const { user } = useAuthStore();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [cancellingId, setCancellingId] = useState<string | null>(null);

  useEffect(() => {
    if (!user) router.push("/login");
  }, [user, router]);

  const { data, isLoading } = useQuery({
    queryKey: ["orders", page],
    queryFn: () => getMyOrders(page),
    enabled: !!user,
  });

  const { mutate: cancel } = useMutation({
    mutationFn: (id: string) => cancelOrder(id),
    onSuccess: () => {
      toast.success("Order cancelled successfully");
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      setCancellingId(null);
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.detail || "Cannot cancel this order");
      setCancellingId(null);
    },
  });

  if (!user) return null;

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <Navbar />
      <main className="flex-1 max-w-4xl mx-auto w-full px-4 py-8">
        <h1 className="text-3xl font-bold text-slate-900 mb-6">My Orders</h1>

        {isLoading ? (
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-40 rounded-xl" />
            ))}
          </div>
        ) : data?.content.length === 0 ? (
          <div className="text-center py-20">
            <ShoppingBag className="h-16 w-16 text-slate-300 mx-auto mb-4" />
            <p className="text-lg text-slate-500">No orders yet</p>
            <Link href="/products">
              <Button className="mt-4">Browse Products</Button>
            </Link>
          </div>
        ) : (
          <div className="space-y-4">
            {data?.content.map((order: Order) => (
              <Card key={order.id} className="overflow-hidden">
                <CardHeader className="pb-3">
                  <div className="flex items-start justify-between">
                    <div>
                      <CardTitle className="text-sm font-mono text-slate-500">
                        #{order.id.slice(0, 8).toUpperCase()}
                      </CardTitle>
                      <p className="text-xs text-slate-400 mt-1">
                        {new Date(order.createdAt).toLocaleDateString("en-US", {
                          year: "numeric", month: "long", day: "numeric",
                          hour: "2-digit", minute: "2-digit",
                        })}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className={`text-xs font-semibold px-2 py-1 rounded-full ${statusColor[order.status]}`}>
                        {order.status}
                      </span>
                      {order.status === "PENDING" && (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-red-500 hover:text-red-700 hover:bg-red-50"
                          disabled={cancellingId === order.id}
                          onClick={() => {
                            setCancellingId(order.id);
                            cancel(order.id);
                          }}
                        >
                          {cancellingId === order.id ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                          ) : (
                            <><X className="h-4 w-4 mr-1" />Cancel</>
                          )}
                        </Button>
                      )}
                    </div>
                  </div>
                </CardHeader>
                <Separator />
                <CardContent className="pt-4">
                  <div className="space-y-2">
                    {order.items.map((item, i) => (
                      <div key={i} className="flex items-center justify-between text-sm">
                        <div className="flex items-center gap-2">
                          <Package className="h-4 w-4 text-slate-400" />
                          <span className="text-slate-700">{item.productName}</span>
                          <Badge variant="secondary" className="text-xs">x{item.quantity}</Badge>
                        </div>
                        <span className="font-medium">${item.subtotal.toFixed(2)}</span>
                      </div>
                    ))}
                  </div>
                  <Separator className="my-3" />
                  <div className="flex justify-between font-semibold">
                    <span>Total</span>
                    <span className="text-blue-600">${order.totalAmount.toFixed(2)}</span>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex justify-center gap-2 mt-8">
            <Button variant="outline" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Previous
            </Button>
            <span className="flex items-center text-sm text-slate-500">
              Page {page + 1} of {data.totalPages}
            </span>
            <Button variant="outline" disabled={page >= data.totalPages - 1} onClick={() => setPage(page + 1)}>
              Next
            </Button>
          </div>
        )}
      </main>
    </div>
  );
}
