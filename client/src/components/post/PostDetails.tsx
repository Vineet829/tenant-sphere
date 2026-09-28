"use client";

import EmptyState from "@/components/estate/EmptyState";
import ErrorState from "@/components/estate/ErrorState";
import { isMissing } from "@/components/estate/apiError";
import { DetailSkeleton } from "@/components/estate/Skeletons";
import { plural } from "@/components/estate/format";
import {
	useBookmarkPostMutation,
	useDeletePostMutation,
	useDownvotePostMutation,
	useGetSinglePostQuery,
	useUpvotePostMutation,
} from "@/lib/redux/features/posts/postApiSlice";
import { useGetUserProfileQuery } from "@/lib/redux/features/users/usersApiSlice";
import { sortByDateDescending } from "@/utils";
import { ArrowLeft, FileQuestion, MessagesSquare } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "react-toastify";
import CreateReplyForm from "../forms/add-reply/CreateReplyForm";
import ProtectedRoute from "../shared/ProtectedRoutes";
import PostActions from "./PostActions";
import PostBody from "./PostBody";
import PostHeader from "./PostHeader";
import RepliesList from "./RepliesList";

interface PostDetailsProps {
	params: {
		slug: string;
	};
}

function BackLink() {
	return (
		<Link href="/welcome" className="es-backlink">
			<ArrowLeft aria-hidden="true" />
			Back to community
		</Link>
	);
}

function PostDetailsContent({ params }: PostDetailsProps) {
	const slug = params.slug;

	const { data, isLoading, isFetching, isError, error, refetch } = useGetSinglePostQuery(slug);
	const { data: currentUser } = useGetUserProfileQuery();
	const post = data?.post;

	const [upvotePost, { isLoading: isUpvoteLoading }] = useUpvotePostMutation();
	const [downvotePost, { isLoading: isDownvoteLoading }] = useDownvotePostMutation();
	const [bookmarkPost, { isLoading: isBookmarkLoading }] = useBookmarkPostMutation();
	const [deletePost, { isLoading: isDeleteLoading }] = useDeletePostMutation();
	const router = useRouter();
	const sortedReplies = sortByDateDescending(post?.replies ?? [], "created_at");

	const isAuthor = Boolean(post && currentUser && post.author_username === currentUser.profile.username);

	const handleUpvote = () => {
		post?.id && upvotePost(post.id);
		toast.success("Post upvoted");
	};

	const handleDownVote = () => {
		post?.id && downvotePost(post.id);
		toast.success("Post downvoted");
	};

	const handleBookmarkPost = () => {
		post?.slug && bookmarkPost(post.slug);
		toast.success("Saved to your bookmarks");
	};

	const handleDeletePost = async () => {
		try {
			post?.id && (await deletePost(post.id).unwrap());

			toast.success("Post deleted");
			router.back();
		} catch (error) {
			toast.error("We could not delete the post");
		}
	};

	if (isLoading) {
		return (
			<div className="es-page es-page--reading">
				<BackLink />
				<DetailSkeleton />
			</div>
		);
	}

	if (isError && !isMissing(error)) {
		return (
			<div className="es-page es-page--reading">
				<BackLink />
				<ErrorState what="this post" onRetry={refetch} retrying={isFetching} />
			</div>
		);
	}

	if (isError || !post) {
		return (
			<div className="es-page es-page--reading">
				<BackLink />
				<EmptyState
					icon={FileQuestion}
					description="It may have been deleted, or the link might be out of date."
					action={
						<Link href="/welcome" className="es-btn es-btn--primary">
							Go to the community
						</Link>
					}
				>
					We could not find that post
				</EmptyState>
			</div>
		);
	}

	return (
		<div className="es-page es-page--reading">
			<BackLink />

			<article className="es-card es-article">
				<PostHeader
					title={post.title}
					tags={post.tags}
					avatar={post.avatar}
					author_username={post.author_username}
					created_at={post.created_at}
					updated_at={post.updated_at}
					view_count={post.view_count}
				/>
				<PostBody body={post.body} />
				<PostActions
					slug={post.slug}
					upvotes={post.upvotes}
					downvotes={post.downvotes}
					isUpvoted={post.is_upvoted}
					isBookmarked={post.is_bookmarked}
					canManage={isAuthor}
					handleUpvote={handleUpvote}
					handleDownVote={handleDownVote}
					handleBookmarkPost={handleBookmarkPost}
					handleDeletePost={handleDeletePost}
					isUpvoteLoading={isUpvoteLoading}
					isDownvoteLoading={isDownvoteLoading}
					isBookmarkLoading={isBookmarkLoading}
					isDeleteLoading={isDeleteLoading}
				/>
			</article>

			<section className="es-thread" id="replies" aria-labelledby="replies-title">
				<h2 className="es-section-title" id="replies-title">
					<MessagesSquare aria-hidden="true" strokeWidth={1.75} />
					{plural(post.replies_count, "reply", "replies")}
				</h2>

				<CreateReplyForm slug={post.slug} />

				{sortedReplies.length > 0 ? (
					<ol className="es-replies">
						{sortedReplies.map((reply) => (
							<RepliesList key={reply.id} reply={reply} isAuthor={reply.author_username === post.author_username} />
						))}
					</ol>
				) : (
					<p className="es-thread-empty">No replies yet. Be the first to join in.</p>
				)}
			</section>
		</div>
	);
}

export default function PostDetails({ params }: PostDetailsProps) {
	return (
		<ProtectedRoute>
			<PostDetailsContent params={params} />
		</ProtectedRoute>
	);
}
