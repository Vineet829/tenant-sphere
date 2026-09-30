import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { setAuth, setLogout } from "@/lib/redux/features/auth/authSlice";
import {
	BaseQueryFn,
	FetchArgs,
	FetchBaseQueryError,
} from "@reduxjs/toolkit/query";

import { Mutex } from "async-mutex";
import { getCookie } from "cookies-next";

const mutex = new Mutex();
const csrfMutex = new Mutex();
const csrfToken = () => getCookie(
	typeof window !== "undefined" && window.location.protocol === "https:" ? "__Host-XSRF-TOKEN" : "XSRF-TOKEN",
);

const baseQuery = fetchBaseQuery({
	baseUrl: "/api/v1",
	credentials: "include",
	prepareHeaders: (headers) => {
		const token = csrfToken();
		if (typeof token === "string") headers.set("X-XSRF-TOKEN", token);
		return headers;
	},
});


const securedQuery: BaseQueryFn<string | FetchArgs, unknown, FetchBaseQueryError> = async (args, api, extraOptions) => {
	const method = typeof args === "string" ? "GET" : args.method?.toUpperCase() ?? "GET";
	if (!["GET", "HEAD", "OPTIONS"].includes(method) && !csrfToken()) {
		const release = await csrfMutex.acquire();
		try {
			if (!csrfToken()) {
				const tokenResponse = await baseQuery("/auth/csrf/", api, extraOptions);
				if (tokenResponse.error) return tokenResponse;
			}
		} finally { release(); }
	}
	return baseQuery(args, api, extraOptions);
};

const baseQueryWithReauth: BaseQueryFn<
	string | FetchArgs,
	unknown,
	FetchBaseQueryError
> = async (args, api, extraOptions) => {
	await mutex.waitForUnlock();

	let response = await securedQuery(args, api, extraOptions);
	const url = typeof args === "string" ? args : args.url;
	const authRequest = url.startsWith("/auth/") && url !== "/auth/users/me/";

	if (!authRequest && response.error?.status === 401) {
		if (!mutex.isLocked()) {
			const release = await mutex.acquire();
			try {
				const refreshResponse = await securedQuery(
					{
						url: "/auth/refresh/",
						method: "POST",
					},
					api,
					extraOptions,
				);

				if (refreshResponse?.data) {
					api.dispatch(setAuth());
					response = await securedQuery(args, api, extraOptions);
				} else if (refreshResponse.error?.status === 401 || refreshResponse.error?.status === 403) {
					api.dispatch(setLogout());
				} else {
					// A temporary network/relay failure does not invalidate the user's session.
					response = refreshResponse;
				}
			} finally {
				release();
			}
		} else {
			await mutex.waitForUnlock();
			response = await securedQuery(args, api, extraOptions);
		}
	}
	return response;
};

export const baseApiSlice = createApi({
	reducerPath: "api",
	baseQuery: baseQueryWithReauth,
	tagTypes: ["User", "Apartment", "Issue", "Report", "Post"],
	refetchOnFocus: true,
	refetchOnMountOrArgChange: true,
	endpoints: (builder) => ({}),
});
