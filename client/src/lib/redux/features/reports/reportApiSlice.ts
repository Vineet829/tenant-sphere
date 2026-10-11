import {
	MyReportsResponse,
	ReportTenantData,
	ReportTenantResponse,
} from "@/types";
import { baseApiSlice } from "../api/baseApiSlice";

export const reportApiSlice = baseApiSlice.injectEndpoints({
	endpoints: (builder) => ({
		reportTenant: builder.mutation<ReportTenantResponse, ReportTenantData>({
			query: (reportData) => ({
				url: "/reports/create/",
				method: "POST",
				body: reportData,
			}),
			invalidatesTags: ["Report"],
		}),

		getMyReports: builder.query<MyReportsResponse, number>({
			query: (page = 1) => `/reports/me/?page=${page}`,
			providesTags: ["Report"],
		}),
	}),
});


export const { useReportTenantMutation, useGetMyReportsQuery } = reportApiSlice;
