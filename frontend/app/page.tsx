"use client";

import { useCallback, useEffect, useState } from "react";
import Hero from "@/components/hero";
import PageNav from "@/components/page-nav";
import RestaurantList from "@/components/restaurant-list";
import RestaurantSearch from "@/components/restaurant-search";
import type { RestaurantSearchParams, RestaurantSummary } from "@/domain/domain";
import { useAppContext } from "@/providers/app-context-provider";

const PAGE_SIZE = 8;

export default function Home() {
  const { apiService } = useAppContext();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [restaurants, setRestaurants] = useState<RestaurantSummary[]>([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [params, setParams] = useState<RestaurantSearchParams>({});

  const load = useCallback(
    async (searchParams: RestaurantSearchParams, targetPage: number) => {
      setLoading(true);
      setError(false);
      try {
        const result = await apiService.searchRestaurants({ ...searchParams, page: targetPage, size: PAGE_SIZE });
        setRestaurants(result.content);
        setTotalPages(result.totalPages);
        setPage(result.page);
        setParams(searchParams);
      } catch {
        setError(true);
      } finally {
        setLoading(false);
      }
    },
    [apiService],
  );

  useEffect(() => {
    void load({}, 1);
  }, [load]);

  return (
    <div>
      <Hero />
      <main className="max-w-[1200px] mx-auto px-4 py-8">
        <RestaurantSearch searchRestaurants={(p) => load(p, 1)} />
        {error ? (
          <p className="text-red-600">Could not load restaurants. Please try again in a moment.</p>
        ) : (
          <RestaurantList loading={loading} restaurants={restaurants} />
        )}
        <PageNav page={page} totalPages={totalPages} onChange={(p) => void load(params, p)} />
      </main>
    </div>
  );
}
