import FormShell, { FormContext } from "@/components/estate/FormShell";
import ApartmentCreateForm from "@/components/forms/apartment/ApartmentCreateForm";
import { Wrench } from "lucide-react";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Add your apartment — Tenant Sphere",
	description: "Authenticated users can add their apartment details",
};

export default function AddApartmentPage() {
	return (
		<FormShell
			eyebrow="Your space"
			title={
				<>
					Add your <em>apartment</em>
				</>
			}
			description="Put your home on your profile, so your neighbours and the technicians know where to find you."
			back={{ href: "/profile", label: "Back to profile" }}
			aside={
				<FormContext icon={<Wrench strokeWidth={1.75} />} label="Good to know">
					You need an apartment on your profile to report an issue.
				</FormContext>
			}
		>
			<ApartmentCreateForm />
		</FormShell>
	);
}
