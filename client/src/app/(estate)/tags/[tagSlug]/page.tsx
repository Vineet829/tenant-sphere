import PostTagCard from "@/components/cards/PostTagCard";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Posts by tag — Tenant Sphere",
	description: "Authenticated users can see the tags details of a post",
};


interface SlugParamsProps {
	params: {
		tagSlug: string;
	};
}



export default async function TagPostsPage({ params }: { params: Promise<SlugParamsProps["params"]> }) {
	const resolved = await params;
	return (
		<>
			<PostTagCard params={resolved} />
		</>
	);
}
