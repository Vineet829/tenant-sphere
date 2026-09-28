"use client";

import EmptyState from "@/components/estate/EmptyState";
import ErrorState from "@/components/estate/ErrorState";
import PageHeader from "@/components/estate/PageHeader";
import PostListItem from "@/components/estate/PostListItem";
import { FeedSkeleton } from "@/components/estate/Skeletons";
import { useGetAllMyBookmarksQuery } from "@/lib/redux/features/posts/postApiSlice";
import { sortByDateDescending } from "@/utils";
import { Bookmark } from "lucide-react";
import Link from "next/link";

export default function BookmarkedPostCard() {
	const { data, isLoading, isFetching, isError, refetch } = useGetAllMyBookmarksQuery();
	const bookmarks = sortByDateDescending(data?.bookmarked_posts.results ?? [], "created_at");

	return (
		<div className="es-page es-page--feed">
			<PageHeader
				eyebrow="Your space"
				title={
					<>
						Saved <em>for later</em>
					</>
				}
				count={isLoading || isError ? undefined : bookmarks.length}
				description="Posts you bookmarked, so the good ones never get lost in the feed."
			/>

			{isLoading ? (
				<FeedSkeleton rows={3} />
			) : isError ? (
				<ErrorState what="your bookmarks" onRetry={refetch} retrying={isFetching} />
			) : bookmarks.length > 0 ? (
				<div className="es-postlist es-stagger">
					{bookmarks.map((post, index) => (
						<PostListItem key={post.id} post={post} index={index} showSaved={false} />
					))}
				</div>
			) : (
				<EmptyState
					icon={Bookmark}
					description="Open any post and choose Save. It will be waiting for you here."
					action={
						<Link href="/welcome" className="es-btn es-btn--primary">
							Browse the community
						</Link>
					}
				>
					Nothing saved yet
				</EmptyState>
			)}
		</div>
	);
}
