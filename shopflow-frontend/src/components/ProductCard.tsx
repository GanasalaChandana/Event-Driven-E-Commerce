"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { Product } from "@/lib/types";
import { getInventory } from "@/lib/queries";
import { useCartStore } from "@/store/cart";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ShoppingCart, Package } from "lucide-react";
import Link from "next/link";

export default function ProductCard({ product }: { product: Product }) {
  const addToCart = useCartStore((s) => s.add);
  const [qty, setQty] = useState(1);

  const { data: inventory } = useQuery({
    queryKey: ["inventory", product.id],
    queryFn: () => getInventory(product.id),
  });

  const available = inventory?.availableQuantity ?? 0;
  const inStock = available > 0;

  const handleAdd = () => {
    addToCart({ productId: product.id, name: product.name, price: product.price }, qty, available);
    toast.success(`${product.name} added to cart`);
    setQty(1);
  };

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
        <Button className="flex-1" disabled={!inStock} onClick={handleAdd} size="sm">
          <ShoppingCart className="h-4 w-4 mr-1" /> Add to cart
        </Button>
      </CardFooter>
    </Card>
  );
}
