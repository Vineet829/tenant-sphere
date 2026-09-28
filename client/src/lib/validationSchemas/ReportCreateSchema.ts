import * as z from "zod";

export const reportCreateSchema = z.object({
	title: z.string().trim().min(1, "Give the report a title"),
	description: z.string().min(1, "Describe what happened"),
	reported_user_username: z.string().min(1, "Enter the tenant's username. You can find it in Tenants."),
});

export type TReportCreateSchema = z.infer<typeof reportCreateSchema>;
