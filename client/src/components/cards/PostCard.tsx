"use client";

import EmptyState from "@/components/estate/EmptyState";
import ErrorState from "@/components/estate/ErrorState";
import PageHeader from "@/components/estate/PageHeader";
import PersonAvatar from "@/components/estate/PersonAvatar";
import PostListItem from "@/components/estate/PostListItem";
import { FeedSkeleton } from "@/components/estate/Skeletons";
import { useUserProfile } from "@/hooks/useUseProfile";
import { useGetAllPostsQuery } from "@/lib/redux/features/posts/postApiSlice";
import { useAppSelector } from "@/lib/redux/hooks/typedHooks";
import { PostState } from "@/types";
import { sortByDateDescending } from "@/utils";
import { MessagesSquare, PenLine } from "lucide-react";
import Link from "next/link";
import PaginationSection from "../shared/PaginationSection";

const PAGE_SIZE = 9;

export default function PostCard() {
	const page = useAppSelector((state: PostState) => state.post.page);
	const { data, isLoading, isFetching, isError, refetch } = useGetAllPostsQuery({ page });
	const ready = !isLoading && !isError;
	const { profile } = useUserProfile();

	const totalCount = data?.posts.count || 0;
	const totalPages = Math.ceil(totalCount / PAGE_SIZE);
	const posts = sortByDateDescending(data?.posts.results ?? [], "created_at");

	return (
		<div className="es-page es-page--feed">
			<PageHeader
				compact
				eyebrow="Community"
				title={
					<>
						What&apos;s happening <em>at home</em>
					</>
				}
				count={ready ? totalCount : undefined}
			/>

			<Link href="/add-post" className="es-composer" aria-label="Create a post">
				<PersonAvatar name={profile?.full_name || profile?.username} seed={profile?.username} src={profile?.avatar} size="md" />
				<span className="es-composer-field">Share something with your neighbours…</span>
				<span className="es-btn es-btn--primary es-btn--sm">
					<PenLine aria-hidden="true" />
					Post
				</span>
			</Link>

			<section aria-labelledby="feed-title">
				<div className="es-listhead">
					<h2 id="feed-title">Posts</h2>
					{ready && totalCount > posts.length && (
						<span>
							Showing {posts.length} of {totalCount}
						</span>
					)}
				</div>

				{isLoading ? (
					<FeedSkeleton rows={4} />
				) : isError ? (
					<ErrorState what="posts" onRetry={refetch} retrying={isFetching} />
				) : posts.length > 0 ? (
					<div className="es-postlist es-stagger" aria-busy={isFetching}>
						{posts.map((post, index) => (
							<PostListItem key={post.id} post={post} index={index} />
						))}
					</div>
				) : (
					<EmptyState
						icon={MessagesSquare}
						description="Be the first to say hello, ask a question or share a notice with the building."
						action={
							<Link href="/add-post" className="es-btn es-btn--primary">
								<PenLine aria-hidden="true" />
								Create a post
							</Link>
						}
					>
						It&apos;s quiet in here
					</EmptyState>
				)}
			</section>

			<PaginationSection totalPages={totalPages} entityType="post" />
		</div>
	);
}
