"use client";

import { useState } from "react";
import { Search, Star } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import type { RestaurantSearchParams } from "@/domain/domain";

interface RestaurantSearchProps {
  searchRestaurants: (params: RestaurantSearchParams) => Promise<void>;
}

export default function RestaurantSearch({ searchRestaurants }: RestaurantSearchProps) {
  const [query, setQuery] = useState("");
  const [minRating, setMinRating] = useState<number | undefined>(undefined);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await searchRestaurants({ q: query, minRating });
  };

  const toggleMinRating = async (value: number) => {
    const next = minRating === value ? undefined : value;
    setMinRating(next);
    await searchRestaurants({ q: query, minRating: next });
  };

  return (
    <div>
      <form onSubmit={handleSubmit} className="flex gap-2 mb-4">
        <Input
          type="text"
          placeholder="Search for restaurants or cuisines..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="flex-grow"
        />
        <Button type="submit">
          <Search className="mr-2 h-4 w-4" /> Search
        </Button>
      </form>
      <div className="flex mb-8 gap-2">
        {[2, 3, 4].map((rating) => (
          <Button
            key={rating}
            type="button"
            onClick={() => void toggleMinRating(rating)}
            variant={minRating === rating ? "default" : "outline"}
          >
            <Star /> {rating}+
          </Button>
        ))}
      </div>
    </div>
  );
}
