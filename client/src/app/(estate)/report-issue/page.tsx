import { ApartmentContext } from "@/components/estate/FormContexts";
import FormShell, { FormSteps } from "@/components/estate/FormShell";
import CreateIssueForm from "@/components/forms/report-issue/CreateIssueForm";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Report an issue — Tenant Sphere",
	description: "Tenants can report any issues to the management with regards to their apartments",
};

const STEPS = [
	{ title: "Reported", text: "You describe what is wrong at your apartment." },
	{ title: "In progress", text: "The technician assigned to it gets to work." },
	{ title: "Resolved", text: "The fix is done and the issue is closed." },
];

export default function ReportIssuePage() {
	return (
		<FormShell
			eyebrow="Get help"
			title={
				<>
					Something <em>not right?</em>
				</>
			}
			description="Tell us what needs attention. You can follow it from reported to resolved on your profile."
			aside={
				<>
					<ApartmentContext />
					<FormSteps steps={STEPS} label="How an issue moves along" />
				</>
			}
		>
			<CreateIssueForm />
		</FormShell>
	);
}
