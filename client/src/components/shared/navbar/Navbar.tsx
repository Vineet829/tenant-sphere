import Wordmark from "@/components/estate/Wordmark";
import AuthAvatar from "@/components/shared/navbar/AuthAvatar";
import MobileNavbar from "./MobileNavbar";

export default function Navbar() {
	return (
		<header className="es-topbar">
			<Wordmark />
			<div className="es-topbar-actions">
				<AuthAvatar />
				<MobileNavbar />
			</div>
		</header>
	);
}
