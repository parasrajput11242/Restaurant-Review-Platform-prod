"use client";

import { useMemo } from "react";
import { useAuth } from "react-oidc-context";
import type { Role } from "@/domain/domain";
import { rolesFromAccessToken } from "@/lib/auth";

export interface CurrentUser {
  isLoading: boolean;
  isAuthenticated: boolean;
  userId?: string;
  username?: string;
  roles: Role[];
  isAdmin: boolean;
  isOwner: boolean; // RESTAURANT_OWNER (admins count too)
  isUser: boolean;
  /** Fetches a fresh token so a just-granted role shows up without logging out and in. */
  refreshRoles: () => Promise<void>;
}

export function useRoles(): CurrentUser {
  const auth = useAuth();
  const token = auth.user?.access_token;
  const roles = useMemo(() => rolesFromAccessToken(token), [token]);

  return {
    isLoading: auth.isLoading,
    isAuthenticated: auth.isAuthenticated,
    userId: auth.user?.profile.sub,
    username: auth.user?.profile.preferred_username as string | undefined,
    roles,
    isAdmin: roles.includes("ADMIN"),
    isOwner: roles.includes("RESTAURANT_OWNER"),
    isUser: roles.includes("USER"),
    refreshRoles: async () => {
      await auth.signinSilent();
    },
  };
}
