"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { useFormContext } from "react-hook-form";
import { Trash2 } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import type { OperatingHours, TimeRange } from "@/domain/domain";
import { errorMessage } from "@/lib/errors";

export interface RestaurantFormData {
  name: string;
  cuisineType: string;
  contactInformation: string;
  address: {
    streetNumber?: string;
    streetName: string;
    unit?: string;
    city: string;
    state: string;
    postalCode: string;
    country: string;
  };
  operatingHours: OperatingHours;
  photos: string[];
}

export const emptyRestaurantForm: RestaurantFormData = {
  name: "",
  cuisineType: "",
  contactInformation: "",
  address: { streetNumber: "", streetName: "", unit: "", city: "", state: "", postalCode: "", country: "" },
  operatingHours: {},
  photos: [],
};

const DAYS = ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"] as const;

interface RestaurantFormProps {
  uploadPhoto: (file: File) => Promise<{ url: string }>;
  submitLabel: string;
  submitting: boolean;
  error?: string;
}

export default function RestaurantForm({ uploadPhoto, submitLabel, submitting, error }: RestaurantFormProps) {
  const { register, setValue, getValues, watch } = useFormContext<RestaurantFormData>();
  const [uploadError, setUploadError] = useState<string>();
  const [uploading, setUploading] = useState(false);

  const photos = watch("photos") ?? [];
  const hours = watch("operatingHours") ?? {};

  // react-hook-form does not know about the dynamic hours/photos fields until they are registered
  useEffect(() => {
    register("photos");
    register("operatingHours");
  }, [register]);

  const handleFiles = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? []);
    e.target.value = "";
    if (files.length === 0) return;

    setUploading(true);
    setUploadError(undefined);
    try {
      const uploaded = await Promise.all(files.map((file) => uploadPhoto(file)));
      setValue("photos", [...(getValues("photos") ?? []), ...uploaded.map((p) => p.url)], { shouldDirty: true });
    } catch (err) {
      setUploadError(errorMessage(err, "Photo upload failed"));
    } finally {
      setUploading(false);
    }
  };

  const removePhoto = (index: number) => {
    setValue(
        "photos",
        (getValues("photos") ?? []).filter((_, i) => i !== index),
        { shouldDirty: true },
    );
  };

  const setHours = (day: (typeof DAYS)[number], field: keyof TimeRange, value: string) => {
    const current: TimeRange = getValues("operatingHours")?.[day] ?? { openTime: "", closeTime: "" };
    const next = { ...current, [field]: value };
    const updated: OperatingHours = {
      ...getValues("operatingHours"),
      [day]: !next.openTime && !next.closeTime ? null : next,
    };
    setValue("operatingHours", updated, { shouldDirty: true });
  };

  const field = (id: string, label: string, name: Parameters<typeof register>[0], required = true) => (
      <div>
        <Label htmlFor={id}>{label}</Label>
        <Input id={id} {...register(name, { required })} />
      </div>
  );

  return (
      <div className="space-y-8">
        <div>
          <h2 className="text-xl font-semibold mb-4">Basic Information</h2>
          <div className="space-y-4">
            {field("name", "Restaurant Name", "name")}
            {field("cuisineType", "Cuisine Type", "cuisineType")}
            {field("contactInformation", "Contact Information", "contactInformation")}
          </div>
        </div>

        <div>
          <h2 className="text-xl font-semibold mb-4">Address</h2>
          <p className="text-sm text-muted-foreground mb-4">
            We use this to place your restaurant on the map. No street number? Leave it empty and enter the road, sector or area name. If the exact spot cannot be found, the pin is placed in the nearest area.
          </p>
          <div className="space-y-4">
            {field("streetNumber", "Street / House Number (optional)", "address.streetNumber", false)}
            {field("streetName", "Street Name or Area", "address.streetName")}
            {field("unit", "Unit (Optional)", "address.unit", false)}
            {field("city", "City", "address.city")}
            {field("state", "State", "address.state")}
            {field("postalCode", "Postal Code", "address.postalCode")}
            {field("country", "Country", "address.country")}
          </div>
        </div>

        <div>
          <h2 className="text-xl font-semibold mb-4">Operating Hours</h2>
          <p className="text-sm text-muted-foreground mb-4">Leave both fields empty for days you are closed.</p>
          <div className="space-y-4">
            {DAYS.map((day) => (
                <div key={day} className="flex items-center gap-4">
                  <Label className="w-24 capitalize">{day}</Label>
                  <div className="flex-1 flex items-center gap-2">
                    <Input
                        type="time"
                        aria-label={`${day} opening time`}
                        className="w-[120px]"
                        value={hours[day]?.openTime ?? ""}
                        onChange={(e) => setHours(day, "openTime", e.target.value)}
                    />
                    <span>to</span>
                    <Input
                        type="time"
                        aria-label={`${day} closing time`}
                        className="w-[120px]"
                        value={hours[day]?.closeTime ?? ""}
                        onChange={(e) => setHours(day, "closeTime", e.target.value)}
                    />
                  </div>
                </div>
            ))}
          </div>
        </div>

        <div>
          <h2 className="text-xl font-semibold mb-4">Restaurant Photos</h2>
          <input
              id="photos"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              multiple
              onChange={handleFiles}
              className="hidden"
          />
          <Button
              type="button"
              variant="outline"
              disabled={uploading || photos.length >= 10}
              onClick={() => document.getElementById("photos")?.click()}
          >
            {uploading ? "Uploading…" : "Select Photos"}
          </Button>
          <p className="text-sm text-muted-foreground mt-2">JPEG, PNG or WebP, up to 5 MB each, 1 to 10 photos.</p>
          {uploadError && <p className="text-sm text-red-600 mt-2">{uploadError}</p>}

          {photos.length > 0 && (
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mt-4">
                {photos.map((photo, index) => (
                    <div key={photo} className="relative aspect-square group">
                      <Image src={`/api/photos/${photo}`} alt={`Photo ${index + 1}`} fill className="object-cover rounded-md" />
                      <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 focus-within:opacity-100 transition-opacity flex items-center justify-center rounded-md">
                        <button
                            type="button"
                            className="p-2 bg-red-500 rounded-full text-white hover:bg-red-600"
                            onClick={() => removePhoto(index)}
                            aria-label="Remove photo"
                        >
                          <Trash2 size={20} />
                        </button>
                      </div>
                    </div>
                ))}
              </div>
          )}
        </div>

        {error && (
            <p role="alert" className="text-red-600">
              {error}
            </p>
        )}

        <Button type="submit" className="w-full" disabled={submitting || uploading || photos.length === 0}>
          {submitting ? "Saving…" : submitLabel}
        </Button>
      </div>
  );
}