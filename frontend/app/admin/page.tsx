"use client";

import { useCallback, useEffect, useState } from "react";
import PageNav from "@/components/page-nav";
import RequireRole from "@/components/require-role";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import type { OwnerRequest, OwnerRequestStatus } from "@/domain/domain";
import { parseServerDate } from "@/lib/auth";
import { errorMessage } from "@/lib/errors";
import { useAppContext } from "@/providers/app-context-provider";

const TABS: OwnerRequestStatus[] = ["PENDING", "APPROVED", "REJECTED"];

function RequestCard({
  request,
  onChanged,
}: {
  request: OwnerRequest;
  onChanged: () => Promise<void>;
}) {
  const { apiService } = useAppContext();
  const [busy, setBusy] = useState(false);
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string>();

  const run = async (action: () => Promise<unknown>) => {
    setBusy(true);
    setError(undefined);
    try {
      await action();
      await onChanged();
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  };

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle className="text-lg">
          {request.username} <span className="text-sm font-normal text-muted-foreground">{request.email}</span>
        </CardTitle>
        <Badge variant={request.status === "PENDING" ? "default" : "secondary"}>{request.status}</Badge>
      </CardHeader>
      <CardContent className="space-y-3">
        <p className="whitespace-pre-line text-sm">{request.message}</p>
        <p className="text-xs text-muted-foreground">
          Sent {parseServerDate(request.createdAt).toLocaleString()}
          {request.reviewedBy && ` · reviewed by ${request.reviewedBy}`}
          {request.rejectionReason && ` · reason: ${request.rejectionReason}`}
        </p>

        {request.status === "PENDING" &&
          (rejecting ? (
            <div className="flex gap-2">
              <Input
                placeholder="Reason (optional)"
                value={reason}
                maxLength={500}
                onChange={(e) => setReason(e.target.value)}
              />
              <Button
                variant="destructive"
                disabled={busy}
                onClick={() => void run(() => apiService.rejectOwnerRequest(request.id, reason))}
              >
                Confirm reject
              </Button>
              <Button variant="outline" onClick={() => setRejecting(false)}>
                Cancel
              </Button>
            </div>
          ) : (
            <div className="flex gap-2">
              <Button disabled={busy} onClick={() => void run(() => apiService.approveOwnerRequest(request.id))}>
                {busy ? "Working…" : "Approve"}
              </Button>
              <Button variant="outline" disabled={busy} onClick={() => setRejecting(true)}>
                Reject
              </Button>
            </div>
          ))}

        {error && (
          <p role="alert" className="text-sm text-red-600">
            {error}
          </p>
        )}
      </CardContent>
    </Card>
  );
}

function OwnerRequests() {
  const { apiService } = useAppContext();
  const [status, setStatus] = useState<OwnerRequestStatus>("PENDING");
  const [requests, setRequests] = useState<OwnerRequest[]>([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  const load = useCallback(
    async (target: number, tab: OwnerRequestStatus) => {
      setLoading(true);
      setError(undefined);
      try {
        const result = await apiService.listOwnerRequests(tab, target, 10);
        setRequests(result.content);
        setPage(result.page);
        setTotalPages(result.totalPages);
      } catch (err) {
        setError(errorMessage(err));
      } finally {
        setLoading(false);
      }
    },
    [apiService],
  );

  useEffect(() => {
    void load(1, status);
  }, [load, status]);

  return (
    <div className="max-w-[800px] mx-auto px-4 py-8 space-y-6">
      <h1 className="text-3xl font-bold">Owner requests</h1>

      <div className="flex gap-2">
        {TABS.map((tab) => (
          <Button key={tab} variant={tab === status ? "default" : "outline"} onClick={() => setStatus(tab)}>
            {tab.charAt(0) + tab.slice(1).toLowerCase()}
          </Button>
        ))}
      </div>

      {error && (
        <p role="alert" className="text-red-600">
          {error}
        </p>
      )}
      {loading ? (
        <p>Loading…</p>
      ) : requests.length === 0 ? (
        <p className="text-muted-foreground">No {status.toLowerCase()} requests.</p>
      ) : (
        <div className="space-y-4">
          {requests.map((request) => (
            <RequestCard key={request.id} request={request} onChanged={() => load(page, status)} />
          ))}
        </div>
      )}
      <PageNav page={page} totalPages={totalPages} onChange={(p) => void load(p, status)} />
    </div>
  );
}

export default function AdminPage() {
  return (
    <RequireRole role="ADMIN">
      <OwnerRequests />
    </RequireRole>
  );
}
