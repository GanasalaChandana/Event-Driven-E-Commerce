"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { getMe, getMyOrders } from "@/lib/queries";
import { useAuthStore } from "@/store/auth";
import Navbar from "@/components/Navbar";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import { User, Mail, Shield, Calendar, ShoppingCart, CheckCircle, XCircle, Clock } from "lucide-react";

export default function ProfilePage() {
  const { user } = useAuthStore();
  const router = useRouter();

  useEffect(() => {
    if (!user) router.push("/login");
  }, [user, router]);

  const { data: profile, isLoading: loadingProfile } = useQuery({
    queryKey: ["me"],
    queryFn: getMe,
    enabled: !!user,
  });

  const { data: orders, isLoading: loadingOrders } = useQuery({
    queryKey: ["orders", 0],
    queryFn: () => getMyOrders(0),
    enabled: !!user,
  });

  const confirmed = orders?.content.filter((o) => o.status === "CONFIRMED").length ?? 0;
  const pending = orders?.content.filter((o) => o.status === "PENDING").length ?? 0;
  const cancelled = orders?.content.filter((o) => o.status === "CANCELLED").length ?? 0;
  const totalSpent = orders?.content
    .filter((o) => o.status === "CONFIRMED")
    .reduce((sum, o) => sum + o.totalAmount, 0) ?? 0;

  if (!user) return null;

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <Navbar />
      <main className="max-w-3xl mx-auto w-full px-4 py-8">
        <h1 className="text-3xl font-bold text-slate-900 mb-6">My Profile</h1>

        {/* Profile card */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <User className="h-5 w-5 text-blue-600" />
              Account Details
            </CardTitle>
          </CardHeader>
          <CardContent>
            {loadingProfile ? (
              <div className="space-y-3">
                <Skeleton className="h-5 w-48" />
                <Skeleton className="h-5 w-64" />
                <Skeleton className="h-5 w-32" />
              </div>
            ) : (
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <div className="h-16 w-16 rounded-full bg-blue-100 flex items-center justify-center text-2xl font-bold text-blue-600">
                    {(profile?.name || user.name)?.[0]?.toUpperCase()}
                  </div>
                  <div>
                    <p className="text-xl font-semibold text-slate-900">{profile?.name || user.name}</p>
                    <Badge variant={profile?.role === "ADMIN" ? "default" : "secondary"}>
                      {profile?.role || user.role}
                    </Badge>
                  </div>
                </div>

                <Separator />

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="flex items-center gap-3 text-sm">
                    <Mail className="h-4 w-4 text-slate-400" />
                    <div>
                      <p className="text-slate-500 text-xs">Email</p>
                      <p className="font-medium">{profile?.email || user.email}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3 text-sm">
                    <Shield className="h-4 w-4 text-slate-400" />
                    <div>
                      <p className="text-slate-500 text-xs">Role</p>
                      <p className="font-medium">{profile?.role || user.role}</p>
                    </div>
                  </div>
                  {profile?.createdAt && (
                    <div className="flex items-center gap-3 text-sm">
                      <Calendar className="h-4 w-4 text-slate-400" />
                      <div>
                        <p className="text-slate-500 text-xs">Member since</p>
                        <p className="font-medium">{new Date(profile.createdAt).toLocaleDateString("en-US", { year: "numeric", month: "long", day: "numeric" })}</p>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Order stats */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <ShoppingCart className="h-5 w-5 text-blue-600" />
              Order Statistics
            </CardTitle>
          </CardHeader>
          <CardContent>
            {loadingOrders ? (
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-20 rounded-lg" />)}
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                <div className="bg-slate-50 rounded-xl p-4 text-center">
                  <ShoppingCart className="h-6 w-6 text-slate-400 mx-auto mb-2" />
                  <p className="text-2xl font-bold text-slate-900">{orders?.totalElements ?? 0}</p>
                  <p className="text-xs text-slate-500">Total Orders</p>
                </div>
                <div className="bg-green-50 rounded-xl p-4 text-center">
                  <CheckCircle className="h-6 w-6 text-green-500 mx-auto mb-2" />
                  <p className="text-2xl font-bold text-green-700">{confirmed}</p>
                  <p className="text-xs text-slate-500">Confirmed</p>
                </div>
                <div className="bg-yellow-50 rounded-xl p-4 text-center">
                  <Clock className="h-6 w-6 text-yellow-500 mx-auto mb-2" />
                  <p className="text-2xl font-bold text-yellow-700">{pending}</p>
                  <p className="text-xs text-slate-500">Pending</p>
                </div>
                <div className="bg-red-50 rounded-xl p-4 text-center">
                  <XCircle className="h-6 w-6 text-red-400 mx-auto mb-2" />
                  <p className="text-2xl font-bold text-red-600">{cancelled}</p>
                  <p className="text-xs text-slate-500">Cancelled</p>
                </div>
              </div>
            )}

            {!loadingOrders && totalSpent > 0 && (
              <div className="mt-4 p-4 bg-blue-50 rounded-xl flex justify-between items-center">
                <span className="text-sm text-slate-600 font-medium">Total spent (confirmed orders)</span>
                <span className="text-xl font-bold text-blue-600">${totalSpent.toFixed(2)}</span>
              </div>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  );
}
