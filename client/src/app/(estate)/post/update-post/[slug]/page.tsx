import ProtectedRoute from "@/components/shared/ProtectedRoutes";
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

function UpdatePostPage({ params }: UpdateParamsProps) {
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

export default async function ProtectedPage({ params }: { params: Promise<UpdateParamsProps["params"]> }) {
	const props = { params: await params };
	return <ProtectedRoute><UpdatePostPage {...props} /></ProtectedRoute>;
}
