"use client";

import { useAuthNavigation } from "@/hooks";
import { Circle } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Fragment, type ReactElement, type ReactNode } from "react";
import { NAV_GROUPS, NAV_META } from "./navMeta";

type NavLinksProps = {
	/** Lets the mobile sheet wrap each link so that choosing one closes the sheet. */
	wrap?: (link: ReactElement) => ReactNode;
};

export default function NavLinks({ wrap = (link) => link }: NavLinksProps) {
	const pathname = usePathname();
	const { filteredNavLinks } = useAuthNavigation();

	const isActive = (path: string) => (pathname.includes(path) && path.length > 1) || pathname === path;

	const cta = filteredNavLinks.find((link) => NAV_META[link.path]?.group === "cta");

	return (
		<>
			{cta &&
				wrap(
					<Link
						href={cta.path}
						className="es-nav-cta"
						aria-current={isActive(cta.path) ? "page" : undefined}
						title={NAV_META[cta.path].label}
					>
						<PlusMark />
						<span className="es-nav-text">{NAV_META[cta.path].label}</span>
					</Link>,
				)}

			<nav className="es-nav" aria-label="Main">
				{NAV_GROUPS.map((group) => {
					const links = filteredNavLinks.filter((link) => NAV_META[link.path]?.group === group.key);
					if (links.length === 0) return null;
					return (
						<div className="es-nav-group" key={group.key}>
							<p className="es-nav-label">{group.label}</p>
							{links.map((link) => {
								const meta = NAV_META[link.path];
								const Icon = meta?.icon ?? Circle;
								const label = meta?.label ?? link.label;
								return (
									<Fragment key={link.path}>
										{wrap(
											<Link
												href={link.path}
												className="es-nav-link"
												aria-current={isActive(link.path) ? "page" : undefined}
												title={label}
											>
												<Icon aria-hidden="true" strokeWidth={1.75} />
												<span className="es-nav-text">{label}</span>
											</Link>,
										)}
									</Fragment>
								);
							})}
						</div>
					);
				})}
			</nav>
		</>
	);
}

function PlusMark() {
	return (
		<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
			<path d="M12 5v14M5 12h14" />
		</svg>
	);
}
