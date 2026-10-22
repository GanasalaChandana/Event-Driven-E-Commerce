"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import Link from "next/link";
import { getProduct, getInventory } from "@/lib/queries";
import { useAuthStore } from "@/store/auth";
import Navbar from "@/components/Navbar";
import ProductImage from "@/components/ProductImage";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import {
  ShoppingCart, ArrowLeft, CheckCircle,
  AlertCircle, Tag
} from "lucide-react";
import { useCartStore } from "@/store/cart";

export default function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { user } = useAuthStore();
  const [qty, setQty] = useState(1);
  const addToCart = useCartStore((s) => s.add);

  const { data: product, isLoading: loadingProduct } = useQuery({
    queryKey: ["product", id],
    queryFn: () => getProduct(id),
  });

  const { data: inventory, isLoading: loadingInventory } = useQuery({
    queryKey: ["inventory", id],
    queryFn: () => getInventory(id),
    enabled: !!id,
  });

  const available = inventory?.availableQuantity ?? 0;
  const inStock = available > 0;

  if (loadingProduct) {
    return (
      <div className="min-h-screen flex flex-col bg-slate-50">
        <Navbar />
        <main className="max-w-4xl mx-auto w-full px-4 py-8">
          <Skeleton className="h-8 w-32 mb-6" />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <Skeleton className="h-80 rounded-xl" />
            <div className="space-y-4">
              <Skeleton className="h-8 w-3/4" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-2/3" />
              <Skeleton className="h-10 w-1/3" />
            </div>
          </div>
        </main>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="min-h-screen flex flex-col bg-slate-50">
        <Navbar />
        <main className="max-w-4xl mx-auto w-full px-4 py-20 text-center">
          <p className="text-slate-400 text-lg">Product not found</p>
          <Link href="/products"><Button className="mt-4">Back to Products</Button></Link>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <Navbar />
      <main className="max-w-4xl mx-auto w-full px-4 py-8">
        {/* Breadcrumb */}
        <Link href="/products" className="flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700 mb-6">
          <ArrowLeft className="h-4 w-4" /> Back to Products
        </Link>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {/* Left — Product image */}
          <div className="bg-white rounded-2xl border h-80 shadow-sm overflow-hidden">
            <ProductImage product={product} className="h-full w-full" iconSize="h-24 w-24" />
          </div>

          {/* Right — Product info */}
          <div className="flex flex-col">
            {product.category && (
              <div className="flex items-center gap-1 mb-3">
                <Tag className="h-3 w-3 text-slate-400" />
                <Badge variant="secondary">{product.category.name}</Badge>
              </div>
            )}

            <h1 className="text-3xl font-bold text-slate-900 mb-3">{product.name}</h1>

            {product.description && (
              <p className="text-slate-500 mb-4 leading-relaxed">{product.description}</p>
            )}

            <div className="text-4xl font-bold text-blue-600 mb-4">
              ${product.price.toFixed(2)}
            </div>

            <Separator className="mb-4" />

            {/* Stock status */}
            <div className="flex items-center gap-2 mb-6">
              {loadingInventory ? (
                <Skeleton className="h-5 w-32" />
              ) : inStock ? (
                <>
                  <CheckCircle className="h-5 w-5 text-green-500" />
                  <span className="text-green-600 font-medium">{available} in stock</span>
                </>
              ) : (
                <>
                  <AlertCircle className="h-5 w-5 text-red-500" />
                  <span className="text-red-500 font-medium">Out of stock</span>
                </>
              )}
            </div>

            {/* SKU */}
            <p className="text-xs text-slate-400 mb-6">SKU: {product.sku}</p>

            {/* Quantity + Order */}
            {inStock && (
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <span className="text-sm font-medium text-slate-700">Quantity:</span>
                  <div className="flex items-center border rounded-lg overflow-hidden">
                    <button
                      className="px-4 py-2 text-slate-600 hover:bg-slate-100 font-medium"
                      onClick={() => setQty(Math.max(1, qty - 1))}
                    >−</button>
                    <span className="px-4 py-2 font-semibold min-w-[3rem] text-center">{qty}</span>
                    <button
                      className="px-4 py-2 text-slate-600 hover:bg-slate-100 font-medium"
                      onClick={() => setQty(Math.min(available, qty + 1))}
                    >+</button>
                  </div>
                </div>

                <div className="flex items-center justify-between text-sm text-slate-500 bg-slate-50 rounded-lg p-3">
                  <span>Subtotal</span>
                  <span className="font-semibold text-slate-800">${(product.price * qty).toFixed(2)}</span>
                </div>

                <Button
                  size="lg"
                  variant="outline"
                  className="w-full"
                  onClick={() => {
                    addToCart({ productId: id, name: product.name, price: product.price }, qty, available);
                    toast.success(`${product.name} added to cart`);
                  }}
                >
                  <ShoppingCart className="h-5 w-5 mr-2" /> Add to cart
                </Button>

                <Button
                  size="lg"
                  className="w-full"
                  onClick={() => {
                    if (!user) { router.push("/login"); return; }
                    addToCart({ productId: id, name: product.name, price: product.price }, qty, available);
                    router.push("/checkout");
                  }}
                >
                  {user ? "Buy now" : "Login to order"}
                </Button>

                {!user && (
                  <p className="text-xs text-center text-slate-400">
                    <Link href="/login" className="text-blue-500 hover:underline">Sign in</Link> to place an order
                  </p>
                )}
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
