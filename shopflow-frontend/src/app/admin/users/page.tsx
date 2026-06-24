"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import AdminLayout from "@/components/AdminLayout";
import { getAllUsers } from "@/lib/queries";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { User } from "lucide-react";

export default function AdminUsersPage() {
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["admin-users", page],
    queryFn: () => getAllUsers(page),
  });

  return (
    <AdminLayout>
      <h1 className="text-3xl font-bold text-slate-900 mb-6">Users</h1>

      {isLoading ? (
        <div className="space-y-3">{Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-16 rounded-xl" />)}</div>
      ) : (
        <Card>
          <CardContent className="p-0">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b">
                <tr>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">User</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Email</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Role</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Joined</th>
                </tr>
              </thead>
              <tbody>
                {data?.content.map((user) => (
                  <tr key={user.id} className="border-b last:border-0 hover:bg-slate-50">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="h-8 w-8 rounded-full bg-blue-100 flex items-center justify-center">
                          <User className="h-4 w-4 text-blue-600" />
                        </div>
                        <span className="font-medium">{user.name}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-slate-500">{user.email}</td>
                    <td className="px-4 py-3">
                      <Badge variant={user.role === "ADMIN" ? "default" : "secondary"}>
                        {user.role}
                      </Badge>
                    </td>
                    <td className="px-4 py-3 text-slate-500">
                      {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-6">
          <Button variant="outline" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
          <span className="flex items-center text-sm text-slate-500">Page {page + 1} of {data.totalPages}</span>
          <Button variant="outline" disabled={page >= data.totalPages - 1} onClick={() => setPage(page + 1)}>Next</Button>
        </div>
      )}
    </AdminLayout>
  );
}
