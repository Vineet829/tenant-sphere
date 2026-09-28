import { TechnicianContext } from "@/components/estate/FormContexts";
import FormShell from "@/components/estate/FormShell";
import CreateRatingForm from "@/components/forms/add-rating/CreateRatingForm";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Rate a technician — Tenant Sphere",
	description:
		"Tenants can rate the technicians, if they are satisfied or dissatisfied with the services rendered to them",
};

export default function AddRatingPage() {
	return (
		<FormShell
			eyebrow="Technicians"
			title={
				<>
					Rate the <em>work</em>
				</>
			}
			description="Tell us how the job went. An honest rating helps your neighbours choose well."
			back={{ href: "/technicians", label: "Back to technicians" }}
			aside={<TechnicianContext />}
		>
			<CreateRatingForm />
		</FormShell>
	);
}
