"use client";

import { useCallback, useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "react-oidc-context";
import { ArrowUpDown, ChevronLeft, Dot, MessageSquare, PenLine, SquarePen, Trash2 } from "lucide-react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import OpenStreetMap from "@/components/open-street-maps";
import RestaurantList from "@/components/restaurant-list";
import Stars from "@/components/stars";
import type { Restaurant, RestaurantSummary, Review, ReviewSort } from "@/domain/domain";
import { useRoles } from "@/hooks/use-roles";
import { parseServerDate } from "@/lib/auth";
import { errorMessage } from "@/lib/errors";
import { useAppContext } from "@/providers/app-context-provider";

const SORT_LABELS: Record<ReviewSort, string> = {
  "datePosted,desc": "Newest first",
  "datePosted,asc": "Oldest first",
  "rating,desc": "Highest rating",
  "rating,asc": "Lowest rating",
};

type DeleteTarget = { type: "restaurant" } | { type: "review"; review: Review };

export default function RestaurantPage() {
  const { id } = useParams<{ id: string }>();
  const { apiService } = useAppContext();
  const auth = useAuth();
  const { isAuthenticated, isUser, isAdmin, userId } = useRoles();
  const router = useRouter();

  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [restaurant, setRestaurant] = useState<Restaurant | null>(null);
  const [nearby, setNearby] = useState<RestaurantSummary[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [sort, setSort] = useState<ReviewSort>("datePosted,desc");
  const [deleteTarget, setDeleteTarget] = useState<DeleteTarget | null>(null);
  const [actionError, setActionError] = useState<string>();

  const loadReviews = useCallback(
    async (order: ReviewSort) => {
      const result = await apiService.getRestaurantReviews(id, order);
      setReviews(result.content);
    },
    [apiService, id],
  );

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await apiService.getRestaurant(id);
        if (cancelled) return;
        setRestaurant(data);
        setReviews(data.reviews ?? []);

        if (data.geoLocation) {
          const near = await apiService.searchRestaurants({
            latitude: data.geoLocation.latitude,
            longitude: data.geoLocation.longitude,
            radius: 20,
          });
          if (!cancelled) setNearby(near.content.filter((r) => r.id !== data.id).slice(0, 4));
        }
      } catch (err) {
        if (!cancelled) {
          setNotFound(true);
          console.error("Could not load restaurant", err);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [apiService, id]);

  const changeSort = async (order: ReviewSort) => {
    setSort(order);
    try {
      await loadReviews(order);
    } catch (err) {
      setActionError(errorMessage(err));
    }
  };

  if (loading) {
    return <p className="max-w-[1200px] mx-auto px-4 py-8">Loading…</p>;
  }
  if (notFound || !restaurant) {
    return (
      <div className="max-w-[1200px] mx-auto px-4 py-8">
        <p className="mb-4">We could not find that restaurant.</p>
        <Link href="/" className="underline">
          Back to all restaurants
        </Link>
      </div>
    );
  }

  const isRestaurantOwner = !!userId && restaurant.createdBy?.id === userId;
  const canManageRestaurant = isRestaurantOwner || isAdmin;
  const alreadyReviewed = !!userId && reviews.some((r) => r.writtenBy?.id === userId);
  const canReview = isUser && !isRestaurantOwner && !alreadyReviewed;

  const handleWriteReview = () => {
    if (!isAuthenticated) {
      void auth.signinRedirect();
    } else {
      router.push(`/restaurants/${restaurant.id}/review`);
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    setActionError(undefined);
    try {
      if (deleteTarget.type === "restaurant") {
        await apiService.deleteRestaurant(restaurant.id);
        router.push("/");
        return;
      }
      await apiService.deleteReview(restaurant.id, deleteTarget.review.id);
      const updated = await apiService.getRestaurant(restaurant.id);
      setRestaurant(updated);
      await loadReviews(sort);
    } catch (err) {
      setActionError(errorMessage(err));
    } finally {
      setDeleteTarget(null);
    }
  };

  const heroPhoto = restaurant.photos?.[0]?.url;

  return (
    <div className="max-w-[1200px] mx-auto px-4 py-8">
      <div className="flex mb-10 opacity-[0.6] text-[14px]">
        <ChevronLeft size={20} />
        <Link href="/">Back</Link>
      </div>

      <div className="grid md:grid-cols-2 gap-8 mb-8">
        <div>
          <div className="flex justify-between gap-4">
            <h1 className="text-4xl font-bold mb-2">{restaurant.name}</h1>
            {canManageRestaurant && (
              <div className="flex gap-2 text-black/60">
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label="Edit restaurant"
                  onClick={() => router.push(`/restaurants/update?id=${restaurant.id}`)}
                >
                  <PenLine />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label="Delete restaurant"
                  onClick={() => setDeleteTarget({ type: "restaurant" })}
                >
                  <Trash2 />
                </Button>
              </div>
            )}
          </div>

          <p className="text-muted-foreground mb-2 flex flex-wrap items-center">
            {restaurant.address.city}, {restaurant.address.state} <Dot /> {restaurant.cuisineType} Cuisine <Dot />{" "}
            {restaurant.contactInformation}
          </p>
          <div className="flex items-center gap-2 mb-4">
            <Stars value={restaurant.averageRating} size={5} />
            <span className="text-muted-foreground">({restaurant.totalReviews ?? 0} reviews)</span>
          </div>

          {!isAuthenticated || canReview ? (
            <Button size="lg" className="w-full md:w-auto" onClick={handleWriteReview}>
              Write a review
            </Button>
          ) : (
            <p className="text-sm text-muted-foreground">
              {isRestaurantOwner
                ? "This is your restaurant, so you cannot review it."
                : alreadyReviewed
                  ? "You have already reviewed this restaurant. You can edit your review below."
                  : null}
            </p>
          )}

          <div className="mt-4">{restaurant.geoLocation && <OpenStreetMap location={restaurant.geoLocation} />}</div>
        </div>
        <div className="aspect-[4/3] relative">
          <Image
            src={heroPhoto ? `/api/photos/${heroPhoto}` : "/placeholder.svg"}
            alt={restaurant.name}
            fill
            className="object-cover rounded-lg"
          />
        </div>
      </div>

      {actionError && (
        <p role="alert" className="text-red-600 mb-4">
          {actionError}
        </p>
      )}

      {nearby.length > 0 && (
        <div className="space-y-6 mb-16">
          <h2 className="text-2xl font-semibold">Restaurants Nearby</h2>
          <RestaurantList loading={false} restaurants={nearby} />
        </div>
      )}

      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <h2 className="text-2xl font-semibold">Reviews</h2>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="sm" className="flex items-center gap-2">
                <ArrowUpDown className="h-4 w-4" />
                {SORT_LABELS[sort]}
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              {(Object.keys(SORT_LABELS) as ReviewSort[]).map((order) => (
                <DropdownMenuItem key={order} onClick={() => void changeSort(order)}>
                  {SORT_LABELS[order]}
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        {reviews.length > 0 ? (
          reviews.map((review) => {
            const isAuthor = !!userId && review.writtenBy?.id === userId;
            return (
              <div key={review.id} className="border-b pb-6">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex gap-2">
                    <p className="text-sm">{review.writtenBy?.username}</p>
                    <p className="text-sm text-muted-foreground">
                      {parseServerDate(review.datePosted).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    {isAuthor && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => router.push(`/restaurants/${restaurant.id}/review?reviewId=${review.id}`)}
                      >
                        <SquarePen className="h-4 w-4" /> Edit
                      </Button>
                    )}
                    {(isAuthor || isAdmin) && (
                      <Button variant="ghost" size="sm" onClick={() => setDeleteTarget({ type: "review", review })}>
                        <Trash2 className="h-4 w-4" /> {isAuthor ? "Delete" : "Remove"}
                      </Button>
                    )}
                  </div>
                </div>
                <div className="mb-2">
                  <Stars value={review.rating} />
                </div>
                <p className="text-gray-600 whitespace-pre-line">{review.content}</p>
                {review.photos && review.photos.length > 0 && (
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-4">
                    {review.photos.map((photo) => (
                      <div key={photo.url} className="relative aspect-square">
                        <Image src={`/api/photos/${photo.url}`} alt="Review photo" fill className="object-cover rounded-lg" />
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })
        ) : (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <MessageSquare className="w-12 h-12 text-gray-400 mb-4" />
            <p className="text-lg text-gray-600 mb-2">No reviews yet</p>
          </div>
        )}
      </div>

      <AlertDialog open={deleteTarget !== null} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure you want to delete this {deleteTarget?.type}?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. This will permanently delete the {deleteTarget?.type}.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={() => void confirmDelete()}>Delete</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
