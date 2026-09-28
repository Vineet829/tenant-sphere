import * as z from "zod";

export const postUpdateSchema = z.object({
	title: z.string().trim().min(1, "Give your post a title"),
	body: z.string().min(1, "Write something to share"),
});

export type TPostUpdateSchema = z.infer<typeof postUpdateSchema>;
