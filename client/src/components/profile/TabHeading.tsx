import type { ReactNode } from "react";

type TabHeadingProps = {
	title: string;
	note?: string;
	action?: ReactNode;
};

export default function TabHeading({ title, note, action }: TabHeadingProps) {
	return (
		<div className="es-tabhead">
			<div>
				<h2>{title}</h2>
				{note && <p>{note}</p>}
			</div>
			{action}
		</div>
	);
}
