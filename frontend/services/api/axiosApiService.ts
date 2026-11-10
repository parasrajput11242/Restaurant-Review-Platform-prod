import axios, { AxiosHeaders, type AxiosInstance } from "axios";
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
import type { ApiService } from "./apiService";

/** What the API client needs to know about the signed-in session. */
export interface Session {
  accessToken?: string;
  expired: boolean;
  /** Gets a fresh access token with the refresh token; resolves to it, or undefined on failure. */
  renew: () => Promise<string | undefined>;
}

export class AxiosApiService implements ApiService {
  private readonly api: AxiosInstance;

  /**
   * @param baseUrl     API root, e.g. "/api"
   * @param getSession  returns the current session. It is called on every request, so this
   *                    service can be created once and never goes stale.
   */
  constructor(baseUrl: string, private readonly getSession: () => Session) {
    this.api = axios.create({ baseURL: baseUrl });

    this.api.interceptors.request.use(async (config) => {
      const token = await this.currentAccessToken();
      if (token) {
        const headers = AxiosHeaders.from(config.headers);
        headers.setAuthorization(`Bearer ${token}`);
        config.headers = headers;
      }
      return config;
    });
  }

  private async currentAccessToken(): Promise<string | undefined> {
    const session = this.getSession();
    if (!session.accessToken) return undefined;
    if (!session.expired) return session.accessToken;
    // Expired (e.g. the tab slept): use the refresh token to get a new one
    return session.renew();
  }

  // ---- restaurants ----
  async searchRestaurants(params: RestaurantSearchParams) {
    const { data } = await this.api.get<PaginatedResponse<RestaurantSummary>>("/restaurants", { params });
    return data;
  }

  async getMyRestaurants(page = 1, size = 20) {
    const { data } = await this.api.get<PaginatedResponse<RestaurantSummary>>("/restaurants/mine", {
      params: { page, size },
    });
    return data;
  }

  async getRestaurant(restaurantId: string) {
    const { data } = await this.api.get<Restaurant>(`/restaurants/${restaurantId}`);
    return data;
  }

  async createRestaurant(request: RestaurantRequest) {
    const { data } = await this.api.post<Restaurant>("/restaurants", request);
    return data;
  }

  async updateRestaurant(restaurantId: string, request: RestaurantRequest) {
    const { data } = await this.api.put<Restaurant>(`/restaurants/${restaurantId}`, request);
    return data;
  }

  async deleteRestaurant(restaurantId: string) {
    await this.api.delete(`/restaurants/${restaurantId}`);
  }

  // ---- reviews ----
  async getRestaurantReviews(restaurantId: string, sort?: ReviewSort, page?: number, size?: number) {
    const { data } = await this.api.get<PaginatedResponse<Review>>(`/restaurants/${restaurantId}/reviews`, {
      params: { sort, page, size },
    });
    return data;
  }

  async getRestaurantReview(restaurantId: string, reviewId: string) {
    const { data } = await this.api.get<Review>(`/restaurants/${restaurantId}/reviews/${reviewId}`);
    return data;
  }

  async createReview(restaurantId: string, request: ReviewRequest) {
    const { data } = await this.api.post<Review>(`/restaurants/${restaurantId}/reviews`, request);
    return data;
  }

  async updateReview(restaurantId: string, reviewId: string, request: ReviewRequest) {
    const { data } = await this.api.put<Review>(`/restaurants/${restaurantId}/reviews/${reviewId}`, request);
    return data;
  }

  async deleteReview(restaurantId: string, reviewId: string) {
    await this.api.delete(`/restaurants/${restaurantId}/reviews/${reviewId}`);
  }

  // ---- photos ----
  async uploadPhoto(file: File): Promise<Photo> {
    const formData = new FormData();
    formData.append("file", file);
    const { data } = await this.api.post<Photo>("/photos", formData);
    return data;
  }

  // ---- owner requests ----
  async submitOwnerRequest(message: string) {
    const { data } = await this.api.post<OwnerRequest>("/owner-requests", { message });
    return data;
  }

  async getMyOwnerRequest(): Promise<OwnerRequest | null> {
    const response = await this.api.get<OwnerRequest | "">("/owner-requests/me");
    return response.status === 204 || response.data === "" ? null : response.data;
  }

  async listOwnerRequests(status: OwnerRequestStatus, page = 1, size = 20) {
    const { data } = await this.api.get<PaginatedResponse<OwnerRequest>>("/admin/owner-requests", {
      params: { status, page, size },
    });
    return data;
  }

  async approveOwnerRequest(id: string) {
    const { data } = await this.api.post<OwnerRequest>(`/admin/owner-requests/${id}/approve`);
    return data;
  }

  async rejectOwnerRequest(id: string, reason?: string) {
    const { data } = await this.api.post<OwnerRequest>(`/admin/owner-requests/${id}/reject`, { reason });
    return data;
  }
}
