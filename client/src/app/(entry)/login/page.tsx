import type { Metadata } from "next";
import AuthExperience from "@/components/auth/AuthExperience";

export const metadata: Metadata = { title: "Log in — Tenant Sphere", description: "Welcome home. Log in to your Tenant Sphere community." };

export default function LoginPage() {
	return <AuthExperience mode="login" />;
}
