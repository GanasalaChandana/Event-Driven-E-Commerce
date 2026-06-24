"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Product } from "@/lib/types";
import { placeOrder, getInventory } from "@/lib/queries";
import { useAuthStore } from "@/store/auth";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ShoppingCart, Package, Loader2 } from "lucide-react";
import { useRouter } from "next/navigation";
import Link from "next/link";

export default function ProductCard({ product }: { product: Product }) {
  const { user } = useAuthStore();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [qty, setQty] = useState(1);

  const { data: inventory } = useQuery({
    queryKey: ["inventory", product.id],
    queryFn: () => getInventory(product.id),
  });

  const { mutate: order, isPending } = useMutation({
    mutationFn: () =>
      placeOrder([{
        productId: product.id,
        productName: product.name,
        quantity: qty,
        unitPrice: product.price,
      }]),
    onSuccess: () => {
      toast.success("Order placed! Check your email for confirmation.");
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      queryClient.invalidateQueries({ queryKey: ["inventory", product.id] });
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.detail || "Failed to place order");
    },
  });

  const handleOrder = () => {
    if (!user) {
      router.push("/login");
      return;
    }
    order();
  };

  const available = inventory?.availableQuantity ?? 0;
  const inStock = available > 0;

  return (
    <Card className="flex flex-col hover:shadow-md transition-shadow">
      <CardContent className="flex-1 pt-6">
        {/* Category badge */}
        {product.category && (
          <Badge variant="secondary" className="mb-3 text-xs">
            {product.category.name}
          </Badge>
        )}

        <div className="flex items-start justify-between gap-2 mb-2">
          <Link href={`/products/${product.id}`} className="font-semibold text-slate-900 leading-tight hover:text-blue-600 transition-colors">
            {product.name}
          </Link>
        </div>

        {product.description && (
          <p className="text-sm text-slate-500 mb-3 line-clamp-2">{product.description}</p>
        )}

        <p className="text-2xl font-bold text-blue-600">${product.price.toFixed(2)}</p>

        <div className="flex items-center gap-1 mt-2">
          <Package className="h-3 w-3 text-slate-400" />
          <span className={`text-xs ${inStock ? "text-green-600" : "text-red-500"}`}>
            {inStock ? `${available} in stock` : "Out of stock"}
          </span>
        </div>
      </CardContent>

      <CardFooter className="pt-0 flex gap-2">
        {inStock && (
          <div className="flex items-center gap-1 border rounded-md">
            <button
              className="px-2 py-1 text-slate-600 hover:bg-slate-100 rounded-l-md"
              onClick={() => setQty(Math.max(1, qty - 1))}
            >−</button>
            <span className="px-2 text-sm font-medium">{qty}</span>
            <button
              className="px-2 py-1 text-slate-600 hover:bg-slate-100 rounded-r-md"
              onClick={() => setQty(Math.min(available, qty + 1))}
            >+</button>
          </div>
        )}
        <Button
          className="flex-1"
          disabled={!inStock || isPending}
          onClick={handleOrder}
          size="sm"
        >
          {isPending ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <><ShoppingCart className="h-4 w-4 mr-1" /> {user ? "Order" : "Login to Order"}</>
          )}
        </Button>
      </CardFooter>
    </Card>
  );
}
