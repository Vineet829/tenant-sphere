"use client";

import * as Dialog from "@radix-ui/react-dialog";
import { ChevronRight, Star, UsersRound, Wrench, X, type LucideIcon } from "lucide-react";
import Link from "next/link";
import type { CSSProperties, ReactNode } from "react";
import { useState } from "react";
import { useGetUserQuery } from "@/lib/redux/features/auth/authApiSlice";
import { estateEditorial, estateSans } from "./fonts";
import PersonAvatar from "./PersonAvatar";
import ProfileActivity from "./ProfileActivity";
import RatingStars from "./RatingStars";

export type PersonFact = {
	icon: LucideIcon;
	label: string;
	value: ReactNode;
};

type PersonCardProps = {
	name: string;
	username: string;
	slug: string;
	avatar?: string | null;
	bio?: string | null;
	/** Small label beside the name, such as a trade. */
	badge?: string;
	facts: PersonFact[];
	summaryFacts: PersonFact[];
	profileType?: "tenant" | "technician";
	rating?: number;
	index?: number;
};

function PersonFacts({ facts, detailed = false }: { facts: PersonFact[]; detailed?: boolean }) {
	return (
		<dl className={detailed ? "es-person-details" : "es-facts"}>
			{facts.map(({ icon: Icon, label, value }) => (
				<div className={detailed ? "es-person-detail" : "es-fact"} key={label}>
					<dt>
						<Icon aria-hidden="true" strokeWidth={1.75} />
						{label}
					</dt>
					<dd>{value}</dd>
				</div>
			))}
		</dl>
	);
}

export default function PersonCard({ name, username, slug, avatar, bio, badge, facts, summaryFacts, profileType = "tenant", rating, index = 0 }: PersonCardProps) {
	const [open, setOpen] = useState(false);
	const displayName = name || username;
	const isTechnician = profileType === "technician";
	const ProfileIcon = isTechnician ? Wrench : UsersRound;
	const ratingHref = `/add-rating?username=${encodeURIComponent(username)}`;
	const { data: viewer } = useGetUserQuery();
	// Tenants rate technicians; guests' ratings would count against real people's work.
	const canRate = isTechnician && viewer?.occupation === "tenant" && !viewer.is_guest;
	const cardFacts = isTechnician
		? [...summaryFacts, { icon: Star, label: "Rating", value: <RatingStars value={rating} /> }]
		: summaryFacts;

	return (
		<Dialog.Root open={open} onOpenChange={setOpen}>
			<article className="es-card es-person" style={{ "--i": Math.min(index, 8) } as CSSProperties}>
				<header className="es-person-head">
					<PersonAvatar name={displayName} seed={username} src={avatar} size="lg" />
					<div className="es-person-id">
						<h3 className="es-person-name" title={displayName}>
							{displayName}
						</h3>
						<div className="es-person-meta">
							<p className="es-person-handle">@{username}</p>
							{badge && <span className="es-chip es-chip--accent">{badge}</span>}
						</div>
					</div>
				</header>

				<PersonFacts facts={cardFacts} />

				<footer className="es-person-foot">
					<Dialog.Trigger asChild>
						<button type="button" className="es-person-open" aria-label={`View ${displayName}'s profile`}>
							View profile
							<ChevronRight aria-hidden="true" />
						</button>
					</Dialog.Trigger>
					{canRate && (
						<Link href={ratingHref} className="es-btn es-btn--outline es-btn--sm es-person-rate" aria-label={`Rate ${displayName}`}>
							<Star aria-hidden="true" />
							Rate
						</Link>
					)}
				</footer>
			</article>

			<Dialog.Portal>
				<Dialog.Overlay className="es-person-backdrop" />
				<Dialog.Content className={`estate-shell es-person-dialog ${estateSans.variable} ${estateEditorial.variable}`}>
					<header className="es-person-dialog-head">
						<PersonAvatar name={displayName} seed={username} src={avatar} size="lg" className="es-person-dialog-avatar" />
						<div className="es-person-dialog-identity">
							<span className="es-person-dialog-type">
								<ProfileIcon aria-hidden="true" strokeWidth={1.75} />
								{isTechnician ? "Technician profile" : "Tenant profile"}
							</span>
							<Dialog.Title className="es-person-dialog-name">{displayName}</Dialog.Title>
							<div className="es-person-dialog-meta">
								<Dialog.Description className="es-person-dialog-handle">@{username}</Dialog.Description>
								{badge && <span className="es-chip es-chip--accent">{badge}</span>}
							</div>
						</div>
						<Dialog.Close asChild>
							<button type="button" className="es-icon-button es-person-dialog-close" aria-label="Close profile">
								<X aria-hidden="true" />
							</button>
						</Dialog.Close>
					</header>

					<div className="es-person-dialog-scroll">
						<div className="es-person-dialog-body">
							<div className="es-person-overview">
								<section className="es-person-dialog-about">
									<h3 className="es-person-dialog-section">About</h3>
									<p className="es-prose" data-empty={!bio}>{bio || "No bio shared yet."}</p>
								</section>
								<section className="es-person-background">
									<h3 className="es-person-dialog-section">{isTechnician ? "Background" : "Home & background"}</h3>
									<PersonFacts facts={facts} detailed />
								</section>
							</div>
							{open && <ProfileActivity slug={slug} profileType={profileType} />}
						</div>
					</div>

					<footer className="es-person-dialog-foot">
						<Dialog.Close asChild>
							<button type="button" className="es-btn es-btn--ghost">
								Close
							</button>
						</Dialog.Close>
						{canRate && (
							<Dialog.Close asChild>
								<Link href={ratingHref} className="es-btn es-btn--primary" aria-label={`Rate ${displayName}`}>
									<Star aria-hidden="true" />
									Write a review
								</Link>
							</Dialog.Close>
						)}
					</footer>
				</Dialog.Content>
			</Dialog.Portal>
		</Dialog.Root>
	);
}
