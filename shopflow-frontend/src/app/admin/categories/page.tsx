"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import AdminLayout from "@/components/AdminLayout";
import { getCategories, createCategory } from "@/lib/queries";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { Plus, Tag } from "lucide-react";

export default function AdminCategoriesPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  const { data: categories, isLoading } = useQuery({
    queryKey: ["categories"],
    queryFn: getCategories,
  });

  const { mutate: create, isPending } = useMutation({
    mutationFn: () => createCategory({ name, description }),
    onSuccess: () => {
      toast.success("Category created");
      queryClient.invalidateQueries({ queryKey: ["categories"] });
      setOpen(false);
      setName("");
      setDescription("");
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.detail || "Category already exists");
    },
  });

  return (
    <AdminLayout>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Tag className="h-7 w-7 text-blue-600" />
          <h1 className="text-3xl font-bold text-slate-900">Categories</h1>
        </div>
        <Button onClick={() => setOpen(true)}>
          <Plus className="h-4 w-4 mr-2" /> Add Category
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-16 rounded-xl" />)}
        </div>
      ) : categories?.length === 0 ? (
        <div className="text-center py-20 text-slate-400">
          <Tag className="h-12 w-12 mx-auto mb-3 opacity-30" />
          <p>No categories yet. Add one to get started.</p>
        </div>
      ) : (
        <Card>
          <CardContent className="p-0">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b">
                <tr>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Name</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Description</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">ID</th>
                </tr>
              </thead>
              <tbody>
                {categories?.map((cat) => (
                  <tr key={cat.id} className="border-b last:border-0 hover:bg-slate-50">
                    <td className="px-4 py-3 font-medium">{cat.name}</td>
                    <td className="px-4 py-3 text-slate-500">{cat.description || "—"}</td>
                    <td className="px-4 py-3 text-slate-400 font-mono text-xs">{cat.id}</td>
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
            <DialogTitle>Add Category</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 pt-2">
            <div className="space-y-2">
              <Label>Name *</Label>
              <Input
                placeholder="e.g. Clothing"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label>Description</Label>
              <Input
                placeholder="e.g. Apparel and accessories"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
            <Button
              className="w-full"
              disabled={!name || isPending}
              onClick={() => create()}
            >
              {isPending ? "Creating..." : "Create Category"}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </AdminLayout>
  );
}
