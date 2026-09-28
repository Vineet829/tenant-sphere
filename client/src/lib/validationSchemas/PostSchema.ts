import * as z from "zod";

export const postSchema = z.object({
	title: z.string().trim().min(1, "Give your post a title"),
	tags: z.array(z.string()).min(1, "Add at least one tag"),
	body: z.string().min(1, "Write something to share"),
});

export type TPostSchema = z.infer<typeof postSchema>;
