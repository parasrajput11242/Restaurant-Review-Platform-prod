"use client";

import { AuthProvider, type AuthProviderProps } from "react-oidc-context";
import type { PropsWithChildren } from "react";

const baseUrl = process.env.NEXT_PUBLIC_BASE_URL ?? "http://localhost:3000";

// Keycloak login with the Authorization Code flow + PKCE (no client secret in the browser).
const oidcConfig: AuthProviderProps = {
  authority: `${process.env.NEXT_PUBLIC_KEYCLOAK_URL}/realms/${process.env.NEXT_PUBLIC_KEYCLOAK_REALM}`,
  client_id: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID ?? "restaurant-review-app",
  redirect_uri: baseUrl,
  post_logout_redirect_uri: baseUrl,
  scope: "openid profile email",
  automaticSilentRenew: true,
  onSigninCallback: () => {
    // Drop ?code=...&state=... from the address bar without reloading
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};

export function AppAuthProvider({ children }: PropsWithChildren) {
  return <AuthProvider {...oidcConfig}>{children}</AuthProvider>;
}
