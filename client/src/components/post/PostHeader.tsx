import MetaList from "@/components/estate/MetaList";
import PersonAvatar from "@/components/estate/PersonAvatar";
import { longDate, plural, tagSlug, timeAgo } from "@/components/estate/format";
import { useDisplayName } from "@/components/estate/useDisplayName";
import Link from "next/link";

interface PostHeaderProps {
	title: string | undefined;
	tags?: string[];
	avatar: string | undefined | null;
	author_username: string | undefined;
	created_at: string | undefined;
	updated_at?: string;
	view_count: number | undefined;
}

export default function PostHeader({
	title,
	tags = [],
	avatar,
	author_username,
	created_at,
	updated_at,
	view_count,
}: PostHeaderProps) {
	const created = created_at ? new Date(created_at.replace(" ", "T")).getTime() : NaN;
	const updated = updated_at ? new Date(updated_at.replace(" ", "T")).getTime() : NaN;
	// Saves within a minute of posting are part of writing it, not an edit.
	const edited = updated - created > 60_000;
	const editedAgo = edited ? timeAgo(updated_at) : "";
	const displayName = useDisplayName();

	return (
		<header className="es-article-head">
			{tags.length > 0 && (
				<div className="es-article-tags">
					{tags.map((tag) => (
						<Link key={tag} href={`/tags/${tagSlug(tag)}`} className="es-chip es-chip--tag">
							{tag}
						</Link>
					))}
				</div>
			)}

			<h1 className="es-article-title">{title}</h1>

			<div className="es-byline">
				<PersonAvatar name={displayName(author_username)} seed={author_username} src={avatar} size="md" />
				<div className="es-byline-copy">
					<span className="es-byline-name">@{author_username}</span>
					<MetaList className="es-byline-meta">
						<span>
							Posted{" "}
							<time dateTime={created_at} title={longDate(created_at)}>
								{timeAgo(created_at)}
							</time>
						</span>
						{edited && (
							<span title={`Edited ${longDate(updated_at)}`}>
								{editedAgo === timeAgo(created_at) ? "Edited" : `Edited ${editedAgo}`}
							</span>
						)}
						<span>{plural(view_count, "view")}</span>
					</MetaList>
				</div>
			</div>
		</header>
	);
}
