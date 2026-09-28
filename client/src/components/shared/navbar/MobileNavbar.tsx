"use client";
import { estateSans } from "@/components/estate/fonts";
import Wordmark from "@/components/estate/Wordmark";
import { Sheet, SheetClose, SheetContent, SheetTrigger } from "@/components/ui/sheet";
import { useAuthNavigation } from "@/hooks";
import { LogOut, Menu } from "lucide-react";
import Link from "next/link";
import NavLinks from "./NavLinks";

export default function MobileNavbar() {
	const { handleLogout, isAuthenticated } = useAuthNavigation();

	return (
		<Sheet>
			<SheetTrigger asChild>
				<button type="button" className="es-icon-button es-mobile-trigger" aria-label="Open navigation">
					<Menu aria-hidden="true" strokeWidth={1.75} />
				</button>
			</SheetTrigger>
			<SheetContent side="left" className={`estate-mobile-panel ${estateSans.variable}`}>
				<div className="es-mobile-head">
					<Wordmark />
				</div>

				<NavLinks wrap={(link) => <SheetClose asChild>{link}</SheetClose>} />

				<div className="es-nav-foot">
					{isAuthenticated ? (
						<SheetClose asChild>
							<button type="button" className="es-btn es-btn--outline es-btn--block" onClick={handleLogout}>
								<LogOut aria-hidden="true" strokeWidth={1.75} />
								Log out
							</button>
						</SheetClose>
					) : (
						<div className="es-nav-auth">
							<SheetClose asChild>
								<Link href="/login" className="es-btn es-btn--outline">
									Log in
								</Link>
							</SheetClose>
							<SheetClose asChild>
								<Link href="/register" className="es-btn es-btn--primary">
									Register
								</Link>
							</SheetClose>
						</div>
					)}
				</div>
			</SheetContent>
		</Sheet>
	);
}
