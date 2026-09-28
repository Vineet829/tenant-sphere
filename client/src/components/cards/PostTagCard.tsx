"use client";

import EmptyState from "@/components/estate/EmptyState";
import ErrorState from "@/components/estate/ErrorState";
import PageHeader from "@/components/estate/PageHeader";
import PostListItem from "@/components/estate/PostListItem";
import { FeedSkeleton } from "@/components/estate/Skeletons";
import { tagSlug } from "@/components/estate/format";
import { useGetPopularTagsQuery, useGetPostsByTagQuery } from "@/lib/redux/features/posts/postApiSlice";
import { ArrowLeft, Hash } from "lucide-react";
import Link from "next/link";

interface SlugParamsProps {
	params: {
		tagSlug: string;
	};
}

function nameFromSlug(slug: string) {
	const text = decodeURIComponent(slug).replace(/-/g, " ");
	return text.charAt(0).toUpperCase() + text.slice(1);
}

export default function PostTagCard({ params }: SlugParamsProps) {
	const { data, isLoading, isFetching, isError, refetch } = useGetPostsByTagQuery(params.tagSlug);
	const { data: tagData } = useGetPopularTagsQuery();

	const posts = data?.posts_by_tag.results || [];
	const totalCount = data?.posts_by_tag.count ?? posts.length;
	const knownTag = tagData?.popular_tags.results.find((tag) => tag.slug === params.tagSlug);
	const usedTag = posts.flatMap((post) => post.tags ?? []).find((tag) => tagSlug(tag) === params.tagSlug);
	const tagName = knownTag?.name ?? usedTag ?? nameFromSlug(params.tagSlug);

	return (
		<div className="es-page es-page--feed">
			<Link href="/welcome" className="es-backlink">
				<ArrowLeft aria-hidden="true" />
				Back to community
			</Link>

			<PageHeader
				eyebrow="Tag"
				title={
					<>
						Posts tagged <em>{tagName}</em>
					</>
				}
				count={isLoading || isError ? undefined : totalCount}
			/>

			{isLoading ? (
				<FeedSkeleton rows={3} />
			) : isError ? (
				<ErrorState what="these posts" onRetry={refetch} retrying={isFetching} />
			) : posts.length > 0 ? (
				<div className="es-postlist es-stagger">
					{posts.map((post, index) => (
						<PostListItem key={post.id} post={post} index={index} />
					))}
				</div>
			) : (
				<EmptyState
					icon={Hash}
					description="Nobody has used this tag yet. Start the conversation and it will show up here."
					action={
						<Link href="/add-post" className="es-btn es-btn--primary">
							Create a post
						</Link>
					}
				>
					No posts with this tag
				</EmptyState>
			)}
		</div>
	);
}
