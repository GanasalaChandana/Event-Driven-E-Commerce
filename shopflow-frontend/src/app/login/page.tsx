"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAuthStore } from "@/store/auth";
import { login } from "@/lib/queries";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ShoppingBag, Loader2 } from "lucide-react";

export default function LoginPage() {
  const router = useRouter();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const { mutate, isPending } = useMutation({
    mutationFn: () => login(email, password),
    onSuccess: (data) => {
      setAuth(data);
      toast.success(`Welcome back, ${data.name}!`);
      router.push(data.role === "ADMIN" ? "/admin" : "/products");
    },
    onError: () => toast.error("Invalid email or password"),
  });

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-md">
        <div className="flex justify-center mb-8">
          <Link href="/" className="flex items-center gap-2 text-2xl font-bold text-slate-900">
            <ShoppingBag className="h-8 w-8 text-blue-600" />
            ShopFlow
          </Link>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="text-2xl">Welcome back</CardTitle>
            <CardDescription>Sign in to your account</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={(e) => { e.preventDefault(); mutate(); }} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
                <Input
                  id="password"
                  type="password"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
              <Button type="submit" className="w-full" disabled={isPending}>
                {isPending ? <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Signing in...</> : "Sign in"}
              </Button>
            </form>

            <div className="mt-4 text-center text-sm text-slate-500">
              Don&apos;t have an account?{" "}
              <Link href="/register" className="text-blue-600 hover:underline font-medium">
                Register
              </Link>
            </div>

            <div className="mt-4 p-3 bg-slate-50 rounded-lg text-xs text-slate-500">
              <p className="font-medium mb-1">Demo accounts:</p>
              <p>Admin: admin@shopflow.com / admin123</p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
