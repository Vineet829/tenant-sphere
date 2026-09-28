import FormShell, { FormSteps } from "@/components/estate/FormShell";
import CreatePostForm from "@/components/forms/add-post/CreatePostForm";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Create a post — Tenant Sphere",
	description: "Authenticated users can ask any question or post some content for all users to see",
};

const STEPS = [
	{ title: "Lead with the point", text: "A clear title gets answers faster." },
	{ title: "Add the detail", text: "Where, when, and who it matters to." },
	{ title: "Tag it", text: "One or two tags are enough." },
];

export default function AddPostPage() {
	return (
		<FormShell
			eyebrow="Community"
			title={
				<>
					Start a <em>conversation</em>
				</>
			}
			description="Ask a question, share a notice or pass on a recommendation with everyone in the building."
			back={{ href: "/welcome", label: "Back to community" }}
			aside={<FormSteps steps={STEPS} label="Writing a good post" />}
		>
			<CreatePostForm />
		</FormShell>
	);
}
