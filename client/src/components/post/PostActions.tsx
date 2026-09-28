import ConfirmButton from "@/components/estate/ConfirmButton";
import { voteScore } from "@/components/estate/format";
import { ArrowBigDown, ArrowBigUp, Bookmark, Pencil, Trash2 } from "lucide-react";
import Link from "next/link";

interface PostActionsProps {
	slug: string | undefined;
	upvotes: number | undefined;
	downvotes: number | undefined;
	isUpvoted?: boolean;
	isDownvoted?: boolean;
	isBookmarked?: boolean;
	/** Only the author can edit or delete a post. */
	canManage?: boolean;
	handleUpvote: () => void;
	handleDownVote: () => void;
	handleBookmarkPost: () => void;
	handleDeletePost: () => void;
	isUpvoteLoading: boolean;
	isDownvoteLoading: boolean;
	isBookmarkLoading: boolean;
	isDeleteLoading: boolean;
}

export default function PostActions({
	slug,
	upvotes,
	downvotes,
	isUpvoted = false,
	isDownvoted = false,
	isBookmarked = false,
	canManage = false,
	handleUpvote,
	handleDownVote,
	handleBookmarkPost,
	handleDeletePost,
	isUpvoteLoading,
	isBookmarkLoading,
	isDownvoteLoading,
	isDeleteLoading,
}: PostActionsProps) {
	const isVoting = isUpvoteLoading || isDownvoteLoading;
	const score = voteScore({ upvotes, downvotes });

	return (
		<footer className="es-actionbar">
			<div className="es-actionbar-group">
				<div className="es-votes" role="group" aria-label="Vote on this post" aria-busy={isVoting}>
					<button
						type="button"
						className="es-action es-action--vote"
						onClick={handleUpvote}
						disabled={isVoting}
						aria-busy={isUpvoteLoading}
						aria-pressed={isUpvoted}
						aria-label={isUpvoted ? "Remove upvote" : "Upvote"}
						title={isUpvoted ? "Remove upvote" : "Upvote this post"}
					>
						{isUpvoteLoading ? <span className="es-spinner" aria-hidden="true" /> : <ArrowBigUp aria-hidden="true" />}
					</button>
					<span
						className="es-vote-score"
						role="status"
						aria-live="polite"
						aria-atomic="true"
						title={`${upvotes ?? 0} upvotes, ${downvotes ?? 0} downvotes`}
					>
						<span className="es-sr">Vote score: </span>{score}
					</span>
					<button
						type="button"
						className="es-action es-action--vote"
						onClick={handleDownVote}
						disabled={isVoting}
						aria-busy={isDownvoteLoading}
						aria-pressed={isDownvoted}
						aria-label={isDownvoted ? "Remove downvote" : "Downvote"}
						title={isDownvoted ? "Remove downvote" : "Downvote this post"}
					>
						{isDownvoteLoading ? <span className="es-spinner" aria-hidden="true" /> : <ArrowBigDown aria-hidden="true" />}
					</button>
				</div>

				<button
					type="button"
					className="es-action"
					onClick={handleBookmarkPost}
					disabled={isBookmarkLoading || isBookmarked}
					aria-busy={isBookmarkLoading}
					aria-pressed={isBookmarked}
					title={isBookmarked ? "In your bookmarks" : "Bookmark this post"}
				>
					{isBookmarkLoading ? <span className="es-spinner" aria-hidden="true" /> : <Bookmark aria-hidden="true" />}
					{isBookmarkLoading ? "Saving…" : isBookmarked ? "Saved" : "Save"}
				</button>
			</div>

			{canManage && (
				<div className="es-actionbar-group">
					<Link href={`/post/update-post/${slug}`} className="es-action">
						<Pencil aria-hidden="true" />
						Edit
					</Link>
					<ConfirmButton
						className="es-action es-action--danger"
						onConfirm={handleDeletePost}
						disabled={isDeleteLoading}
						confirmLabel={
							<>
								<Trash2 aria-hidden="true" />
								Delete for good?
							</>
						}
					>
						<Trash2 aria-hidden="true" />
						Delete
					</ConfirmButton>
				</div>
			)}
		</footer>
	);
}
