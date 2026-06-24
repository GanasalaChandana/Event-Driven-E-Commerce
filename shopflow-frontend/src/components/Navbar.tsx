"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth";
import { Button } from "@/components/ui/button";
import { ShoppingBag, LayoutDashboard, LogOut, User } from "lucide-react";

export default function Navbar() {
  const { user, isAdmin, logout } = useAuthStore();
  const router = useRouter();

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  return (
    <nav className="border-b bg-white sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
        <Link href="/" className="flex items-center gap-2 font-bold text-xl text-slate-900">
          <ShoppingBag className="h-6 w-6 text-blue-600" />
          ShopFlow
        </Link>

        <div className="flex items-center gap-4">
          <Link href="/products" className="text-sm text-slate-600 hover:text-slate-900">
            Products
          </Link>

          {user ? (
            <>
              {!isAdmin && (
                <Link href="/orders" className="text-sm text-slate-600 hover:text-slate-900">
                  My Orders
                </Link>
              )}
              {isAdmin && (
                <Link href="/admin" className="flex items-center gap-1 text-sm text-slate-600 hover:text-slate-900">
                  <LayoutDashboard className="h-4 w-4" />
                  Admin
                </Link>
              )}
              <Link href="/profile" className="flex items-center gap-2 text-sm text-slate-600 hover:text-slate-900">
                <User className="h-4 w-4" />
                {user.name}
              </Link>
              <Button variant="ghost" size="sm" onClick={handleLogout}>
                <LogOut className="h-4 w-4 mr-1" />
                Logout
              </Button>
            </>
          ) : (
            <>
              <Link href="/login">
                <Button variant="ghost" size="sm">Login</Button>
              </Link>
              <Link href="/register">
                <Button size="sm">Register</Button>
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
