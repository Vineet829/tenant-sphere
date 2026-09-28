import type { LucideIcon } from "lucide-react";
import type { CSSProperties, ReactNode } from "react";
import PersonAvatar from "./PersonAvatar";

export type PersonFact = {
	icon: LucideIcon;
	label: string;
	value: ReactNode;
};

type PersonCardProps = {
	name: string;
	username: string;
	avatar?: string | null;
	bio?: string | null;
	/** Small label beside the name, such as a trade. */
	badge?: string;
	facts: PersonFact[];
	footer?: ReactNode;
	index?: number;
};

export default function PersonCard({ name, username, avatar, bio, badge, facts, footer, index = 0 }: PersonCardProps) {
	return (
		<article className="es-card es-person" style={{ "--i": Math.min(index, 8) } as CSSProperties}>
			<header className="es-person-head">
				<PersonAvatar name={name || username} seed={username} src={avatar} size="lg" />
				<div className="es-person-id">
					<h3 className="es-person-name" title={name}>
						{name}
					</h3>
					<p className="es-person-handle">@{username}</p>
				</div>
				{badge && <span className="es-chip es-chip--accent">{badge}</span>}
			</header>

			<p className="es-person-bio" data-empty={!bio}>
				{bio || "No bio yet."}
			</p>

			<dl className="es-facts">
				{facts.map(({ icon: Icon, label, value }) => (
					<div className="es-fact" key={label}>
						<dt>
							<Icon aria-hidden="true" strokeWidth={1.75} />
							{label}
						</dt>
						<dd>{value}</dd>
					</div>
				))}
			</dl>

			{footer && <footer className="es-person-foot">{footer}</footer>}
		</article>
	);
}
