import React from "react";
import { CardDescription } from "../ui/card";
import Tooltip from "../shared/Tooltip";
import { BookMarkedIcon, ThumbsDownIcon, ThumbsUpIcon, Trash2Icon } from "lucide-react";

interface PostActionsProps {
	upvotes: number | undefined;
	downvotes: number | undefined;
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
	upvotes,
	downvotes,
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
		<CardDescription className="mt-2">
			<div className="flex flex-col sm:flex-row items-center space-y-2 sm:space-y-0 sm:space-x-4">
				<Tooltip content="Upvote this post" position="top">
					<button
						onClick={handleUpvote}
						disabled={isUpvoteLoading}
						className={`flex items-center ${isUpvoteLoading ? 'opacity-50' : ''}`}
					>
						<ThumbsUpIcon className="tab-icon text-electricIndigo h-6 w-6" />
						<span className="ml-1 text-lg">{upvotes}</span>
						{isUpvoteLoading && <span className="ml-1 text-sm">Loading...</span>}
					</button>
				</Tooltip>
				<Tooltip content="Downvote this post" position="top">
					<button
						onClick={handleDownVote}
						disabled={isDownvoteLoading}
						className={`flex items-center ${isDownvoteLoading ? 'opacity-50' : ''}`}
					>
						<ThumbsDownIcon className="tab-icon text-electricIndigo h-6 w-6" />
						<span className="ml-1 text-lg">{downvotes}</span>
						{isDownvoteLoading && <span className="ml-1 text-sm">Loading...</span>}
					</button>
				</Tooltip>
				<Tooltip content="Bookmark this post" position="top">
					<button
						onClick={handleBookmarkPost}
						disabled={isBookmarkLoading}
						className={`flex items-center ${isBookmarkLoading ? 'opacity-50' : ''}`}
					>
						<BookMarkedIcon className="tab-icon text-electricIndigo h-6 w-6" />
						{isBookmarkLoading && <span className="ml-1 text-sm">Loading...</span>}
					</button>
				</Tooltip>
				<Tooltip content="Delete this post" position="top">
					<button
						onClick={handleDeletePost}
						disabled={isDeleteLoading}
						className={`text-red-500 flex items-center ${isDeleteLoading ? 'opacity-50' : ''}`}
					>
						<Trash2Icon className="tab-icon text-electricIndigo h-6 w-6" />
					</button>
				</Tooltip>
			</div>
		</CardDescription>
	);
}
