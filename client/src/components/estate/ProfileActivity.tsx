"use client";

import { useGetProfilePostsQuery, useGetProfileReviewsQuery } from "@/lib/redux/features/users/usersApiSlice";
import * as Dialog from "@radix-ui/react-dialog";
import { ArrowUp, ChevronLeft, ChevronRight, Loader2, MessageSquare, RefreshCw, Star } from "lucide-react";
import Link from "next/link";
import { useRef, useState } from "react";
import { plural, shortDate } from "./format";
import PersonAvatar from "./PersonAvatar";
import RatingStars from "./RatingStars";

function useActivityPage() {
	const [page, setPage] = useState(1);
	const headingRef = useRef<HTMLHeadingElement>(null);

	function changePage(nextPage: number) {
		setPage(nextPage);
		const heading = headingRef.current;
		if (!heading) return;
		heading.focus({ preventScroll: true });
		const scroller = heading.closest<HTMLElement>(".es-person-dialog-scroll");
		if (scroller) {
			const zoom = Number.parseFloat(window.getComputedStyle(document.documentElement).getPropertyValue("zoom")) || 1;
			const offset = (heading.getBoundingClientRect().top - scroller.getBoundingClientRect().top) / zoom;
			const top = scroller.scrollTop + offset - 28;
			scroller.scrollTo({ top: Math.max(0, top), behavior: "instant" });
		}
	}

	return { page, changePage, headingRef };
}

function ActivityLoading({ label }: { label: string }) {
	return (
		<div className="es-profile-loading" role="status" aria-label={`Loading ${label}`}>
			{[0, 1, 2].map((item) => (
				<div key={item} aria-hidden="true">
					<span className="es-skeleton" />
					<span className="es-skeleton" />
					<span className="es-skeleton" />
				</div>
			))}
		</div>
	);
}

function ActivityError({ label, onRetry, retrying }: { label: string; onRetry: () => void; retrying: boolean }) {
	return (
		<div className="es-profile-notice" role="alert">
			<p>We couldn’t load the {label}. Please try again.</p>
			<button type="button" className="es-btn es-btn--outline es-btn--sm" onClick={onRetry} disabled={retrying}>
				<RefreshCw aria-hidden="true" />
				Try again
			</button>
		</div>
	);
}

function ActivityPagination({ page, count, hasNext, busy, error, label, onChange }: {
	page: number;
	count: number;
	hasNext: boolean;
	busy: boolean;
	error: boolean;
	label: string;
	onChange: (page: number) => void;
}) {
	if (count <= 3 && page === 1) return null;
	const start = (page - 1) * 3 + 1;
	const end = Math.min(page * 3, count);
	let status = start > count ? "No more results" : `${start}–${end} of ${count}`;
	if (error) status = `Page ${page} unavailable`;
	if (busy) status = "Loading…";
	return (
		<nav className="es-profile-pagination" aria-label={`${label} pages`}>
			<p role="status">{status}</p>
			<div>
				<button type="button" className="es-btn es-btn--outline es-btn--sm" disabled={page === 1 || busy} onClick={() => onChange(page - 1)} aria-label={`Newer ${label}`}>
					<ChevronLeft aria-hidden="true" />
					Newer
				</button>
				<button type="button" className="es-btn es-btn--outline es-btn--sm" disabled={!hasNext || busy || error} onClick={() => onChange(page + 1)} aria-label={`Older ${label}`}>
					Older
					<ChevronRight aria-hidden="true" />
				</button>
			</div>
		</nav>
	);
}

