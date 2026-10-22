"use client";

import { useState } from "react";
import { BookOpen, Cpu, Home, Dumbbell, Shirt, Package, type LucideIcon } from "lucide-react";
import type { Product } from "@/lib/types";

const THEMES: Record<string, { icon: LucideIcon; gradient: string }> = {
  electronics: { icon: Cpu, gradient: "from-blue-100 to-indigo-200 text-blue-500" },
  books: { icon: BookOpen, gradient: "from-amber-100 to-orange-200 text-amber-600" },
  "home & kitchen": { icon: Home, gradient: "from-emerald-100 to-teal-200 text-emerald-600" },
  "sports & fitness": { icon: Dumbbell, gradient: "from-rose-100 to-pink-200 text-rose-500" },
  fashion: { icon: Shirt, gradient: "from-purple-100 to-fuchsia-200 text-purple-500" },
};
const FALLBACK = { icon: Package, gradient: "from-slate-100 to-slate-200 text-slate-400" };

/** The product photo, or a category-themed placeholder when there is none (or it fails to load). */
export default function ProductImage({
  product,
  className = "",
  iconSize = "h-12 w-12",
}: {
  product: Pick<Product, "name" | "imageUrl" | "category">;
  className?: string;
  iconSize?: string;
}) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);

  if (product.imageUrl && product.imageUrl !== failedUrl) {
    return (
      // eslint-disable-next-line @next/next/no-img-element -- admin-supplied URLs from any host
      <img
        src={product.imageUrl}
        alt={product.name}
        loading="lazy"
        onError={() => setFailedUrl(product.imageUrl)}
        className={`object-cover ${className}`}
      />
    );
  }

  const theme = THEMES[product.category?.name.toLowerCase() ?? ""] ?? FALLBACK;
  const Icon = theme.icon;
  return (
    <div
      role="img"
      aria-label={product.name}
      className={`flex items-center justify-center bg-gradient-to-br ${theme.gradient} ${className}`}
    >
      <Icon className={iconSize} strokeWidth={1.5} />
    </div>
  );
}
