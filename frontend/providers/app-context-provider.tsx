"use client";

import { createContext, useContext, useEffect, useState, type PropsWithChildren } from "react";
import { useAuth, type AuthContextProps } from "react-oidc-context";
import type { ApiService } from "@/services/api/apiService";
import { AxiosApiService, type Session } from "@/services/api/axiosApiService";

interface AppContextType {
  apiService: ApiService;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

/** Always holds the newest auth state, so the long-lived API client never sends a stale token. */
class SessionHolder {
  private auth: AuthContextProps | undefined;

  update(auth: AuthContextProps) {
    this.auth = auth;
  }

  get(): Session {
    const auth = this.auth;
    return {
      accessToken: auth?.user?.access_token,
      expired: auth?.user?.expired ?? false,
      renew: async () => {
        try {
          return (await auth?.signinSilent())?.access_token;
        } catch {
          return undefined;
        }
      },
    };
  }
}

export function AppContextProvider({ children }: PropsWithChildren) {
  const auth = useAuth();
  const [holder] = useState(() => new SessionHolder());
  const [value] = useState<AppContextType>(() => ({
    apiService: new AxiosApiService("/api", () => holder.get()),
  }));

  useEffect(() => {
    holder.update(auth);
  }, [holder, auth]);

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export const useAppContext = (): AppContextType => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error("useAppContext must be used within an AppContextProvider");
  }
  return context;
}
