import RestaurantCard from "@/components/restaurant-card";
import type { RestaurantSummary } from "@/domain/domain";
import { Donut } from "lucide-react";

interface RestaurantListProps {
  loading: boolean;
  restaurants: RestaurantSummary[];
  emptyMessage?: string;
}

export default function RestaurantList({ loading, restaurants, emptyMessage }: RestaurantListProps) {
  if (loading) {
    return <div>Loading restaurants...</div>;
  }

  if (restaurants.length === 0) {
    return (
      <div className="flex items-center justify-center min-h-[200px]">
        <div className="text-center">
          <div className="flex justify-center">
            <Donut size={100} className="opacity-[0.1]" />
          </div>
          <h3 className="mt-4 opacity-[0.3]">{emptyMessage ?? "No Restaurants Available"}</h3>
        </div>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
      {restaurants.map((restaurant) => (
        <RestaurantCard key={restaurant.id} restaurant={restaurant} />
      ))}
    </div>
  );
}
