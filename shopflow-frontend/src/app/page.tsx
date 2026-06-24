import Link from "next/link";
import { Button } from "@/components/ui/button";
import Navbar from "@/components/Navbar";
import { ShoppingBag, Zap, Shield, Package } from "lucide-react";

export default function Home() {
  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      <main className="flex-1">
        {/* Hero */}
        <section className="bg-gradient-to-br from-slate-900 to-blue-900 text-white py-24 px-4">
          <div className="max-w-4xl mx-auto text-center">
            <div className="flex justify-center mb-6">
              <ShoppingBag className="h-16 w-16 text-blue-400" />
            </div>
            <h1 className="text-5xl font-bold mb-6">Welcome to ShopFlow</h1>
            <p className="text-xl text-slate-300 mb-10 max-w-2xl mx-auto">
              A modern e-commerce platform powered by a Spring Boot microservices backend.
              Browse products, place orders, and track everything in real time.
            </p>
            <div className="flex gap-4 justify-center">
              <Link href="/products">
                <Button size="lg" className="bg-blue-500 hover:bg-blue-600">
                  Browse Products
                </Button>
              </Link>
              <Link href="/register">
                <Button size="lg" variant="outline" className="text-white border-white hover:bg-white hover:text-slate-900">
                  Get Started
                </Button>
              </Link>
            </div>
          </div>
        </section>

        {/* Features */}
        <section className="py-20 px-4 bg-slate-50">
          <div className="max-w-5xl mx-auto">
            <h2 className="text-3xl font-bold text-center text-slate-900 mb-12">Why ShopFlow?</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              <div className="bg-white rounded-xl p-6 shadow-sm text-center">
                <Zap className="h-10 w-10 text-blue-500 mx-auto mb-4" />
                <h3 className="font-semibold text-lg mb-2">Fast & Reliable</h3>
                <p className="text-slate-500 text-sm">Event-driven architecture ensures orders are processed instantly with automatic confirmation.</p>
              </div>
              <div className="bg-white rounded-xl p-6 shadow-sm text-center">
                <Shield className="h-10 w-10 text-blue-500 mx-auto mb-4" />
                <h3 className="font-semibold text-lg mb-2">Secure</h3>
                <p className="text-slate-500 text-sm">JWT authentication with role-based access control keeps your account and data safe.</p>
              </div>
              <div className="bg-white rounded-xl p-6 shadow-sm text-center">
                <Package className="h-10 w-10 text-blue-500 mx-auto mb-4" />
                <h3 className="font-semibold text-lg mb-2">Real-time Inventory</h3>
                <p className="text-slate-500 text-sm">Live stock tracking ensures you only order what is available.</p>
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t py-6 text-center text-sm text-slate-500">
        © 2026 ShopFlow · Built with Spring Boot + Next.js
      </footer>
    </div>
  );
}
