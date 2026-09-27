import * as z from "zod";

export const loginUserSchema = z.object({
	email: z.string().trim().email({ message: "Enter a valid email address" }),
	password: z
		.string()
		.min(1, { message: "Enter your password" }),
});


export type TLoginUserSchema = z.infer<typeof loginUserSchema>;
