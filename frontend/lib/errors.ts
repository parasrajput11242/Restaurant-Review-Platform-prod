import axios from "axios";

/** Turns any thrown value into a message that is safe to show to a person. */
export function errorMessage(error: unknown, fallback = "Something went wrong"): string {
  if (axios.isAxiosError(error)) {
    const data: unknown = error.response?.data;
    if (data && typeof data === "object" && "message" in data) {
      const message = (data as { message?: unknown }).message;
      if (typeof message === "string" && message) return message;
    }
    if (error.response?.status === 401) return "Please sign in again";
    if (error.response?.status === 403) return "You do not have permission to do this";
    if (!error.response) return "Cannot reach the server";
  }
  return fallback;
}
