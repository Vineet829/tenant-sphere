import BookmarkedPostCard from "@/components/cards/BookmarkedPostCard";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Tenant Sphere | Bookmarks",
	description: "Authenticated users can see the posts they have bookmarked",
};


export default function BookmarkedPostsPage() {
	return (
		<>
			<BookmarkedPostCard />
		</>
	);
}
