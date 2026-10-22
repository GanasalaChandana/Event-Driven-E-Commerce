"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { CreditCard, Loader2, Lock, MapPin, Plus } from "lucide-react";
import Navbar from "@/components/Navbar";
import AddressForm, { formatAddress } from "@/components/AddressForm";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { checkoutOrder, createAddress, getAddresses, placeOrder } from "@/lib/queries";
import type { ShippingAddress } from "@/lib/types";
import { useAuthStore } from "@/store/auth";
import { useCartStore } from "@/store/cart";
import { apiErrorMessage, redirectTo } from "@/lib/utils";

export default function CheckoutPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const { items, clear } = useCartStore();

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);

  const total = items.reduce((sum, i) => sum + i.price * i.quantity, 0);

  const { data: addresses, isLoading } = useQuery({
    queryKey: ["addresses"],
    queryFn: getAddresses,
    enabled: !!user,
  });

  // Derived (not effects): the chosen address, else the default; the form opens itself when there are none
  const selected =
    addresses?.find((a) => a.id === selectedId) ?? addresses?.find((a) => a.defaultAddress) ?? addresses?.[0];
  const showForm = adding || (!!addresses && addresses.length === 0);

  const { mutate: placeAndPay, isPending } = useMutation({
    mutationFn: async ({ address, save }: { address: ShippingAddress; save: boolean }) => {
      const order = await placeOrder(
        items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
        address,
      );
      // The order exists now, so empty the cart before anything else can fail — a retry must not duplicate it
      clear();
      if (save) {
        try {
          await createAddress(address);
        } catch {
          // not worth failing the order over
        }
      }
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
      queryClient.invalidateQueries({ queryKey: ["addresses"] });
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

  if (!user) {
    return (
      <div className="min-h-screen flex flex-col bg-slate-50">
        <Navbar />
        <main className="flex-1 max-w-3xl mx-auto w-full px-4 py-16 text-center">
          <p className="text-slate-600 mb-4">Please sign in to check out.</p>
          <Link href="/login"><Button>Sign in</Button></Link>
        </main>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="min-h-screen flex flex-col bg-slate-50">
        <Navbar />
        <main className="flex-1 max-w-3xl mx-auto w-full px-4 py-16 text-center">
          <p className="text-slate-600 mb-4">Your cart is empty.</p>
          <Link href="/products"><Button>Browse products</Button></Link>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <Navbar />
      <main className="flex-1 max-w-4xl mx-auto w-full px-4 py-8">
        <h1 className="text-3xl font-bold text-slate-900 mb-6">Checkout</h1>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="md:col-span-2 space-y-4">
            <h2 className="font-semibold text-slate-800 flex items-center gap-2">
              <MapPin className="h-4 w-4" /> 1. Delivery address
            </h2>

            {isLoading ? (
              <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
            ) : showForm ? (
              <Card>
                <CardContent className="pt-6">
                  <AddressForm
                    allowSave
                    pending={isPending}
                    submitLabel="Deliver here & pay"
                    onSubmit={(address, save) => placeAndPay({ address, save })}
                    onCancel={addresses && addresses.length > 0 ? () => setAdding(false) : undefined}
                  />
                </CardContent>
              </Card>
            ) : (
              <div className="space-y-3">
                {addresses?.map((a) => (
                  <label
                    key={a.id}
                    className={`flex gap-3 rounded-lg border bg-white p-4 cursor-pointer ${
                      a.id === selected?.id ? "border-blue-500 ring-1 ring-blue-500" : "border-slate-200"
                    }`}
                  >
                    <input
                      type="radio"
                      name="address"
                      checked={a.id === selected?.id}
                      onChange={() => setSelectedId(a.id)}
                    />
                    <span className="whitespace-pre-line text-sm text-slate-700">
                      {formatAddress(a)}
                      {a.defaultAddress && <span className="ml-2 text-xs text-blue-600">Default</span>}
                    </span>
                  </label>
                ))}
                <Button variant="outline" onClick={() => setAdding(true)}>
                  <Plus className="h-4 w-4 mr-1" /> Add a new address
                </Button>
              </div>
            )}

            <h2 className="font-semibold text-slate-800 flex items-center gap-2 pt-2">
              <CreditCard className="h-4 w-4" /> 2. Payment
            </h2>
            <p className="text-sm text-slate-500 flex items-start gap-2">
              <Lock className="h-4 w-4 mt-0.5 shrink-0" />
              You&apos;ll enter your card or bank details on Stripe&apos;s secure payment page. We never see or store them.
            </p>
          </div>

          <Card className="h-fit">
            <CardContent className="pt-6 space-y-3">
              <h2 className="font-semibold text-slate-800">Order summary</h2>
              {items.map((i) => (
                <div key={i.productId} className="flex justify-between text-sm text-slate-600 gap-2">
                  <span className="truncate">{i.name} × {i.quantity}</span>
                  <span>${(i.price * i.quantity).toFixed(2)}</span>
                </div>
              ))}
              <Separator />
              <div className="flex justify-between font-semibold">
                <span>Total</span>
                <span className="text-blue-600">${total.toFixed(2)}</span>
              </div>
              {!showForm && (
                <Button
                  className="w-full"
                  size="lg"
                  disabled={isPending || !selected}
                  onClick={() => {
                    if (!selected) return;
                    placeAndPay({
                      address: {
                        fullName: selected.fullName,
                        line1: selected.line1,
                        line2: selected.line2,
                        city: selected.city,
                        state: selected.state,
                        postalCode: selected.postalCode,
                        country: selected.country,
                        phone: selected.phone,
                      },
                      save: false,
                    });
                  }}
                >
                  {isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Place order & pay"}
                </Button>
              )}
              <p className="text-xs text-slate-400 text-center">Final prices are confirmed by the server.</p>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
}
