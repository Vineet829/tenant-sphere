import FormShell from "@/components/estate/FormShell";
import PasswordResetRequestForm from "@/components/forms/auth/PasswordResetRequestForm";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Reset your password — Tenant Sphere",
	description: "Password request reset page",
};

export default function ForgotPassword() {
	return (
		<FormShell
			eyebrow="Account"
			title={
				<>
					Reset your <em>password</em>
				</>
			}
			description="Enter your email address and we will send you a link to choose a new password."
			back={{ href: "/login", label: "Back to log in" }}
		>
			<PasswordResetRequestForm />
		</FormShell>
	);
}
