"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import PageNav from "@/components/page-nav";
import RequireRole from "@/components/require-role";
import RestaurantList from "@/components/restaurant-list";
import { Button } from "@/components/ui/button";
import type { RestaurantSummary } from "@/domain/domain";
import { errorMessage } from "@/lib/errors";
import { useAppContext } from "@/providers/app-context-provider";

function MyRestaurants() {
  const { apiService } = useAppContext();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [restaurants, setRestaurants] = useState<RestaurantSummary[]>([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);

  const load = useCallback(
    async (target: number) => {
      setLoading(true);
      try {
        const result = await apiService.getMyRestaurants(target, 8);
        setRestaurants(result.content);
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
    void load(1);
  }, [load]);

  return (
    <div className="max-w-[1200px] mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold">My restaurants</h1>
        <Button asChild>
          <Link href="/restaurants/create">Add restaurant</Link>
        </Button>
      </div>
      {error ? (
        <p role="alert" className="text-red-600">
          {error}
        </p>
      ) : (
        <RestaurantList loading={loading} restaurants={restaurants} emptyMessage="You have not added a restaurant yet" />
      )}
      <PageNav page={page} totalPages={totalPages} onChange={(p) => void load(p)} />
    </div>
  );
}

export default function MyRestaurantsPage() {
  return (
    <RequireRole role="RESTAURANT_OWNER">
      <MyRestaurants />
    </RequireRole>
  );
}
