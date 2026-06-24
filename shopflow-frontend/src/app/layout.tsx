"use client";

import { Geist } from "next/font/google";
import "./globals.css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "@/components/ui/sonner";
import { useEffect, useState } from "react";
import { useAuthStore } from "@/store/auth";

const geist = Geist({ subsets: ["latin"], variable: "--font-geist-sans" });

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
});

function AuthInit({ children }: { children: React.ReactNode }) {
  const init = useAuthStore((s) => s.init);
  useEffect(() => { init(); }, [init]);
  return <>{children}</>;
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={geist.variable}>
      <body className="min-h-screen bg-background font-sans antialiased">
        <QueryClientProvider client={queryClient}>
          <AuthInit>
            {children}
            <Toaster richColors position="top-right" />
          </AuthInit>
        </QueryClientProvider>
      </body>
    </html>
  );
}
