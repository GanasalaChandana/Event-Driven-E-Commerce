"use client";

import { useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import { useAuthStore } from "@/store/auth";
import { ShoppingBag, Package, ShoppingCart, Users, LayoutDashboard, LogOut, Boxes, Tag } from "lucide-react";
import { Button } from "@/components/ui/button";

const navItems = [
  { href: "/admin", label: "Dashboard", icon: LayoutDashboard },
  { href: "/admin/products", label: "Products", icon: Package },
  { href: "/admin/categories", label: "Categories", icon: Tag },
  { href: "/admin/inventory", label: "Inventory", icon: Boxes },
  { href: "/admin/orders", label: "Orders", icon: ShoppingCart },
  { href: "/admin/users", label: "Users", icon: Users },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { user, isAdmin, logout, init, hydrated } = useAuthStore();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    init();
  }, [init]);

  useEffect(() => {
    if (hydrated && (!user || !isAdmin)) router.push("/login");
  }, [hydrated, user, isAdmin, router]);

  if (!hydrated || !user || !isAdmin) return null;

  return (
    <div className="min-h-screen flex bg-slate-50">
      {/* Sidebar */}
      <aside className="w-56 bg-slate-900 text-white flex flex-col fixed h-full">
        <div className="p-4 border-b border-slate-700">
          <Link href="/" className="flex items-center gap-2 font-bold text-lg">
            <ShoppingBag className="h-5 w-5 text-blue-400" />
            ShopFlow
          </Link>
          <p className="text-xs text-slate-400 mt-1">Admin Panel</p>
        </div>

        <nav className="flex-1 p-3 space-y-1">
          {navItems.map(({ href, label, icon: Icon }) => (
            <Link
              key={href}
              href={href}
              className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors ${
                pathname === href
                  ? "bg-blue-600 text-white"
                  : "text-slate-300 hover:bg-slate-800 hover:text-white"
              }`}
            >
              <Icon className="h-4 w-4" />
              {label}
            </Link>
          ))}
        </nav>

        <div className="p-3 border-t border-slate-700">
          <div className="text-xs text-slate-400 mb-2 px-3">{user.name}</div>
          <Button
            variant="ghost"
            size="sm"
            className="w-full justify-start text-slate-300 hover:text-white hover:bg-slate-800"
            onClick={() => { logout(); router.push("/login"); }}
          >
            <LogOut className="h-4 w-4 mr-2" />
            Logout
          </Button>
        </div>
      </aside>

      {/* Main content */}
      <main className="ml-56 flex-1 p-8">{children}</main>
    </div>
  );
}
