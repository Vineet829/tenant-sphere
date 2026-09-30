import { baseApiSlice } from "@/lib/redux/features/api/baseApiSlice";
import {
	ActivateUserData,
	LoginResponse,
	LoginUserData,
	RegisterUserData,
	RegisterUserResponse,
	ResetPasswordConfirmData,
	ResetPasswordData,
	SocialAuthArgs,
	SocialAuthResponse,
	UserResponse,
} from "@/types";


export const authApiSlice = baseApiSlice.injectEndpoints({
	endpoints: (builder) => ({
		registerUser: builder.mutation<RegisterUserResponse, RegisterUserData>({
			query: (userData) => ({
				url: "/auth/users/",
				method: "POST",
				body: userData,
			}),
		}),
		loginGuest: builder.mutation<LoginResponse, void>({
			query: () => ({ url: "/auth/guest/", method: "POST" }),
			invalidatesTags: (result) => result ? ["User", "Post", "Apartment", "Issue", "Report"] : [],
		}),
		loginUser: builder.mutation<LoginResponse, LoginUserData>({
			query: (credentials) => ({
				url: "/auth/login/",
				method: "POST",
				body: credentials,
			}),
			invalidatesTags: (result) => result ? ["User", "Post", "Apartment", "Issue", "Report"] : [],
		}),
		activateUser: builder.mutation<void, ActivateUserData>({
			query: (credentials) => ({
				url: "/auth/users/activation/",
				method: "POST",
				body: credentials,
			}),
		}),
		resetPasswordRequest: builder.mutation<void, ResetPasswordData>({
			query: (formData) => ({
				url: "/auth/users/reset_password/",
				method: "POST",
				body: formData,
			}),
		}),
		resetPasswordConfirm: builder.mutation<void, ResetPasswordConfirmData>({
			query: (formData) => ({
				url: "/auth/users/reset_password_confirm/",
				method: "POST",
				body: formData,
			}),
		}),
		logoutUser: builder.mutation<void, void>({
			query: () => ({
				url: "/auth/logout/",
				method: "POST",
			}),
		}),
		refreshJWT: builder.mutation<void, void>({
			query: () => ({
				url: "/auth/refresh/",
				method: "POST",
			}),
		}),
		getUser: builder.query<UserResponse, void>({
			query: () => "/auth/users/me/",
			providesTags: ["User"],
		}),
		socialAuthentication: builder.mutation<SocialAuthResponse, SocialAuthArgs>({
			query: ({ provider, state, code }) => ({
				url: `/auth/o/${provider}/?state=${encodeURIComponent(state)}&code=${encodeURIComponent(code)}`,
				method: "POST",
				headers: {
					Accept: "application/json",
					"Content-Type": "application/x-www-form-urlencoded",
				},
			}),
			invalidatesTags: (result) => result ? ["User", "Post", "Apartment", "Issue", "Report"] : [],
		}),
	}),
});

export const {
	useSocialAuthenticationMutation,
	useActivateUserMutation,
	useLoginUserMutation,
	useLoginGuestMutation,
	useLogoutUserMutation,
	useGetUserQuery,
	useRegisterUserMutation,
	useResetPasswordConfirmMutation,
	useResetPasswordRequestMutation,
	useRefreshJWTMutation,
} = authApiSlice;
