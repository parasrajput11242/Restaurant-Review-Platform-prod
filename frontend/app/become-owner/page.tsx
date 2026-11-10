"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import RequireRole from "@/components/require-role";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { OwnerRequest } from "@/domain/domain";
import { useRoles } from "@/hooks/use-roles";
import { parseServerDate } from "@/lib/auth";
import { errorMessage } from "@/lib/errors";
import { useAppContext } from "@/providers/app-context-provider";

function BecomeOwner() {
  const { apiService } = useAppContext();
  const { isOwner, refreshRoles } = useRoles();

  const [loading, setLoading] = useState(true);
  const [request, setRequest] = useState<OwnerRequest | null>(null);
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string>();
  const triedAutoRefresh = useRef(false);

  useEffect(() => {
    let cancelled = false;
    apiService
      .getMyOwnerRequest()
      .then((existing) => !cancelled && setRequest(existing))
      .catch((err) => !cancelled && setError(errorMessage(err)))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [apiService]);

  const refresh = async () => {
    setRefreshing(true);
    setError(undefined);
    try {
      await refreshRoles();
    } catch {
      setError("Could not refresh your session. Please log out and log in again.");
    } finally {
      setRefreshing(false);
    }
  };

  // Approved while this person was away: their token predates the new role, so fetch a new one once.
  useEffect(() => {
    if (request?.status === "APPROVED" && !isOwner && !triedAutoRefresh.current) {
      triedAutoRefresh.current = true;
      void refresh();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [request, isOwner]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(undefined);
    try {
      setRequest(await apiService.submitOwnerRequest(message));
      setMessage("");
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <p className="max-w-[700px] mx-auto px-4 py-8">Loading…</p>;
  }

  if (isOwner) {
    return (
      <div className="max-w-[700px] mx-auto px-4 py-8">
        <Card>
          <CardHeader>
            <CardTitle>You are a restaurant owner</CardTitle>
            <CardDescription>You can add restaurants and manage the ones you created.</CardDescription>
          </CardHeader>
          <CardContent className="flex gap-2">
            <Button asChild>
              <Link href="/restaurants/create">Add a restaurant</Link>
            </Button>
            <Button variant="outline" asChild>
              <Link href="/my-restaurants">My restaurants</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const status = request?.status;

  return (
    <div className="max-w-[700px] mx-auto px-4 py-8 space-y-6">
      <h1 className="text-3xl font-bold">Become a restaurant owner</h1>

      {status === "PENDING" && request && (
        <Card>
          <CardHeader>
            <CardTitle>Your request is waiting for review</CardTitle>
            <CardDescription>Sent on {parseServerDate(request.createdAt).toLocaleDateString()}.</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground whitespace-pre-line">{request.message}</p>
          </CardContent>
        </Card>
      )}

      {status === "APPROVED" && (
        <Card>
          <CardHeader>
            <CardTitle>Your request was approved</CardTitle>
            <CardDescription>Your session needs a refresh before the new permissions appear.</CardDescription>
          </CardHeader>
          <CardContent>
            <Button onClick={() => void refresh()} disabled={refreshing}>
              {refreshing ? "Refreshing…" : "Refresh my session"}
            </Button>
          </CardContent>
        </Card>
      )}

      {status === "REJECTED" && request && (
        <Card>
          <CardHeader>
            <CardTitle>Your last request was not approved</CardTitle>
            {request.rejectionReason && <CardDescription>Reason: {request.rejectionReason}</CardDescription>}
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">You are welcome to apply again below.</p>
          </CardContent>
        </Card>
      )}

      {(!status || status === "REJECTED") && (
        <Card>
          <CardHeader>
            <CardTitle>Tell us about your restaurant</CardTitle>
            <CardDescription>An administrator will review your request.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={submit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="message">Restaurant name, location and your role</Label>
                <Textarea
                  id="message"
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  minLength={10}
                  maxLength={1000}
                  className="min-h-[140px]"
                  placeholder="I own Spice Garden at 12 Market Road, Mohali…"
                  required
                />
              </div>
              <Button type="submit" disabled={submitting || message.trim().length < 10}>
                {submitting ? "Sending…" : "Send request"}
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      {error && (
        <p role="alert" className="text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}

export default function BecomeOwnerPage() {
  return (
    <RequireRole role="USER">
      <BecomeOwner />
    </RequireRole>
  );
}
