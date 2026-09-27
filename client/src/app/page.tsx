import type { Metadata } from "next";
import HomeContent from "@/components/home/HomeContent";

export const metadata: Metadata = {
	title: "Tenant Sphere — Good neighbours. Better living.",
	description:
		"A little closer to home. Report maintenance issues, connect with your neighbours, and keep your apartment community together with Tenant Sphere.",
};

export default function HomePage() {
	return <HomeContent />;
}
