import type { Metadata } from "next";
import type { ReactNode } from "react";
import Navbar from "@/components/navbar";
import { AppAuthProvider } from "@/providers/app-auth-provider";
import { AppContextProvider } from "@/providers/app-context-provider";
import "./globals.css";

export const metadata: Metadata = {
  title: "Restaurant Review Platform",
  description: "Discover and review local restaurants",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body className="font-sans antialiased">
        <AppAuthProvider>
          <AppContextProvider>
            <Navbar />
            {children}
          </AppContextProvider>
        </AppAuthProvider>
      </body>
    </html>
  );
}
