"use client";

import EmptyState from "@/components/estate/EmptyState";
import ErrorState from "@/components/estate/ErrorState";
import PostListItem from "@/components/estate/PostListItem";
import { CardsSkeleton } from "@/components/estate/Skeletons";
import { useGetMyPostsQuery } from "@/lib/redux/features/posts/postApiSlice";
import { MessagesSquare, PenLine } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import PageControls from "../shared/PageControls";

export default function MyPostCard() {
	const [page, setPage] = useState(1);
	const { currentData: data, isLoading, isFetching, isError, refetch } = useGetMyPostsQuery(page);
	const totalPages = data?.my_posts.total_pages ?? page;
	useEffect(() => {
		if (data && page > Math.max(totalPages, 1)) setPage(Math.max(totalPages, 1));
	}, [data, page, totalPages]);

	if (isLoading || (!data && isFetching)) return <CardsSkeleton cards={2} />;
	if (isError) return <ErrorState what="your posts" onRetry={refetch} retrying={isFetching} />;

	const posts = data?.my_posts.results || [];

	if (posts.length === 0) {
		return (
			<EmptyState
				icon={MessagesSquare}
				description="Questions, notices and recommendations you share with the building are kept here."
				action={
					<Link href="/add-post" className="es-btn es-btn--primary">
						<PenLine aria-hidden="true" />
						Create a post
					</Link>
				}
			>
				You have not posted yet
			</EmptyState>
		);
	}

	return (
		<>
			<div className="es-postgrid es-stagger">
				{posts.map((post, index) => (
					<PostListItem key={post.id} post={post} index={index} showAuthor={false} />
				))}
			</div>
			<PageControls page={page} totalPages={totalPages} onChange={setPage} pending={isFetching} />
		</>
	);
}
