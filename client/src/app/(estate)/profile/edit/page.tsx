import FormShell, { FormContext } from "@/components/estate/FormShell";
import EditProfileForm from "@/components/forms/profile/EditProfileForm";
import { UsersRound } from "lucide-react";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Edit profile — Tenant Sphere",
	description: "Signed in users can edit their profile information",
};

export default function EditProfilePage() {
	return (
		<FormShell
			eyebrow="Your space"
			title={
				<>
					Edit your <em>profile</em>
				</>
			}
			description="A name, a face and a few lines go a long way towards feeling at home."
			back={{ href: "/profile", label: "Back to profile" }}
			aside={
				<FormContext icon={<UsersRound strokeWidth={1.75} />} label="Who sees this">
					Your name, bio and apartment appear in the tenant directory.
				</FormContext>
			}
		>
			<EditProfileForm />
		</FormShell>
	);
}
