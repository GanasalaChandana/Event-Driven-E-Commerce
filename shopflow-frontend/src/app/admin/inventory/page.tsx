"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import AdminLayout from "@/components/AdminLayout";
import { getProducts, getInventory, adjustStock, addStock } from "@/lib/queries";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Product } from "@/lib/types";
import { Boxes, Plus, Edit } from "lucide-react";

function InventoryRow({ product }: { product: Product }) {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [qty, setQty] = useState("");

  const { data: inv } = useQuery({
    queryKey: ["inventory", product.id],
    queryFn: () => getInventory(product.id),
  });

  const { mutate: adjust, isPending } = useMutation({
    mutationFn: () => adjustStock(product.id, parseInt(qty)),
    onSuccess: () => {
      toast.success("Stock updated");
      queryClient.invalidateQueries({ queryKey: ["inventory", product.id] });
      setOpen(false);
      setQty("");
    },
    onError: () => toast.error("Failed to update stock"),
  });

  const { mutate: add, isPending: isAdding } = useMutation({
    mutationFn: () => addStock({ productId: product.id, productName: product.name, quantityToAdd: parseInt(qty) }),
    onSuccess: () => {
      toast.success("Stock added");
      queryClient.invalidateQueries({ queryKey: ["inventory", product.id] });
      setOpen(false);
      setQty("");
    },
    onError: () => toast.error("Failed to add stock"),
  });

  return (
    <>
      <tr className="border-b last:border-0 hover:bg-slate-50">
        <td className="px-4 py-3 font-medium">{product.name}</td>
        <td className="px-4 py-3 text-slate-500">{product.sku}</td>
        <td className="px-4 py-3">
          {inv ? (
            <span className={`font-semibold ${inv.availableQuantity > 0 ? "text-green-600" : "text-red-500"}`}>
              {inv.availableQuantity}
            </span>
          ) : "—"}
        </td>
        <td className="px-4 py-3 text-slate-400">{inv?.reservedQuantity ?? "—"}</td>
        <td className="px-4 py-3">
          <Button variant="outline" size="sm" onClick={() => setOpen(true)}>
            <Edit className="h-3 w-3 mr-1" /> Adjust
          </Button>
        </td>
      </tr>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Adjust Stock — {product.name}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 pt-2">
            <p className="text-sm text-slate-500">Current available: <strong>{inv?.availableQuantity ?? "—"}</strong></p>
            <div className="space-y-2">
              <Label>New absolute quantity</Label>
              <Input type="number" min="0" value={qty} onChange={(e) => setQty(e.target.value)} placeholder="e.g. 100" />
            </div>
            <div className="flex gap-2">
              <Button className="flex-1" disabled={!qty || isPending} onClick={() => adjust()}>
                {isPending ? "Updating..." : "Set Quantity"}
              </Button>
              <Button variant="outline" className="flex-1" disabled={!qty || isAdding} onClick={() => add()}>
                <Plus className="h-4 w-4 mr-1" />
                {isAdding ? "Adding..." : "Add Stock"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}

export default function AdminInventoryPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["products", "", null, 0],
    queryFn: () => getProducts({ size: 100 }),
  });

  return (
    <AdminLayout>
      <div className="flex items-center gap-3 mb-6">
        <Boxes className="h-7 w-7 text-blue-600" />
        <h1 className="text-3xl font-bold text-slate-900">Inventory</h1>
      </div>

      {isLoading ? (
        <div className="space-y-3">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-14 rounded-xl" />)}</div>
      ) : (
        <Card>
          <CardContent className="p-0">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b">
                <tr>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Product</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">SKU</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Available</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Reserved</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data?.content.map((product) => (
                  <InventoryRow key={product.id} product={product} />
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}
    </AdminLayout>
  );
}
