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
	// Vaadin uses /admin/ for its initialization and UI requests in development.
	skipTrailingSlashRedirect: process.env.NODE_ENV === "development",
	async rewrites() {
		if (process.env.NODE_ENV !== "development") return [];
		const backend = (process.env.NEXT_PUBLIC_DOMAIN || "http://localhost:8000").replace(/\/$/, "");
		return [
			{ source: "/api/v1/:path*", destination: `${backend}/api/v1/:path*/` },
			{ source: "/media/:path*", destination: `${backend}/media/:path*` },
			{ source: "/admin", destination: `${backend}/admin/` },
			{ source: "/admin/", destination: `${backend}/admin/` },
			{ source: "/admin/:path*", destination: `${backend}/admin/:path*` },
			{ source: "/admin-assets/:path*", destination: `${backend}/admin-assets/:path*` },
			{ source: "/VAADIN/:path*", destination: `${backend}/VAADIN/:path*` },
			{ source: "/supersecret/:path*", destination: `${backend}/supersecret/:path*` },
		];
	},
};

export default nextConfig;
