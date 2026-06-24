"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import { useAuthStore } from "@/store/auth";
import { Button } from "@/components/ui/button";
import { ShoppingBag, LayoutDashboard, LogOut, User, Menu, X, ShoppingCart, Package, Sun, Moon } from "lucide-react";
import { useTheme } from "next-themes";

export default function Navbar() {
  const { user, isAdmin, logout } = useAuthStore();
  const router = useRouter();
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);
  const { theme, setTheme } = useTheme();

  const handleLogout = () => {
    logout();
    router.push("/login");
    setMenuOpen(false);
  };

  const close = () => setMenuOpen(false);

  return (
    <nav className="border-b bg-white sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
        {/* Logo */}
        <Link href="/" className="flex items-center gap-2 font-bold text-xl text-slate-900" onClick={close}>
          <ShoppingBag className="h-6 w-6 text-blue-600" />
          ShopFlow
        </Link>

        {/* Desktop nav */}
        <div className="hidden md:flex items-center gap-4">
          <Link href="/products" className="text-sm text-slate-600 hover:text-slate-900">Products</Link>
          {user ? (
            <>
              {!isAdmin && (
                <Link href="/orders" className="text-sm text-slate-600 hover:text-slate-900">My Orders</Link>
              )}
              {isAdmin && (
                <Link href="/admin" className="flex items-center gap-1 text-sm text-slate-600 hover:text-slate-900">
                  <LayoutDashboard className="h-4 w-4" /> Admin
                </Link>
              )}
              <Link href="/profile" className="flex items-center gap-2 text-sm text-slate-600 hover:text-slate-900">
                <User className="h-4 w-4" />{user.name}
              </Link>
              <Button variant="ghost" size="sm" onClick={handleLogout}>
                <LogOut className="h-4 w-4 mr-1" /> Logout
              </Button>
            </>
          ) : (
            <>
              <Link href="/login"><Button variant="ghost" size="sm">Login</Button></Link>
              <Link href="/register"><Button size="sm">Register</Button></Link>
            </>
          )}
        </div>

        {/* Dark mode toggle + Mobile hamburger */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
            className="p-2 rounded-lg text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
            aria-label="Toggle dark mode"
          >
            {theme === "dark" ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
          </button>
          <button
            className="md:hidden p-2 rounded-lg text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
            onClick={() => setMenuOpen(!menuOpen)}
          >
            {menuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
          </button>
        </div>
      </div>

      {/* Mobile menu */}
      {menuOpen && (
        <div className="md:hidden border-t bg-white px-4 py-3 space-y-1">
          <MobileLink href="/products" onClick={close} icon={<Package className="h-4 w-4" />}>Products</MobileLink>

          {user ? (
            <>
              {!isAdmin && (
                <MobileLink href="/orders" onClick={close} icon={<ShoppingCart className="h-4 w-4" />}>My Orders</MobileLink>
              )}
              {isAdmin && (
                <MobileLink href="/admin" onClick={close} icon={<LayoutDashboard className="h-4 w-4" />}>Admin Dashboard</MobileLink>
              )}
              <MobileLink href="/profile" onClick={close} icon={<User className="h-4 w-4" />}>{user.name}</MobileLink>
              <button
                onClick={handleLogout}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-red-500 hover:bg-red-50"
              >
                <LogOut className="h-4 w-4" /> Logout
              </button>
            </>
          ) : (
            <>
              <MobileLink href="/login" onClick={close} icon={<User className="h-4 w-4" />}>Login</MobileLink>
              <MobileLink href="/register" onClick={close} icon={<ShoppingBag className="h-4 w-4" />}>Register</MobileLink>
            </>
          )}
        </div>
      )}
    </nav>
  );
}

function MobileLink({ href, onClick, icon, children }: {
  href: string;
  onClick: () => void;
  icon: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={href}
      onClick={onClick}
      className="flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-slate-700 hover:bg-slate-100"
    >
      {icon}{children}
    </Link>
  );
}
