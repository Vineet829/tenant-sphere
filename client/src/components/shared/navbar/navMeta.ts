import {
	Bookmark,
	ClipboardPen,
	Flag,
	Home,
	PenLine,
	UserRound,
	UsersRound,
	Wrench,
	type LucideIcon,
} from "lucide-react";

export type NavGroupKey = "community" | "you" | "support";

export const NAV_GROUPS: { key: NavGroupKey; label: string }[] = [
	{ key: "community", label: "Community" },
	{ key: "you", label: "Your space" },
	{ key: "support", label: "Get help" },
];

/** Presentation for each route in `leftNavLinks`; the routes themselves stay in constants. */
export const NAV_META: Record<string, { icon: LucideIcon; label: string; group: NavGroupKey | "cta" }> = {
	"/welcome": { icon: Home, label: "Home", group: "community" },
	"/tenants": { icon: UsersRound, label: "Tenants", group: "community" },
	"/technicians": { icon: Wrench, label: "Technicians", group: "community" },
	"/bookmark": { icon: Bookmark, label: "Bookmarks", group: "community" },
	"/profile": { icon: UserRound, label: "Profile", group: "you" },
	"/report-issue": { icon: ClipboardPen, label: "Report an issue", group: "support" },
	"/report-tenant": { icon: Flag, label: "Report a tenant", group: "support" },
	"/add-post": { icon: PenLine, label: "Create a post", group: "cta" },
};
