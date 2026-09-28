"use client";

import { ArrowBigDown, ArrowBigUp, Bookmark, Eye, MessageCircle } from "lucide-react";
import Link from "next/link";
import type { CSSProperties } from "react";
import MetaList from "./MetaList";
import PersonAvatar from "./PersonAvatar";
import { longDate, plural, tagSlug, timeAgo, voteScore } from "./format";
import { useDisplayName } from "./useDisplayName";

export type PostSummary = {
	id: string;
	title: string;
	slug: string;
	body: string;
	tags?: string[];
	author_username?: string;
	avatar?: string | null;
	created_at: string;
	updated_at?: string;
	view_count?: number;
	upvotes?: number;
	downvotes?: number;
	is_upvoted?: boolean;
	is_downvoted?: boolean;
	replies_count?: number;
	replies?: unknown[];
	is_bookmarked?: boolean;
};

type PostListItemProps = {
	post: PostSummary;
	/** Position in the list, used to stagger the entrance. */
	index?: number;
	/** Hide the author when every row belongs to the same person. */
	showAuthor?: boolean;
	/** Hide the saved marker where every row is a saved post. */
	showSaved?: boolean;
};

export default function PostListItem({ post, index = 0, showAuthor = true, showSaved = true }: PostListItemProps) {
	const href = `/post/${post.slug}`;
	const replies = post.replies_count ?? post.replies?.length ?? 0;
	const tags = post.tags ?? [];
	const hasAuthor = showAuthor && Boolean(post.author_username);
	const displayName = useDisplayName();
	const score = voteScore(post);
	const ScoreIcon = post.is_downvoted || (!post.is_upvoted && score < 0) ? ArrowBigDown : ArrowBigUp;

	return (
		<article
			className="es-post"
			style={{ "--i": Math.min(index, 8) } as CSSProperties}
			aria-labelledby={`post-${post.id}`}
		>
			<div className="es-post-meta">
				{hasAuthor && (
					<PersonAvatar
						name={displayName(post.author_username)}
						seed={post.author_username}
						src={post.avatar}
						size="xs"
					/>
				)}
				<MetaList>
					{hasAuthor && <span className="es-post-author">@{post.author_username}</span>}
					<time dateTime={post.created_at} title={longDate(post.created_at)}>
						{hasAuthor ? timeAgo(post.created_at) : `Posted ${timeAgo(post.created_at)}`}
					</time>
				</MetaList>
				{showSaved && post.is_bookmarked && (
					<span className="es-post-saved">
						<Bookmark aria-hidden="true" />
						Saved
					</span>
				)}
			</div>

			<h3 className="es-post-title" id={`post-${post.id}`}>
				<Link href={href} className="es-post-link">
					{post.title}
				</Link>
			</h3>
			<p className="es-post-excerpt">{post.body}</p>

			<div className="es-post-foot">
				<div className="es-post-stats">
					{typeof post.upvotes === "number" && (
						<span
							className="es-stat es-stat--static"
							data-active={Boolean(post.is_upvoted || post.is_downvoted)}
							title={`${plural(post.upvotes, "upvote")}, ${plural(post.downvotes, "downvote")}`}
						>
							<ScoreIcon aria-hidden="true" />
							<span className="es-sr">Vote score: </span>{score}
							<span className="es-sr">{post.is_upvoted ? ". You upvoted" : post.is_downvoted ? ". You downvoted" : ""}</span>
						</span>
					)}
					<Link href={`${href}#replies`} className="es-stat" aria-label={`${plural(replies, "reply", "replies")} on ${post.title}`}>
						<MessageCircle aria-hidden="true" />
						{plural(replies, "reply", "replies")}
					</Link>
					{typeof post.view_count === "number" && (
						<span className="es-stat es-stat--static es-stat--quiet">
							<Eye aria-hidden="true" />
							{plural(post.view_count, "view")}
						</span>
					)}
				</div>
				{tags.length > 0 && (
					<div className="es-post-tags">
						{tags.slice(0, 3).map((tag) => (
							<Link key={tag} href={`/tags/${tagSlug(tag)}`} className="es-chip es-chip--tag">
								{tag}
							</Link>
						))}
					</div>
				)}
			</div>
		</article>
	);
}
