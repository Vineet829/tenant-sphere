import * as z from "zod";

const usernameRegex = /^[a-zA-Z0-9_]+$/;


export const registerUserSchema = z
	.object({
		username: z.string().trim().min(1, "Enter a username").max(60, "Use 60 characters or fewer").regex(usernameRegex, { message: "Use letters, numbers, and underscores only" }),
		first_name: z
			.string()
			.trim()
			.min(1, { message: "Enter your first name" })
			.max(60, { message: "Use 60 characters or fewer" }),
		last_name: z
			.string()
			.trim()
			.min(1, { message: "Enter your last name" })
			.max(60, { message: "Use 60 characters or fewer" }),
		email: z.string().trim().email({ message: "Enter a valid email address" }).max(254, "Use 254 characters or fewer"),
		password: z
			.string()
			.min(8, { message: "Use at least 8 characters" })
			.regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/, { message: "Include uppercase and lowercase letters and a number" }),
		re_password: z.string().min(1, { message: "Confirm your password" }),
	})
	.refine((data) => data.password === data.re_password, {
		message: "Passwords do not match",
		path: ["re_password"],
	});

export type TRregisterUserSchema = z.infer<typeof registerUserSchema>;
