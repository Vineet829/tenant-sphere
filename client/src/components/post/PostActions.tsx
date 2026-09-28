import ConfirmButton from "@/components/estate/ConfirmButton";
import { ArrowBigDown, ArrowBigUp, Bookmark, Pencil, Trash2 } from "lucide-react";
import Link from "next/link";

interface PostActionsProps {
	slug: string | undefined;
	upvotes: number | undefined;
	downvotes: number | undefined;
	isUpvoted?: boolean;
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
	return (
		<footer className="es-actionbar">
			<div className="es-actionbar-group">
				<div className="es-votes" role="group" aria-label="Vote on this post">
					<button
						type="button"
						className="es-action es-action--vote"
						onClick={handleUpvote}
						disabled={isUpvoteLoading}
						aria-pressed={isUpvoted}
						aria-label={`Upvote. ${upvotes ?? 0} so far`}
						title="Upvote this post"
					>
						<ArrowBigUp aria-hidden="true" />
						{upvotes ?? 0}
					</button>
					<span className="es-votes-rule" aria-hidden="true" />
					<button
						type="button"
						className="es-action es-action--vote"
						onClick={handleDownVote}
						disabled={isDownvoteLoading}
						aria-label={`Downvote. ${downvotes ?? 0} so far`}
						title="Downvote this post"
					>
						<ArrowBigDown aria-hidden="true" />
						{downvotes ?? 0}
					</button>
				</div>

				<button
					type="button"
					className="es-action"
					onClick={handleBookmarkPost}
					disabled={isBookmarkLoading}
					aria-pressed={isBookmarked}
					title={isBookmarked ? "In your bookmarks" : "Bookmark this post"}
				>
					<Bookmark aria-hidden="true" />
					{isBookmarked ? "Saved" : "Save"}
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
