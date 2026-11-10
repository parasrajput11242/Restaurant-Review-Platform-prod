import type {
  OwnerRequest,
  OwnerRequestStatus,
  PaginatedResponse,
  Photo,
  Restaurant,
  RestaurantRequest,
  RestaurantSearchParams,
  RestaurantSummary,
  Review,
  ReviewRequest,
  ReviewSort,
} from "@/domain/domain";

export interface ApiService {
  // Restaurants
  searchRestaurants(params: RestaurantSearchParams): Promise<PaginatedResponse<RestaurantSummary>>;
  getMyRestaurants(page?: number, size?: number): Promise<PaginatedResponse<RestaurantSummary>>;
  getRestaurant(restaurantId: string): Promise<Restaurant>;
  createRestaurant(request: RestaurantRequest): Promise<Restaurant>;
  updateRestaurant(restaurantId: string, request: RestaurantRequest): Promise<Restaurant>;
  deleteRestaurant(restaurantId: string): Promise<void>;

  // Reviews
  getRestaurantReviews(
    restaurantId: string,
    sort?: ReviewSort,
    page?: number,
    size?: number,
  ): Promise<PaginatedResponse<Review>>;
  getRestaurantReview(restaurantId: string, reviewId: string): Promise<Review>;
  createReview(restaurantId: string, request: ReviewRequest): Promise<Review>;
  updateReview(restaurantId: string, reviewId: string, request: ReviewRequest): Promise<Review>;
  deleteReview(restaurantId: string, reviewId: string): Promise<void>;

  // Photos
  uploadPhoto(file: File): Promise<Photo>;

  // Becoming an owner
  submitOwnerRequest(message: string): Promise<OwnerRequest>;
  getMyOwnerRequest(): Promise<OwnerRequest | null>;

  // Admin
  listOwnerRequests(
    status: OwnerRequestStatus,
    page?: number,
    size?: number,
  ): Promise<PaginatedResponse<OwnerRequest>>;
  approveOwnerRequest(id: string): Promise<OwnerRequest>;
  rejectOwnerRequest(id: string, reason?: string): Promise<OwnerRequest>;
}
