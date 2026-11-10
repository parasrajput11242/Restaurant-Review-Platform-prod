"use client";

import Link from "next/link";
import { useEffect, type ReactNode } from "react";
import { useAuth } from "react-oidc-context";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import type { Role } from "@/domain/domain";
import { useRoles } from "@/hooks/use-roles";

interface RequireRoleProps {
  role: Role;
  children: ReactNode;
}

/**
 * Page guard: signs the visitor in if needed and shows a friendly message when their role is not
 * enough. This is a convenience only. The API enforces the same rules.
 */
export default function RequireRole({ role, children }: RequireRoleProps) {
  const auth = useAuth();
  const { isLoading, isAuthenticated, roles } = useRoles();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      void auth.signinRedirect();
    }
  }, [isLoading, isAuthenticated, auth]);

  if (isLoading || !isAuthenticated) {
    return <p className="max-w-[1200px] mx-auto px-4 py-8">Loading…</p>;
  }

  if (!roles.includes(role)) {
    return (
      <div className="max-w-[600px] mx-auto px-4 py-12">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <h1 className="text-2xl font-semibold">You do not have access to this page</h1>
            <p className="text-muted-foreground">
              {role === "RESTAURANT_OWNER"
                ? "Only restaurant owners can do this. You can ask for owner access and an admin will review it."
                : "This page is only for administrators."}
            </p>
            <div className="flex gap-2">
              {role === "RESTAURANT_OWNER" && (
                <Button asChild>
                  <Link href="/become-owner">Request owner access</Link>
                </Button>
              )}
              <Button variant="outline" asChild>
                <Link href="/">Back to home</Link>
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return <>{children}</>;
}
