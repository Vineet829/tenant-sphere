import FormShell from "@/components/estate/FormShell";
import PasswordResetConfirmForm from "@/components/forms/auth/PasswordResetConfirmForm";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Choose a new password — Tenant Sphere",
	description: "Password request reset page",
};

export default function ForgotPassword() {
	return (
		<FormShell
			eyebrow="Account"
			title={
				<>
					Choose a new <em>password</em>
				</>
			}
			description="Pick something you have not used before, at least eight characters long."
			back={{ href: "/login", label: "Back to log in" }}
		>
			<PasswordResetConfirmForm />
		</FormShell>
	);
}
