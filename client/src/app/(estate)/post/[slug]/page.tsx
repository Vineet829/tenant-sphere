import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import PostDetails from "@/components/post/PostDetails";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Post — Tenant Sphere",
	description: "Authenticated users can see the details of a post",
};


interface ParamsProps {
	params: {
		slug: string;
	};
}

function PostDetailPage({ params }: ParamsProps) {
	return (
		<>
			<PostDetails params={params} />
		</>
	);
}

export default async function ProtectedPage({ params }: { params: Promise<ParamsProps["params"]> }) {
	const props = { params: await params };
	return <ProtectedRoute><PostDetailPage {...props} /></ProtectedRoute>;
}
