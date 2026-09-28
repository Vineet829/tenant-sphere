import { IssueContext } from "@/components/estate/FormContexts";
import FormShell from "@/components/estate/FormShell";
import UpdateIssueForm from "@/components/forms/update-issue/UpdateIssueForm";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Update issue — Tenant Sphere",
	description: "Technicians assigned to an issue can update the status of the issue",
};

interface UpdateParamsProps {
	params: {
		id: string;
	};
}

export default function UpdateIssuePage({ params }: UpdateParamsProps) {
	return (
		<FormShell
			eyebrow="Assigned to you"
			title={
				<>
					Move it <em>forward</em>
				</>
			}
			description="Update the status so the tenant knows where things stand, every step of the way."
			back={{ href: `/issue/${params.id}`, label: "Back to the issue" }}
			aside={<IssueContext id={params.id} />}
		>
			<UpdateIssueForm params={params} />
		</FormShell>
	);
}
