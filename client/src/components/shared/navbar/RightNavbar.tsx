"use client";

import { useGetPopularTagsQuery, useGetTopPostsQuery } from "@/lib/redux/features/posts/postApiSlice";
import { ArrowBigUp, Hash, TrendingUp } from "lucide-react";
import Link from "next/link";

export default function RightNavbar() {
	const { data, isLoading: postsLoading, isError: postsFailed } = useGetTopPostsQuery();
	const topPosts = data?.top_posts.results;
	const { data: tagData, isLoading: tagsLoading, isError: tagsFailed } = useGetPopularTagsQuery();
	const tags = tagData?.popular_tags.results.slice(0, 5);

	return (
		<aside className="es-rail" aria-label="Community highlights">
			<section className="es-rail-section">
				<h2 className="es-rail-title">
					<TrendingUp aria-hidden="true" strokeWidth={2} />
					Top posts
				</h2>
				{postsLoading ? (
					<div className="es-rail-loading" role="status">
						<span className="es-sr">Loading top posts…</span>
						{[72, 58, 84, 64].map((width) => (
							<div key={width} className="es-rail-loading-row">
								<span className="es-skeleton" style={{ width: 22, height: 22 }} />
								<span className="es-rail-loading-copy">
									<span className="es-skeleton" style={{ width: `${width}%`, height: 12 }} />
									<span className="es-skeleton" style={{ width: "38%", height: 10 }} />
								</span>
							</div>
						))}
					</div>
				) : topPosts && topPosts.length > 0 ? (
					<ol className="es-toplist">
						{topPosts.map((post, index) => (
							<li key={post.id}>
								<Link href={`/post/${post.slug}`} className="es-toplist-item">
									<span className="es-toplist-rank" aria-hidden="true">
										{String(index + 1).padStart(2, "0")}
									</span>
									<span className="es-toplist-title">{post.title}</span>
									<span className="es-toplist-meta">
										<span>@{post.author_username}</span>
										<span title={`${post.upvotes} upvotes`}>
											<ArrowBigUp aria-hidden="true" strokeWidth={1.75} />
											{post.upvotes}
											<span className="es-sr"> upvotes</span>
										</span>
									</span>
								</Link>
							</li>
						))}
					</ol>
				) : (
					<p className="es-rail-empty">{postsFailed ? "Top posts could not be loaded." : "No top posts yet."}</p>
				)}
			</section>

			<section className="es-rail-section">
				<h2 className="es-rail-title">
					<Hash aria-hidden="true" strokeWidth={2} />
					Popular tags
				</h2>
				{tagsLoading ? (
					<div className="es-taglist" role="status">
						<span className="es-sr">Loading popular tags…</span>
						{[96, 132, 118, 104].map((width) => (
							<span key={width} className="es-skeleton es-skeleton--pill" style={{ width, height: 27 }} />
						))}
					</div>
				) : tags && tags.length > 0 ? (
					<div className="es-taglist">
						{tags.map((tag) => (
							<Link key={tag.slug} href={`/tags/${tag.slug}`} className="es-chip es-chip--tag">
								{tag.name}
								<span className="es-chip-count">{tag.post_count}</span>
							</Link>
						))}
					</div>
				) : (
					<p className="es-rail-empty">{tagsFailed ? "Tags could not be loaded." : "No popular tags yet."}</p>
				)}
			</section>

			<footer className="es-rail-foot">
				<p>Good things start with good neighbours.</p>
				<p>© {new Date().getFullYear()} Tenant Sphere</p>
			</footer>
		</aside>
	);
}