function TenantPosts({ slug }: { slug: string }) {
	const { page, changePage, headingRef } = useActivityPage();
	const { currentData, data, isFetching, isError, refetch } = useGetProfilePostsQuery({ slug, page });
	const posts = currentData?.profile_posts;
	const count = data?.profile_posts.count;
	return (
		<section className="es-profile-activity" aria-label="Community posts" aria-busy={isFetching}>
			<div className="es-profile-section-head">
				<div>
					<h3 className="es-person-dialog-section" ref={headingRef} tabIndex={-1}>Community posts</h3>
					<p>Latest conversations</p>
				</div>
				{count !== undefined && <span className="es-profile-count">{plural(count, "post")}</span>}
				{isFetching && posts && <Loader2 className="es-profile-refresh" aria-label="Updating posts" />}
			</div>
			{isError ? (
				<ActivityError label="posts" onRetry={refetch} retrying={isFetching} />
			) : !posts ? (
				<ActivityLoading label="posts" />
			) : posts.results.length === 0 ? (
				<div className="es-profile-notice"><p>{page === 1 ? "No community posts yet." : "No more posts on this page."}</p></div>
			) : (
				<div className="es-profile-posts">
					{posts.results.map((post) => (
						<article className="es-profile-post" key={post.id}>
							<time dateTime={post.created_at}>{shortDate(post.created_at)}</time>
							<h4>
								<Dialog.Close asChild>
									<Link href={`/post/${encodeURIComponent(post.slug)}`}>
										{post.title}
										<ChevronRight aria-hidden="true" />
									</Link>
								</Dialog.Close>
							</h4>
							<p>{post.body}</p>
							<div className="es-profile-post-meta">
								<span><ArrowUp aria-hidden="true" />{plural(post.upvotes, "upvote")}</span>
								<span><MessageSquare aria-hidden="true" />{plural(post.replies_count, "reply", "replies")}</span>
							</div>
						</article>
					))}
				</div>
			)}
			{count !== undefined && <ActivityPagination page={page} count={count} hasNext={Boolean(posts?.next)} busy={isFetching} error={isError} label="posts" onChange={changePage} />}
		</section>
	);
}

function TechnicianReviews({ slug }: { slug: string }) {
	const { page, changePage, headingRef } = useActivityPage();
	const { currentData, data, isFetching, isError, refetch } = useGetProfileReviewsQuery({ slug, page });
	const summary = data?.profile_reviews;
	const reviews = currentData?.profile_reviews.reviews;
	const count = summary?.reviews.count;
	return (
		<section className="es-profile-activity" aria-label="Community reviews" aria-busy={isFetching}>
			<div className="es-profile-section-head">
				<div>
					<h3 className="es-person-dialog-section" ref={headingRef} tabIndex={-1}>Community reviews</h3>
					<p>Feedback from neighbours</p>
				</div>
				{count !== undefined && <span className="es-profile-count">{plural(count, "review")}</span>}
				{isFetching && reviews && <Loader2 className="es-profile-refresh" aria-label="Updating reviews" />}
			</div>
			{summary && count !== undefined && count > 0 && (
				<div className="es-profile-rating-summary">
					<div className="es-profile-rating-score">
						<strong>{summary.average_rating.toFixed(1)}<span> / 5</span></strong>
						<RatingStars value={summary.average_rating} showValue={false} />
						<span>From {plural(count, "review")}</span>
					</div>
					<dl className="es-profile-rating-bars" aria-label="Rating breakdown">
						{summary.distribution.map(({ stars, count: starCount }) => (
							<div key={stars}>
								<dt>{stars}<Star aria-hidden="true" /><span className="sr-only"> stars</span></dt>
								<dd>
									<span className="es-profile-rating-track" aria-hidden="true"><span style={{ width: `${(starCount / count) * 100}%` }} /></span>
									<span>{starCount}<span className="sr-only"> reviews</span></span>
								</dd>
							</div>
						))}
					</dl>
				</div>
			)}
			{isError ? (
				<ActivityError label="reviews" onRetry={refetch} retrying={isFetching} />
			) : !reviews ? (
				<ActivityLoading label="reviews" />
			) : reviews.results.length === 0 ? (
				<div className="es-profile-notice"><p>{page === 1 ? "No reviews yet. Feedback will appear here once someone rates their work." : "No more reviews on this page."}</p></div>
			) : (
				<div className="es-profile-reviews">
					{reviews.results.map((review) => (
						<article className="es-profile-review" key={review.id}>
							<header>
								<PersonAvatar name={review.author_name || review.author_username} seed={review.author_username} src={review.avatar} size="sm" />
								<div className="es-profile-review-author">
									<h4>{review.author_name || review.author_username}</h4>
									<RatingStars value={review.rating} showValue={false} />
								</div>
								<time dateTime={review.created_at}>{shortDate(review.created_at)}</time>
							</header>
							{review.comment.trim() ? <p>{review.comment}</p> : <p className="es-profile-review-empty">Rating only; no written review.</p>}
						</article>
					))}
				</div>
			)}
			{count !== undefined && <ActivityPagination page={page} count={count} hasNext={Boolean(reviews?.next)} busy={isFetching} error={isError} label="reviews" onChange={changePage} />}
		</section>
	);
}

export default function ProfileActivity({ slug, profileType }: { slug: string; profileType: "tenant" | "technician" }) {
	return profileType === "technician" ? <TechnicianReviews key={slug} slug={slug} /> : <TenantPosts key={slug} slug={slug} />;
}
