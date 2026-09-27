import type { Metadata } from "next";
import AuthExperience from "@/components/auth/AuthExperience";

export const metadata: Metadata = { title: "Create your account — Tenant Sphere", description: "Join Tenant Sphere and bring the everyday life of your building together." };

export default function RegisterPage() {
	return <AuthExperience mode="register" />;
}
