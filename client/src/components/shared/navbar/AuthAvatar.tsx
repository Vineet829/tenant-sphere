"use client";
import { estateSans } from "@/components/estate/fonts";
import PersonAvatar from "@/components/estate/PersonAvatar";
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuSeparator,
	DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuthNavigation } from "@/hooks";
import { useUserProfile } from "@/hooks/useUseProfile";
import { Bookmark, ChevronDown, LogOut, UserRound, UsersRound } from "lucide-react";
import Link from "next/link";
import React from "react";

export default function AuthAvatar() {
	const { handleLogout } = useAuthNavigation();
	const { profile, isLoading, isError } = useUserProfile();

	if (isLoading) {
		return <span className="es-account-trigger es-account-trigger--loading es-skeleton" aria-hidden="true" />;
	}

	if (isError || !profile) {
		return null;
	}

	// Keep the page scrollbar in place so the fixed header does not shift.
	return (
		<DropdownMenu modal={false}>
			<DropdownMenuTrigger asChild>
				<button type="button" className="es-account-trigger" aria-label="Account menu">
					<PersonAvatar name={profile.full_name || profile.username} seed={profile.username} src={profile.avatar} size="sm" />
					<ChevronDown aria-hidden="true" strokeWidth={2} />
				</button>
			</DropdownMenuTrigger>

			<DropdownMenuContent align="end" sideOffset={10} className={`estate-popover ${estateSans.variable}`}>
				<div className="es-menu-head">
					<PersonAvatar name={profile.full_name || profile.username} seed={profile.username} src={profile.avatar} size="md" />
					<div className="es-usercard-copy">
						<span className="es-usercard-name">{profile.full_name}</span>
						<span className="es-usercard-handle">@{profile.username}</span>
					</div>
				</div>
				<DropdownMenuItem asChild>
					<Link href="/profile">
						<UserRound aria-hidden="true" strokeWidth={1.75} /> My profile
					</Link>
				</DropdownMenuItem>
				<DropdownMenuItem asChild>
					<Link href="/bookmark">
						<Bookmark aria-hidden="true" strokeWidth={1.75} /> My bookmarks
					</Link>
				</DropdownMenuItem>
				<DropdownMenuItem asChild>
					<Link href="/tenants">
						<UsersRound aria-hidden="true" strokeWidth={1.75} /> Tenants
					</Link>
				</DropdownMenuItem>
				<DropdownMenuSeparator />
				<DropdownMenuItem onClick={handleLogout} className="es-menu-danger">
					<LogOut aria-hidden="true" strokeWidth={1.75} /> Log out
				</DropdownMenuItem>
			</DropdownMenuContent>
		</DropdownMenu>
	);
}
