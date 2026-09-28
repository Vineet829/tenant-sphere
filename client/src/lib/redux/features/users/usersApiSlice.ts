import { baseApiSlice } from "@/lib/redux/features/api/baseApiSlice";
import {
	NonTenantResponse,
	ProfileData,
	ProfileResponse,
	ProfilePostsResponse,
	ProfileReviewsResponse,
	ProfilesResponse,
	QueryParams,
} from "@/types";

export const usersApiSlice = baseApiSlice.injectEndpoints({
	endpoints: (builder) => ({
		
		getAllUsers: builder.query<ProfilesResponse, QueryParams>({
			query: (params = {}) => {
				const queryString = new URLSearchParams();

				if (params.page) {
					queryString.append("page", params.page.toString());
				}
				if (params.searchTerm) {
					queryString.append("search", params.searchTerm);
				}
				return `/profiles/all/?${queryString.toString()}`;
			},
			providesTags: ["User"],
		}),

		getAllTechnicians: builder.query<NonTenantResponse, QueryParams>({
			query: (params = {}) => {
				const queryString = new URLSearchParams();

				if (params.page) {
					queryString.append("page", params.page.toString());
				}
				if (params.searchTerm) {
					queryString.append("search", params.searchTerm);
				}
				return `/profiles/non-tenant-profiles/?${queryString.toString()}`;
			},
			providesTags: ["User"],
		}),
		getUserProfile: builder.query<ProfileResponse, void>({
			query: () => "/profiles/user/my-profile/",
			providesTags: ["User"],
		}),
		getProfilePosts: builder.query<ProfilePostsResponse, { slug: string; page: number }>({
			query: ({ slug, page }) => `/profiles/${encodeURIComponent(slug)}/posts/?page=${page}`,
			providesTags: ["User", "Post"],
		}),
		getProfileReviews: builder.query<ProfileReviewsResponse, { slug: string; page: number }>({
			query: ({ slug, page }) => `/profiles/${encodeURIComponent(slug)}/reviews/?page=${page}`,
			providesTags: ["User"],
		}),
		updateUserProfile: builder.mutation<ProfileData, ProfileData>({
			query: (formData) => ({
				url: "/profiles/user/update/",
				method: "PATCH",
				body: formData,
			}),
			invalidatesTags: ["User"],
		}),
	}),
});

export const {
	useGetAllUsersQuery,
	useGetUserProfileQuery,
	useUpdateUserProfileMutation,
	useGetAllTechniciansQuery,
	useGetProfilePostsQuery,
	useGetProfileReviewsQuery,
} = usersApiSlice;
