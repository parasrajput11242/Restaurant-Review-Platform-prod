import type { Role } from "@/domain/domain";

const KNOWN_ROLES: Role[] = ["ADMIN", "RESTAURANT_OWNER", "USER"];

/**
 * Reads the Keycloak realm roles from the access token. This is only used to decide what to
 * SHOW; the API checks the same roles again on every request, so tampering here gains nothing.
 */
export function rolesFromAccessToken(accessToken: string | undefined): Role[] {
  if (!accessToken) return [];
  try {
    const payload = accessToken.split(".")[1];
    const json = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
    const claims = JSON.parse(json) as { realm_access?: { roles?: string[] } };
    const roles = claims.realm_access?.roles ?? [];
    return KNOWN_ROLES.filter((role) => roles.includes(role));
  } catch {
    return [];
  }
}

/** The API sends UTC timestamps without a zone suffix; treat them as UTC when displaying. */
export function parseServerDate(value: string): Date {
  return new Date(/[zZ]|[+-]\d\d:?\d\d$/.test(value) ? value : `${value}Z`);
}
