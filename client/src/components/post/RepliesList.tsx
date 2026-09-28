import PersonAvatar from "@/components/estate/PersonAvatar";
import { longDate, timeAgo } from "@/components/estate/format";
import { useDisplayName } from "@/components/estate/useDisplayName";
import { Reply } from "@/types";

interface ReplyProps {
	reply: Reply;
	/** The reply was written by the person who wrote the post. */
	isAuthor?: boolean;
}

export default function RepliesList({ reply, isAuthor = false }: ReplyProps) {
	const displayName = useDisplayName();

	return (
		<li className="es-reply">
			<PersonAvatar
				name={displayName(reply.author_username)}
				seed={reply.author_username}
				src={reply.avatar}
				size="sm"
			/>
			<div className="es-reply-body">
				<p className="es-reply-head">
					<span className="es-reply-name">@{reply.author_username}</span>
					{isAuthor && <span className="es-chip es-chip--accent es-reply-badge">Author</span>}
					<time className="es-reply-time" dateTime={reply.created_at} title={longDate(reply.created_at)}>
						{timeAgo(reply.created_at)}
					</time>
				</p>
				<p className="es-reply-text">{reply.body}</p>
			</div>
		</li>
	);
}
