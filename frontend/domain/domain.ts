// Shapes returned by the Spring Boot API.

export type Role = "ADMIN" | "RESTAURANT_OWNER" | "USER";

export interface UserSummary {
  id: string;
  username: string;
  givenName?: string;
  familyName?: string;
}

export interface Address {
  streetNumber?: string;
  streetName: string;
  unit?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export interface GeoLocation {
  latitude: number;
  longitude: number;
}

export interface TimeRange {
  openTime: string;
  closeTime: string;
}

export interface OperatingHours {
  monday?: TimeRange | null;
  tuesday?: TimeRange | null;
  wednesday?: TimeRange | null;
  thursday?: TimeRange | null;
  friday?: TimeRange | null;
  saturday?: TimeRange | null;
  sunday?: TimeRange | null;
}

export interface Photo {
  url: string;
  uploadDate: string;
}

export interface Review {
  id: string;
  content: string;
  rating: number;
  datePosted: string;
  lastEdited?: string;
  photos?: Photo[];
  writtenBy: UserSummary;
}

export interface Restaurant {
  id: string;
  name: string;
  cuisineType: string;
  contactInformation?: string;
  averageRating?: number;
  totalReviews?: number;
  reviews?: Review[];
  address: Address;
  geoLocation?: GeoLocation;
  operatingHours: OperatingHours;
  photos?: Photo[];
  createdBy?: UserSummary;
}

export interface RestaurantSummary {
  id: string;
  name: string;
  cuisineType: string;
  averageRating?: number;
  totalReviews?: number;
  address: Address;
  photos?: Photo[];
}

/** Paged result. `page` is 1-based. */
export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export type OwnerRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface OwnerRequest {
  id: string;
  username: string;
  email?: string;
  message: string;
  status: OwnerRequestStatus;
  createdAt: string;
  reviewedBy?: string;
  reviewedAt?: string;
  rejectionReason?: string;
}

export interface RestaurantRequest {
  name: string;
  cuisineType: string;
  contactInformation: string;
  address: Address;
  operatingHours: OperatingHours;
  photoIds: string[];
}

export interface ReviewRequest {
  content: string;
  rating: number;
  photoIds?: string[];
}

export type ReviewSort =
    | "datePosted,desc"
    | "datePosted,asc"
    | "rating,desc"
    | "rating,asc";

export interface RestaurantSearchParams {
  q?: string;
  minRating?: number;
  latitude?: number;
  longitude?: number;
  radius?: number;
  page?: number;
  size?: number;
}