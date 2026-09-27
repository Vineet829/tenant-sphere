/** @type {import('next').NextConfig} */
const nextConfig = {
	images: {
		remotePatterns: [
			{
				hostname: "res.cloudinary.com",
			},
		],
	},
	output: "standalone",
	async rewrites() {
		if (process.env.NODE_ENV !== "development") return [];
		const backend = (process.env.NEXT_PUBLIC_DOMAIN || "http://localhost:8000").replace(/\/$/, "");
		return [{ source: "/api/v1/:path*", destination: `${backend}/api/v1/:path*/` }];
	},
};

export default nextConfig;
