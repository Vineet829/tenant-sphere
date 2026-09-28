import React from "react";

interface ProfileItemProps {
	icon: React.ReactNode;
	label: string;
	value?: string | null;
}

export const ProfileItem: React.FC<ProfileItemProps> = ({ icon, label, value }) => (
	<div className="es-defrow">
		<dt>
			<span className="es-defrow-icon" aria-hidden="true">
				{icon}
			</span>
			{label}
		</dt>
		<dd data-empty={!value}>{value || "Not shared"}</dd>
	</div>
);
