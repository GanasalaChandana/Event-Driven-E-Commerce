"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import { Loader2, Minus, Plus, ShoppingBag, Trash2, CreditCard } from "lucide-react";
import Navbar from "@/components/Navbar";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { placeOrder, checkoutOrder } from "@/lib/queries";
import { useAuthStore } from "@/store/auth";
import { useCartStore } from "@/store/cart";
import { apiErrorMessage, redirectTo } from "@/lib/utils";

export default function CartPage() {
  const { user } = useAuthStore();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { items, setQuantity, remove, clear } = useCartStore();

  const total = items.reduce((sum, i) => sum + i.price * i.quantity, 0);
  const count = items.reduce((n, i) => n + i.quantity, 0);

  const { mutate: checkout, isPending } = useMutation({
    mutationFn: async () => {
      const order = await placeOrder(items.map((i) => ({ productId: i.productId, quantity: i.quantity })));
      // The order exists now, so empty the cart before anything else can fail — a retry must not duplicate it
      clear();
      try {
        const { url } = await checkoutOrder(order.id);
        return { order, url, payError: null as string | null };
      } catch (err) {
        return { order, url: null, payError: apiErrorMessage(err, "Could not start payment") };
      }
    },
    onSuccess: ({ order, url, payError }) => {
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      queryClient.invalidateQueries({ queryKey: ["inventory"] });
      if (url) {
        toast.success("Order created — redirecting to secure payment…");
        redirectTo(url);
        return;
      }
      if (payError) {
        toast.error(`${payError}. You can retry from My Orders.`);
        router.push("/orders");
        return;
      }
      toast.success("Order placed! Check your email for confirmation.");
      router.push(`/orders/confirmation?orderId=${order.id}&total=${order.totalAmount}`);
    },
    onError: (err: unknown) => {
      toast.error(apiErrorMessage(err, "Failed to place order"));
    },
  });

  const handleCheckout = () => {
    if (!user) {
      router.push("/login");
      return;
    }
    checkout();
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <Navbar />
      <main className="flex-1 max-w-4xl mx-auto w-full px-4 py-8">
        <h1 className="text-3xl font-bold text-slate-900 mb-6">Your Cart</h1>

        {items.length === 0 ? (
          <div className="text-center py-20">
            <ShoppingBag className="h-16 w-16 text-slate-300 mx-auto mb-4" />
            <p className="text-lg text-slate-500">Your cart is empty</p>
            <Link href="/products">
              <Button className="mt-4">Browse Products</Button>
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="md:col-span-2 space-y-3">
              {items.map((item) => (
                <Card key={item.productId}>
                  <CardContent className="pt-6 flex items-center justify-between gap-4">
                    <div className="min-w-0">
                      <Link
                        href={`/products/${item.productId}`}
                        className="font-semibold text-slate-900 hover:text-blue-600 truncate block"
                      >
                        {item.name}
                      </Link>
                      <p className="text-sm text-slate-500">${item.price.toFixed(2)} each</p>
                    </div>

                    <div className="flex items-center gap-4 shrink-0">
                      <div className="flex items-center border rounded-md">
                        <button
                          className="px-2 py-1 text-slate-600 hover:bg-slate-100 rounded-l-md"
                          aria-label={`Decrease ${item.name}`}
                          onClick={() => setQuantity(item.productId, item.quantity - 1)}
                        >
                          <Minus className="h-3 w-3" />
                        </button>
                        <span className="px-3 text-sm font-medium">{item.quantity}</span>
                        <button
                          className="px-2 py-1 text-slate-600 hover:bg-slate-100 rounded-r-md"
                          aria-label={`Increase ${item.name}`}
                          onClick={() => setQuantity(item.productId, item.quantity + 1)}
                        >
                          <Plus className="h-3 w-3" />
                        </button>
                      </div>
                      <span className="w-20 text-right font-semibold">
                        ${(item.price * item.quantity).toFixed(2)}
                      </span>
                      <button
                        className="text-slate-400 hover:text-red-500"
                        aria-label={`Remove ${item.name}`}
                        onClick={() => remove(item.productId)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>

            <Card className="h-fit">
              <CardContent className="pt-6 space-y-3">
                <h2 className="font-semibold text-slate-800">Order Summary</h2>
                <div className="flex justify-between text-sm text-slate-500">
                  <span>Items</span>
                  <span>{count}</span>
                </div>
                <Separator />
                <div className="flex justify-between font-semibold">
                  <span>Total</span>
                  <span className="text-blue-600">${total.toFixed(2)}</span>
                </div>
                <Button className="w-full" size="lg" disabled={isPending} onClick={handleCheckout}>
                  {isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <>
                      <CreditCard className="h-4 w-4 mr-2" />
                      {user ? "Checkout" : "Login to Checkout"}
                    </>
                  )}
                </Button>
                <p className="text-xs text-slate-400 text-center">
                  Final prices are confirmed by the server at checkout.
                </p>
              </CardContent>
            </Card>
          </div>
        )}
      </main>
    </div>
  );
}
