import { estateEditorial, estateSans } from "@/components/estate/fonts";
import LeftNavbar from "@/components/shared/navbar/LeftNavbar";
import Navbar from "@/components/shared/navbar/Navbar";
import RightNavbar from "@/components/shared/navbar/RightNavbar";
import React from "react";
import "@/components/estate/estate.css";

interface LayoutProps {
	children: React.ReactNode;
}

export default function layout({ children }: LayoutProps) {
	return (
		<div className={`estate-shell ${estateSans.variable} ${estateEditorial.variable}`}>
			<a href="#content" className="es-skip">
				Skip to content
			</a>
			<Navbar />
			<div className="es-workspace">
				<LeftNavbar />
				<main className="es-main" id="content" tabIndex={-1}>
					<div className="es-content">{children}</div>
				</main>
				<RightNavbar />
			</div>
		</div>
	);
}
