import PostCard from "@/components/cards/PostCard";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Tenant Sphere | Welcome",
	description:
		"Welcome to the Tenant Sphere Website. This webapp allows users who are tenants to signup, create their profiles, report any issues with their apartments, report any tenants, post anything of relevance for other tenants to see and or respond.",
};

export default function WelcomePage() {
	return (
		<>
			<PostCard />
		</>
	);
}
