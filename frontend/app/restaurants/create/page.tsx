"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { FormProvider, useForm } from "react-hook-form";
import { Card, CardContent } from "@/components/ui/card";
import RequireRole from "@/components/require-role";
import RestaurantForm, { emptyRestaurantForm, type RestaurantFormData } from "@/components/restaurant-form";
import { errorMessage } from "@/lib/errors";
import { useAppContext } from "@/providers/app-context-provider";

export default function CreateRestaurantPage() {
  const { apiService } = useAppContext();
  const router = useRouter();
  const [error, setError] = useState<string>();
  const methods = useForm<RestaurantFormData>({ defaultValues: emptyRestaurantForm });

  const onSubmit = async (data: RestaurantFormData) => {
    setError(undefined);
    try {
      const created = await apiService.createRestaurant({
        name: data.name,
        cuisineType: data.cuisineType,
        contactInformation: data.contactInformation,
        address: data.address,
        operatingHours: data.operatingHours,
        photoIds: data.photos,
      });
      router.push(`/restaurants/${created.id}`);
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <RequireRole role="RESTAURANT_OWNER">
      <div className="max-w-[800px] mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold mb-6">Create a New Restaurant</h1>
        <Card>
          <CardContent className="pt-6">
            <FormProvider {...methods}>
              <form onSubmit={methods.handleSubmit(onSubmit)}>
                <RestaurantForm
                  uploadPhoto={(file) => apiService.uploadPhoto(file)}
                  submitLabel="Create Restaurant"
                  submitting={methods.formState.isSubmitting}
                  error={error}
                />
              </form>
            </FormProvider>
          </CardContent>
        </Card>
      </div>
    </RequireRole>
  );
}
