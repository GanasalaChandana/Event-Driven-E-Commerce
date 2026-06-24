"use client";

import { useSearchParams } from "next/navigation";
import Link from "next/link";
import Navbar from "@/components/Navbar";
import { Button } from "@/components/ui/button";
import { CheckCircle, Package, ArrowRight, ShoppingBag } from "lucide-react";
import { Suspense } from "react";

function ConfirmationContent() {
  const params = useSearchParams();
  const orderId = params.get("orderId");
  const total = params.get("total");

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <Navbar />
      <main className="flex-1 flex items-center justify-center px-4 py-16">
        <div className="max-w-md w-full text-center">
          {/* Success icon */}
          <div className="flex justify-center mb-6">
            <div className="h-24 w-24 bg-green-100 rounded-full flex items-center justify-center">
              <CheckCircle className="h-14 w-14 text-green-500" />
            </div>
          </div>

          <h1 className="text-3xl font-bold text-slate-900 mb-3">Order Placed!</h1>
          <p className="text-slate-500 mb-8">
            Your order has been received and is being processed. You'll receive a confirmation email shortly.
          </p>

          {/* Order details box */}
          <div className="bg-white rounded-2xl border p-6 mb-8 text-left shadow-sm">
            <div className="flex items-center gap-2 mb-4">
              <Package className="h-5 w-5 text-blue-600" />
              <h2 className="font-semibold text-slate-800">Order Summary</h2>
            </div>

            <div className="space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-slate-500">Order ID</span>
                <span className="font-mono font-medium text-slate-800">
                  #{orderId?.slice(0, 8).toUpperCase()}
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500">Total Amount</span>
                <span className="font-semibold text-blue-600">${parseFloat(total || "0").toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500">Status</span>
                <span className="bg-yellow-100 text-yellow-700 text-xs font-semibold px-2 py-0.5 rounded-full">
                  PENDING → CONFIRMED
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500">Email</span>
                <span className="text-slate-600">Confirmation sent</span>
              </div>
            </div>
          </div>

          {/* Actions */}
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

export default function OrderConfirmationPage() {
  return (
    <Suspense>
      <ConfirmationContent />
    </Suspense>
  );
}
