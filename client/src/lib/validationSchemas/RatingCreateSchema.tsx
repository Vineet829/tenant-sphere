import * as z from "zod";

export const ratingCreateSchema = z.object({
	rated_user_username: z.string().trim(),
	rating: z
		.number({ required_error: "Choose a rating", invalid_type_error: "Choose a rating" })
		.nonnegative({ message: "A rating cannot be negative" })
		.min(1, "Choose a rating from 1 to 5")
		.max(5, "Choose a rating from 1 to 5"),
	comment: z.string().min(1, "Tell us a little about how the job went"),
});

export type TRatingCreateSchema = z.infer<typeof ratingCreateSchema>;
