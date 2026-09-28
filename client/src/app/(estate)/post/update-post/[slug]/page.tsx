import FormShell from "@/components/estate/FormShell";
import UpdatePostForm from "@/components/forms/update-post/UpdatePostForm";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Edit post — Tenant Sphere",
	description: "Authors can update the title and the body of their post",
};

interface UpdateParamsProps {
	params: {
		slug: string;
	};
}

export default function UpdatePostPage({ params }: UpdateParamsProps) {
	return (
		<FormShell
			eyebrow="Community"
			title={
				<>
					Edit your <em>post</em>
				</>
			}
			description="Fix a detail or add what you missed. Replies and votes stay where they are."
			back={{ href: `/post/${params.slug}`, label: "Back to the post" }}
		>
			<UpdatePostForm params={params} />
		</FormShell>
	);
}
