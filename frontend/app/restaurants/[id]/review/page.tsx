"use client";

import { Suspense, useEffect, useState } from "react";
import Image from "next/image";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { Star, Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import RequireRole from "@/components/require-role";
import { errorMessage } from "@/lib/errors";
import { useAppContext } from "@/providers/app-context-provider";

const MAX_PHOTOS = 8;

function ReviewForm() {
  const { id: restaurantId } = useParams<{ id: string }>();
  const reviewId = useSearchParams().get("reviewId");
  const isEditing = !!reviewId;
  const router = useRouter();
  const { apiService } = useAppContext();

  const [rating, setRating] = useState(0);
  const [hoveredRating, setHoveredRating] = useState(0);
  const [content, setContent] = useState("");
  const [photoIds, setPhotoIds] = useState<string[]>([]);
  const [uploading, setUploading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string>();

  useEffect(() => {
    if (!reviewId) return;
    let cancelled = false;
    apiService
      .getRestaurantReview(restaurantId, reviewId)
      .then((review) => {
        if (cancelled) return;
        setRating(review.rating);
        setContent(review.content);
        setPhotoIds(review.photos?.map((p) => p.url) ?? []);
      })
      .catch((err) => !cancelled && setError(errorMessage(err, "Could not load the review")));
    return () => {
      cancelled = true;
    };
  }, [apiService, restaurantId, reviewId]);

  const handleFiles = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? []).slice(0, MAX_PHOTOS - photoIds.length);
    e.target.value = "";
    if (files.length === 0) return;
    setUploading(true);
    setError(undefined);
    try {
      const uploaded = await Promise.all(files.map((file) => apiService.uploadPhoto(file)));
      setPhotoIds((current) => [...current, ...uploaded.map((p) => p.url)]);
    } catch (err) {
      setError(errorMessage(err, "Photo upload failed"));
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(undefined);
    try {
      const body = { content, rating, photoIds };
      if (reviewId) {
        await apiService.updateReview(restaurantId, reviewId, body);
      } else {
        await apiService.createReview(restaurantId, body);
      }
      router.push(`/restaurants/${restaurantId}`);
    } catch (err) {
      setError(errorMessage(err, "Failed to submit review. Please try again."));
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-[800px] mx-auto px-4 py-8">
      <Card>
        <CardHeader>
          <CardTitle>{isEditing ? "Edit Review" : "Write a Review"}</CardTitle>
          <CardDescription>Share your dining experience at this restaurant</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-2">
              <Label>Your Rating</Label>
              <div className="flex gap-1">
                {Array.from({ length: 5 }).map((_, i) => (
                  <button
                    key={i}
                    type="button"
                    aria-label={`${i + 1} star${i === 0 ? "" : "s"}`}
                    onClick={() => setRating(i + 1)}
                    onMouseEnter={() => setHoveredRating(i + 1)}
                    onMouseLeave={() => setHoveredRating(0)}
                    className="focus:outline-none"
                  >
                    <Star
                      className={`w-8 h-8 ${
                        i < (hoveredRating || rating) ? "text-yellow-400 fill-yellow-400" : "text-gray-300"
                      }`}
                    />
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="review">Your Review</Label>
              <Textarea
                id="review"
                placeholder="Tell us about your experience at this restaurant..."
                value={content}
                onChange={(e) => setContent(e.target.value)}
                className="min-h-[200px]"
                maxLength={4000}
                required
              />
            </div>

            <div className="space-y-2">
              <Label>Photos</Label>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                {photoIds.map((photo, index) => (
                  <div key={photo} className="relative aspect-square group">
                    <Image src={`/api/photos/${photo}`} alt={`Photo ${index + 1}`} fill className="object-cover rounded-lg" />
                    <button
                      type="button"
                      aria-label="Remove photo"
                      onClick={() => setPhotoIds((current) => current.filter((_, i) => i !== index))}
                      className="absolute top-2 right-2 bg-red-500 text-white rounded-full w-6 h-6 opacity-0 group-hover:opacity-100 focus:opacity-100 transition-opacity"
                    >
                      ×
                    </button>
                  </div>
                ))}
                {photoIds.length < MAX_PHOTOS && (
                  <label className="border-2 border-dashed rounded-lg aspect-square flex flex-col items-center justify-center cursor-pointer hover:border-primary transition-colors">
                    <Upload className="w-8 h-8 mb-2 text-muted-foreground" />
                    <span className="text-sm text-muted-foreground">{uploading ? "Uploading…" : "Upload photos"}</span>
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      multiple
                      onChange={handleFiles}
                      disabled={uploading}
                      className="hidden"
                    />
                  </label>
                )}
              </div>
              <p className="text-sm text-muted-foreground">Up to {MAX_PHOTOS} photos (JPEG, PNG or WebP, 5 MB each)</p>
            </div>

            {error && (
              <p role="alert" className="text-red-600">
                {error}
              </p>
            )}

            <div className="flex gap-4">
              <Button type="button" variant="outline" onClick={() => router.push(`/restaurants/${restaurantId}`)}>
                Cancel
              </Button>
              <Button type="submit" disabled={!rating || !content.trim() || submitting || uploading}>
                {submitting ? "Saving…" : isEditing ? "Update Review" : "Submit Review"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

export default function WriteReviewPage() {
  return (
    <RequireRole role="USER">
      <Suspense fallback={<p className="max-w-[800px] mx-auto px-4 py-8">Loading…</p>}>
        <ReviewForm />
      </Suspense>
    </RequireRole>
  );
}
