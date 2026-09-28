import "@/components/estate/estate.css";
import Wordmark from "@/components/estate/Wordmark";
import { estateEditorial, estateSans } from "@/components/estate/fonts";
import { ArrowLeft } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = { title: "Page not found — Tenant Sphere" };

export default function NotFound() {
	return (
		<div className={`estate-shell es-notfound ${estateSans.variable} ${estateEditorial.variable}`}>
			<header className="es-notfound-head">
				<Wordmark />
			</header>
			<main className="es-statuspage">
				<p className="es-notfound-code" aria-hidden="true">
					404
				</p>
				<p className="es-eyebrow es-eyebrow--plain">Page not found</p>
				<h1 className="es-statuspage-title">
					This door does not <em>lead anywhere</em>.
				</h1>
				<p className="es-statuspage-text">
					The page may have moved, or the address might be mistyped. Let us get you back home.
				</p>
				<Link href="/welcome" className="es-btn es-btn--primary">
					<ArrowLeft aria-hidden="true" />
					Back to the community
				</Link>
			</main>
		</div>
	);
}
