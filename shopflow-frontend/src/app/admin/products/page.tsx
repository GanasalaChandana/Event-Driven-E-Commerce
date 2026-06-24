"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import AdminLayout from "@/components/AdminLayout";
import { getProducts, getCategories, createProduct, updateProduct, deleteProduct } from "@/lib/queries";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Product } from "@/lib/types";
import { Plus, Pencil, Trash2 } from "lucide-react";

const emptyForm = { name: "", description: "", price: "", sku: "", categoryId: "" };

export default function AdminProductsPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Product | null>(null);
  const [form, setForm] = useState(emptyForm);

  const { data, isLoading } = useQuery({
    queryKey: ["products", "", null, 0],
    queryFn: () => getProducts({ size: 100 }),
  });

  const { data: categories } = useQuery({
    queryKey: ["categories"],
    queryFn: getCategories,
  });

  const { mutate: save, isPending } = useMutation({
    mutationFn: () => {
      const payload = {
        name: form.name,
        description: form.description,
        price: parseFloat(form.price),
        sku: form.sku,
        categoryId: form.categoryId || null,
      };
      return editing ? updateProduct(editing.id, payload) : createProduct(payload);
    },
    onSuccess: () => {
      toast.success(editing ? "Product updated" : "Product created");
      queryClient.invalidateQueries({ queryKey: ["products"] });
      setOpen(false);
      setForm(emptyForm);
      setEditing(null);
    },
    onError: (err: any) => toast.error(err?.response?.data?.detail || "Failed to save product"),
  });

  const { mutate: remove } = useMutation({
    mutationFn: (id: string) => deleteProduct(id),
    onSuccess: () => {
      toast.success("Product deleted");
      queryClient.invalidateQueries({ queryKey: ["products"] });
    },
    onError: () => toast.error("Failed to delete product"),
  });

  const openEdit = (p: Product) => {
    setEditing(p);
    setForm({
      name: p.name,
      description: p.description || "",
      price: p.price.toString(),
      sku: p.sku,
      categoryId: p.category?.id || "",
    });
    setOpen(true);
  };

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setOpen(true);
  };

  return (
    <AdminLayout>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold text-slate-900">Products</h1>
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4 mr-2" /> Add Product
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-3">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-14 rounded-xl" />)}</div>
      ) : (
        <Card>
          <CardContent className="p-0">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b">
                <tr>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Name</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">SKU</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Price</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Category</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data?.content.map((product) => (
                  <tr key={product.id} className="border-b last:border-0 hover:bg-slate-50">
                    <td className="px-4 py-3 font-medium">{product.name}</td>
                    <td className="px-4 py-3 text-slate-500">{product.sku}</td>
                    <td className="px-4 py-3 font-semibold text-blue-600">${product.price.toFixed(2)}</td>
                    <td className="px-4 py-3">
                      {product.category ? <Badge variant="secondary">{product.category.name}</Badge> : "—"}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-2">
                        <Button variant="outline" size="sm" onClick={() => openEdit(product)}>
                          <Pencil className="h-3 w-3" />
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          className="text-red-500 hover:text-red-700"
                          onClick={() => remove(product.id)}
                        >
                          <Trash2 className="h-3 w-3" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing ? "Edit Product" : "Add Product"}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 pt-2">
            <div className="space-y-2">
              <Label>Name *</Label>
              <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>Description</Label>
              <Input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label>Price *</Label>
                <Input type="number" step="0.01" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} />
              </div>
              <div className="space-y-2">
                <Label>SKU *</Label>
                <Input value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} />
              </div>
            </div>
            <div className="space-y-2">
              <Label>Category</Label>
              <Select value={form.categoryId} onValueChange={(v) => setForm({ ...form, categoryId: v })}>
                <SelectTrigger>
                  <SelectValue placeholder="Select category" />
                </SelectTrigger>
                <SelectContent>
                  {categories?.map((cat) => (
                    <SelectItem key={cat.id} value={cat.id}>{cat.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <Button className="w-full" disabled={!form.name || !form.price || !form.sku || isPending} onClick={() => save()}>
              {isPending ? "Saving..." : editing ? "Update Product" : "Create Product"}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </AdminLayout>
  );
}
