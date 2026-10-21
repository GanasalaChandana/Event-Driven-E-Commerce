"use client";

import { Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { CheckCircle, Loader2, XCircle, ArrowRight, ShoppingBag } from "lucide-react";
import Navbar from "@/components/Navbar";
import { Button } from "@/components/ui/button";
import { getOrder } from "@/lib/queries";

/**
 * Stripe sends the customer here after paying. The order is confirmed by Stripe's webhook, which
 * usually lands within a few seconds, so poll until the order leaves PENDING.
 */
function SuccessContent() {
  const orderId = useSearchParams().get("order");

  const { data: order, isError } = useQuery({
    queryKey: ["order", orderId],
    queryFn: () => getOrder(orderId as string),
    enabled: !!orderId,
    refetchInterval: (query) => (query.state.data?.status === "PENDING" ? 2000 : false),
  });

  const status = order?.status;
  const stillWaiting = !isError && (!order || status === "PENDING");

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <Navbar />
      <main className="flex-1 flex items-center justify-center px-4 py-16">
        <div className="max-w-md w-full text-center">
          <div className="flex justify-center mb-6">
            {status === "CONFIRMED" ? (
              <div className="h-24 w-24 bg-green-100 rounded-full flex items-center justify-center">
                <CheckCircle className="h-14 w-14 text-green-500" />
              </div>
            ) : status === "CANCELLED" ? (
              <div className="h-24 w-24 bg-red-100 rounded-full flex items-center justify-center">
                <XCircle className="h-14 w-14 text-red-500" />
              </div>
            ) : (
              <div className="h-24 w-24 bg-blue-100 rounded-full flex items-center justify-center">
                <Loader2 className="h-12 w-12 text-blue-500 animate-spin" />
              </div>
            )}
          </div>

          {status === "CONFIRMED" && (
            <>
              <h1 className="text-3xl font-bold text-slate-900 mb-3">Payment received</h1>
              <p className="text-slate-500 mb-8">
                Your order #{order?.id.slice(0, 8).toUpperCase()} is confirmed. Thank you!
              </p>
            </>
          )}
          {status === "CANCELLED" && (
            <>
              <h1 className="text-3xl font-bold text-slate-900 mb-3">Order cancelled</h1>
              <p className="text-slate-500 mb-8">
                This order could not be completed, so any payment will be refunded automatically.
              </p>
            </>
          )}
          {stillWaiting && (
            <>
              <h1 className="text-3xl font-bold text-slate-900 mb-3">Confirming your payment…</h1>
              <p className="text-slate-500 mb-8">This usually takes a few seconds. You can safely leave this page.</p>
            </>
          )}
          {isError && (
            <>
              <h1 className="text-3xl font-bold text-slate-900 mb-3">Payment submitted</h1>
              <p className="text-slate-500 mb-8">
                We could not load the order status. Check My Orders in a moment.
              </p>
            </>
          )}

          <div className="flex flex-col gap-3">
            <Link href="/orders">
              <Button className="w-full" size="lg">
                View My Orders <ArrowRight className="h-4 w-4 ml-2" />
              </Button>
            </Link>
            <Link href="/products">
              <Button variant="outline" className="w-full" size="lg">
                <ShoppingBag className="h-4 w-4 mr-2" /> Continue Shopping
              </Button>
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}

export default function OrderSuccessPage() {
  return (
    <Suspense>
      <SuccessContent />
    </Suspense>
  );
}
