import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import BookmarkedPostCard from "@/components/cards/BookmarkedPostCard";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Bookmarks — Tenant Sphere",
	description: "Authenticated users can see the posts they have bookmarked",
};


function BookmarkedPostsPage() {
	return (
		<>
			<BookmarkedPostCard />
		</>
	);
}

export default function ProtectedPage() {
	return <ProtectedRoute><BookmarkedPostsPage /></ProtectedRoute>;
}
