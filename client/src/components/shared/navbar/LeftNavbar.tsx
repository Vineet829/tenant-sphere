"use client";
import PersonAvatar from "@/components/estate/PersonAvatar";
import { useAuthNavigation } from "@/hooks";
import { useUserProfile } from "@/hooks/useUseProfile";
import { LogOut } from "lucide-react";
import Link from "next/link";
import NavLinks from "./NavLinks";

export default function LeftNavbar() {
	const { handleLogout, isAuthenticated } = useAuthNavigation();
	const { profile, isLoading } = useUserProfile();

	return (
		<aside className="es-sidebar">
			<NavLinks />

			<div className="es-nav-foot">
				{isAuthenticated && isLoading ? (
					<div className="es-usercard" role="status">
						<span className="es-sr">Loading your account…</span>
						<span className="es-avatar es-avatar--sm es-skeleton es-skeleton--circle" />
						<span className="es-usercard-copy !gap-[7px]">
							<span className="es-skeleton h-[11px] w-[96px]" />
							<span className="es-skeleton h-[9px] w-[64px]" />
						</span>
					</div>
				) : isAuthenticated ? (
					<div className="es-usercard">
						<Link href="/profile" className="es-usercard-link" title="Your profile">
							<PersonAvatar name={profile?.full_name || profile?.username} seed={profile?.username} src={profile?.avatar} size="sm" />
							<span className="es-usercard-copy">
								<span className="es-usercard-name">{profile?.full_name ?? "Your profile"}</span>
								{profile?.username && <span className="es-usercard-handle">@{profile.username}</span>}
							</span>
						</Link>
						<button type="button" className="es-icon-button" onClick={handleLogout} aria-label="Log out" title="Log out">
							<LogOut aria-hidden="true" strokeWidth={1.75} />
						</button>
					</div>
				) : (
					<div className="es-nav-auth">
						<Link href="/login" className="es-btn es-btn--outline">
							Log in
						</Link>
						<Link href="/register" className="es-btn es-btn--primary">
							Register
						</Link>
					</div>
				)}
			</div>
		</aside>
	);
}
