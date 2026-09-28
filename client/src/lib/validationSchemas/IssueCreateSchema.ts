import * as z from "zod";

export const issueCreateSchema = z.object({
	title: z.string().trim().min(1, "Give the issue a title"),
	description: z.string().min(1, "Describe what is wrong"),
	priority: z.enum(["low", "medium", "high"], {
		required_error: "Choose a priority",
		invalid_type_error: "Choose a priority",
	}),
	status: z.enum(["reported", "resolved", "in_progress"], {
		required_error: "Choose a status",
		invalid_type_error: "Choose a status",
	}),
});

export type TIssueCreateSchema = z.infer<typeof issueCreateSchema>;
