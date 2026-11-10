import Image from "next/image";
import Link from "next/link";
import { Card } from "@/components/ui/card";
import type { RestaurantSummary } from "@/domain/domain";
import Stars from "@/components/stars";

export default function RestaurantCard({ restaurant }: { restaurant: RestaurantSummary }) {
  const photo = restaurant.photos?.[0]?.url;

  return (
    <Link href={`/restaurants/${restaurant.id}`}>
      <Card className="overflow-hidden hover:shadow-lg transition-shadow">
        <div className="p-4">
          <p className="text-sm text-muted-foreground mb-4">
            {restaurant.address.city}, {restaurant.address.country}
          </p>
          <div className="aspect-[4/3] relative mb-4">
            <Image
              src={photo ? `/api/photos/${photo}` : "/placeholder.svg"}
              alt={restaurant.name}
              fill
              className="object-cover rounded-md"
            />
          </div>
          <h2 className="text-xl font-semibold mb-2">{restaurant.name}</h2>
          <div className="flex items-center gap-2 mb-2">
            <Stars value={restaurant.averageRating} />
            <span className="text-xs text-muted-foreground">({restaurant.totalReviews ?? 0})</span>
          </div>
          <p className="text-sm text-muted-foreground">{restaurant.cuisineType} Cuisine</p>
        </div>
      </Card>
    </Link>
  );
}
