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
import { useEffect, useState } from "react";
import PageControls from "../shared/PageControls";

export default function BookmarkedPostCard() {
	const [page, setPage] = useState(1);
	const { currentData: data, isLoading, isFetching, isError, refetch } = useGetAllMyBookmarksQuery(page);
	const totalPages = data?.bookmarked_posts.total_pages ?? page;
	useEffect(() => {
		if (data && page > Math.max(totalPages, 1)) setPage(Math.max(totalPages, 1));
	}, [data, page, totalPages]);
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
				count={!data || isError ? undefined : data.bookmarked_posts.count}
				description="Posts you bookmarked, so the good ones never get lost in the feed."
			/>

			{isLoading || (!data && isFetching) ? (
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
			<PageControls page={page} totalPages={totalPages} onChange={setPage} pending={isFetching} />
		</div>
	);
}
