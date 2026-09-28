interface PostBodyProps {
	body: string | undefined;
}

export default function PostBody({ body }: PostBodyProps) {
	return (
		<div className="es-article-body">
			<p className="es-prose">{body}</p>
		</div>
	);
}
