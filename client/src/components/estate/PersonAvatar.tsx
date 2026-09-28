"use client";

import * as AvatarPrimitive from "@radix-ui/react-avatar";
import { UserRound } from "lucide-react";

type AvatarSize = "xs" | "sm" | "md" | "lg" | "xl";

interface PersonAvatarProps {
	/** Full name when we have it, username otherwise. Used for the initials. */
	name?: string;
	/** A stable identity, normally the username, so a person keeps one colour everywhere. */
	seed?: string;
	src?: string | null;
	size?: AvatarSize;
	className?: string;
}

export function initialsOf(name = "") {
	const parts = name
		.replace(/^@/, "")
		.split(/[\s_.-]+/)
		.filter(Boolean);
	if (parts.length === 0) return "";
	const first = parts[0][0] ?? "";
	const second = parts.length > 1 ? parts[parts.length - 1][0] : (parts[0][1] ?? "");
	return `${first}${second}`.toUpperCase();
}

export function toneOf(seed = "") {
	let hash = 0;
	for (const character of seed.toLowerCase()) {
		hash = (hash * 31 + character.charCodeAt(0)) >>> 0;
	}
	return hash % 5;
}

export default function PersonAvatar({ name = "", seed, src, size = "md", className = "" }: PersonAvatarProps) {
	const initials = initialsOf(name);

	return (
		<AvatarPrimitive.Root
			className={`es-avatar es-avatar--${size} ${className}`.trim()}
			data-tone={initials ? toneOf(seed || name) : "none"}
			aria-hidden="true"
		>
			{src ? <AvatarPrimitive.Image src={src} alt="" /> : null}
			<AvatarPrimitive.Fallback delayMs={src ? 400 : 0}>
				{initials || <UserRound strokeWidth={1.75} />}
			</AvatarPrimitive.Fallback>
		</AvatarPrimitive.Root>
	);
}
