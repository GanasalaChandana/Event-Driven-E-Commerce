"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getProducts, getCategories } from "@/lib/queries";
import Navbar from "@/components/Navbar";
import ProductCard from "@/components/ProductCard";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Search, X } from "lucide-react";

export default function ProductsPage() {
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  const { data: categories } = useQuery({
    queryKey: ["categories"],
    queryFn: getCategories,
  });

  const { data, isLoading } = useQuery({
    queryKey: ["products", query, selectedCategory, page],
    queryFn: () => getProducts({
      q: query || undefined,
      categoryId: selectedCategory || undefined,
      page,
      size: 12,
    }),
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setQuery(search);
    setPage(0);
  };

  const clearFilters = () => {
    setSearch("");
    setQuery("");
    setSelectedCategory(null);
    setPage(0);
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <Navbar />
      <main className="flex-1 max-w-7xl mx-auto w-full px-4 py-8">
        <h1 className="text-3xl font-bold text-slate-900 mb-6">Products</h1>

        {/* Search + Filter */}
        <div className="bg-white rounded-xl p-4 shadow-sm mb-6 space-y-4">
          <form onSubmit={handleSearch} className="flex gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <Input
                placeholder="Search products..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9"
              />
            </div>
            <Button type="submit">Search</Button>
            {(query || selectedCategory) && (
              <Button type="button" variant="ghost" onClick={clearFilters}>
                <X className="h-4 w-4 mr-1" /> Clear
              </Button>
            )}
          </form>

          {/* Category filters */}
          {categories && categories.length > 0 && (
            <div className="flex flex-wrap gap-2">
              <span className="text-sm text-slate-500 self-center">Category:</span>
              {categories.map((cat) => (
                <Badge
                  key={cat.id}
                  variant={selectedCategory === cat.id ? "default" : "outline"}
                  className="cursor-pointer"
                  onClick={() => {
                    setSelectedCategory(selectedCategory === cat.id ? null : cat.id);
                    setPage(0);
                  }}
                >
                  {cat.name}
                </Badge>
              ))}
            </div>
          )}
        </div>

        {/* Results count */}
        {data && (
          <p className="text-sm text-slate-500 mb-4">
            {data.totalElements} product{data.totalElements !== 1 ? "s" : ""} found
          </p>
        )}

        {/* Product grid */}
        {isLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {Array.from({ length: 8 }).map((_, i) => (
              <Skeleton key={i} className="h-64 rounded-xl" />
            ))}
          </div>
        ) : data?.content.length === 0 ? (
          <div className="text-center py-20 text-slate-400">
            <p className="text-lg">No products found</p>
            <p className="text-sm mt-1">Try a different search or category</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {data?.content.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex justify-center gap-2 mt-8">
            <Button
              variant="outline"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              Previous
            </Button>
            <span className="flex items-center text-sm text-slate-500">
              Page {page + 1} of {data.totalPages}
            </span>
            <Button
              variant="outline"
              disabled={page >= data.totalPages - 1}
              onClick={() => setPage(page + 1)}
            >
              Next
            </Button>
          </div>
        )}
      </main>
    </div>
  );
}
