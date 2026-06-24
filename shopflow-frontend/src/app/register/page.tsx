"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAuthStore } from "@/store/auth";
import { register } from "@/lib/queries";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ShoppingBag, Loader2 } from "lucide-react";

export default function RegisterPage() {
  const router = useRouter();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const { mutate, isPending } = useMutation({
    mutationFn: () => register(name, email, password),
    onSuccess: (data) => {
      setAuth(data);
      toast.success(`Welcome to ShopFlow, ${data.name}!`);
      router.push("/products");
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.detail || "Registration failed";
      toast.error(msg);
    },
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
            <CardTitle className="text-2xl">Create an account</CardTitle>
            <CardDescription>Join ShopFlow today</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={(e) => { e.preventDefault(); mutate(); }} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="name">Full name</Label>
                <Input
                  id="name"
                  placeholder="Alice Smith"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="alice@example.com"
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
                  placeholder="Min. 6 characters"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  minLength={6}
                />
              </div>
              <Button type="submit" className="w-full" disabled={isPending}>
                {isPending ? <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Creating account...</> : "Create account"}
              </Button>
            </form>

            <div className="mt-4 text-center text-sm text-slate-500">
              Already have an account?{" "}
              <Link href="/login" className="text-blue-600 hover:underline font-medium">
                Sign in
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
