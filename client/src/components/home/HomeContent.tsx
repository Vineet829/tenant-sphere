import { ArrowRight, Check, Home, MessageCircle, ShieldCheck, Wrench } from "lucide-react";
import { Instrument_Serif, Manrope } from "next/font/google";
import Image from "next/image";
import Link from "next/link";
import community from "@/../public/assets/images/tenant-community.jpg";
import styles from "./home.module.css";

const sans = Manrope({ subsets: ["latin"], variable: "--home-sans", display: "swap" });
const editorial = Instrument_Serif({
	subsets: ["latin"],
	weight: "400",
	style: ["normal", "italic"],
	variable: "--home-editorial",
	display: "swap",
	adjustFontFallback: false,
	fallback: ["Georgia", "serif"],
});

const features = [
	{
		number: "01",
		icon: Wrench,
		label: "MAINTENANCE",
		title: "A fix. A follow-up. Sorted.",
		description: "Report a repair, follow its progress, and rate the people who help.",
	},
	{
		number: "02",
		icon: MessageCircle,
		label: "COMMUNITY",
		title: "More than next-door neighbours.",
		description: "Share updates, join conversations, and find people in your building.",
	},
	{
		number: "03",
		icon: ShieldCheck,
		label: "YOUR SPACE",
		title: "The little details, together.",
		description: "Keep your apartment details in one place and report community concerns.",
	},
];

// Photo: malwina nogaj / Unsplash. Commercial use permitted by the Unsplash License.
// https://unsplash.com/photos/modern-apartment-buildings-with-glass-balconies-and-trees-YcoBpYHdqCg
export default function HomeContent() {
	return (
		<div className={`${styles.home} ${sans.variable} ${editorial.variable}`}>
			<a href="#home-main" className={styles.skipLink}>Skip to content</a>
			<div className={styles.container}>
				<header className={styles.header}>
					<Link href="/" className={styles.wordmark} aria-label="Tenant Sphere home">
						tenant<span>sphere</span><span className={styles.wordmarkDot}>.</span>
					</Link>
					<a href="#everyday" className={styles.headerExplore}>Everyday living, made easier</a>
					<nav className={styles.navigation} aria-label="Main navigation">
						<Link href="/login" prefetch={false} className={styles.loginButton}>Log in <ArrowRight size={16} aria-hidden="true" /></Link>
					</nav>
				</header>

				<main id="home-main" className={styles.main} tabIndex={-1}>
					<section className={styles.hero} aria-labelledby="home-title">
						<div className={styles.heroCopy}>
							<p className={styles.eyebrow}><span aria-hidden="true" /> A LITTLE CLOSER TO HOME</p>
							<h1 id="home-title">Good neighbours.<br /><em>Better living.</em></h1>
							<p className={styles.introduction}>Home is more than your front door.</p>
							<p className={styles.description}>Get things fixed. Keep conversations going. Bring the everyday life of your building together, in one thoughtful space.</p>
							<div className={styles.heroActions}>
								<Link href="/register" prefetch={false} className={styles.primaryButton}>Join your community <ArrowRight size={19} aria-hidden="true" /></Link>
							</div>
							<div className={styles.heroNote}><Home size={17} strokeWidth={1.5} aria-hidden="true" /><span>For the people who make a building a home.</span></div>
						</div>

						<div className={styles.visual}>
							<div className={styles.photoFrame}>
								<Image src={community} alt="Apartment balconies beside a leafy shared courtyard" fill priority placeholder="blur" sizes="(max-width: 912px) 92vw, (max-width: 1320px) 48vw, 720px" className={styles.photo} />
								<div className={styles.photoShade} />
								<div className={styles.photoCaption}><span>ROOM TO BELONG</span><span>Life happens here.</span></div>
							</div>
							<section className={styles.maintenanceCard} aria-labelledby="maintenance-title">
								<div className={styles.cardTop}><span className={styles.cardIcon}><Wrench size={17} strokeWidth={1.7} aria-hidden="true" /></span><span>THE LITTLE THINGS, TAKEN CARE OF</span></div>
								<h2 id="maintenance-title">From reported to resolved.</h2>
								<ol className={styles.progressSteps} aria-label="Maintenance request stages">
									{["Reported", "In progress", "Resolved"].map((label, index) => (
										<li key={label} className={styles.progressStep}>
											<div className={styles.stepTrack}><span className={styles.stepNode}><Check size={13} strokeWidth={2.5} aria-hidden="true" /></span>{index < 2 && <span className={styles.stepConnector} aria-hidden="true"><span /></span>}</div>
											<span className={styles.stepLabel}>{label}</span>
										</li>
									))}
								</ol>
								<p className={styles.cardNote}>Know where things stand, every step of the way.</p>
							</section>
						</div>
					</section>

					<section id="everyday" className={styles.features} aria-label="A simpler everyday">
						{features.map(({ number, icon: Icon, label, title, description }) => (
							<article key={number} className={styles.feature}>
								<div className={styles.featureLabel}><Icon size={17} strokeWidth={1.7} aria-hidden="true" /><span>{label}</span><span className={styles.featureNumber}>{number}</span></div>
								<h2>{title}</h2>
								<p>{description}</p>
							</article>
						))}
					</section>
				</main>

				<footer className={styles.footer}>
					<p>© {new Date().getFullYear()} Tenant Sphere</p>
					<p>Good things start with good neighbours.</p>
					<Link href="/login" prefetch={false}>Already at home here? <span>Log in <ArrowRight size={14} aria-hidden="true" /></span></Link>
				</footer>
			</div>
		</div>
	);
}
