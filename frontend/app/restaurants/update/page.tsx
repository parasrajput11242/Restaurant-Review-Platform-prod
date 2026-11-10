"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { FormProvider, useForm } from "react-hook-form";
import { Card, CardContent } from "@/components/ui/card";
import RequireRole from "@/components/require-role";
import RestaurantForm, { emptyRestaurantForm, type RestaurantFormData } from "@/components/restaurant-form";
import { errorMessage } from "@/lib/errors";
import { useRoles } from "@/hooks/use-roles";
import { useAppContext } from "@/providers/app-context-provider";

function UpdateRestaurant() {
  const { apiService } = useAppContext();
  const { userId, isAdmin } = useRoles();
  const router = useRouter();
  const restaurantId = useSearchParams().get("id");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [allowed, setAllowed] = useState(true);
  const methods = useForm<RestaurantFormData>({ defaultValues: emptyRestaurantForm });
  const { reset } = methods;

  useEffect(() => {
    if (!restaurantId) {
      setError("Restaurant ID must be provided");
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const restaurant = await apiService.getRestaurant(restaurantId);
        if (cancelled) return;
        setAllowed(isAdmin || restaurant.createdBy?.id === userId);
        reset({
          name: restaurant.name,
          cuisineType: restaurant.cuisineType,
          contactInformation: restaurant.contactInformation ?? "",
          address: { ...restaurant.address, unit: restaurant.address.unit ?? "" },
          operatingHours: restaurant.operatingHours,
          photos: restaurant.photos?.map((p) => p.url) ?? [],
        });
      } catch (err) {
        if (!cancelled) setError(errorMessage(err, "Could not load the restaurant"));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [apiService, restaurantId, reset, userId, isAdmin]);

  const onSubmit = async (data: RestaurantFormData) => {
    if (!restaurantId) return;
    setError(undefined);
    try {
      await apiService.updateRestaurant(restaurantId, {
        name: data.name,
        cuisineType: data.cuisineType,
        contactInformation: data.contactInformation,
        address: data.address,
        operatingHours: data.operatingHours,
        photoIds: data.photos,
      });
      router.push(`/restaurants/${restaurantId}`);
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  if (loading) {
    return <p className="max-w-[800px] mx-auto px-4 py-8">Loading…</p>;
  }

  if (!allowed) {
    return <p className="max-w-[800px] mx-auto px-4 py-8">You can only edit your own restaurants.</p>;
  }

  return (
    <div className="max-w-[800px] mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Update Restaurant</h1>
      <Card>
        <CardContent className="pt-6">
          <FormProvider {...methods}>
            <form onSubmit={methods.handleSubmit(onSubmit)}>
              <RestaurantForm
                uploadPhoto={(file) => apiService.uploadPhoto(file)}
                submitLabel="Save Changes"
                submitting={methods.formState.isSubmitting}
                error={error}
              />
            </form>
          </FormProvider>
        </CardContent>
      </Card>
    </div>
  );
}

export default function UpdateRestaurantPage() {
  return (
    <RequireRole role="RESTAURANT_OWNER">
      <Suspense fallback={<p className="max-w-[800px] mx-auto px-4 py-8">Loading…</p>}>
        <UpdateRestaurant />
      </Suspense>
    </RequireRole>
  );
}
