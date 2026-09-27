import { Check, Home, Mail, MessageCircle, Users, Wrench } from "lucide-react";
import styles from "./auth-aside.module.css";

const communityFeatures = [
	{ icon: Users, label: "Neighbours" },
	{ icon: MessageCircle, label: "Conversations" },
	{ icon: Wrench, label: "Maintenance" },
];

const gettingStarted = [
	{ number: "01", title: "Create your account", description: "A few details to make this space yours.", icon: Home },
	{ number: "02", title: "Check your inbox", description: "Follow your activation link to get started.", icon: Mail },
	{ number: "03", title: "Meet your community", description: "Log in, say hello, and join the conversation.", icon: Users },
];

export default function AuthAside({ mode }: { mode: "login" | "register" }) {
	if (mode === "register") {
		return (
			<aside className={`${styles.aside} ${styles.registerAside}`} aria-labelledby="auth-aside-title">
				<p className={styles.kicker}><span className={styles.kickerLine} aria-hidden="true" />SETTLE IN, STEP BY STEP</p>
				<h2 id="auth-aside-title" className={styles.registerTitle}>From new here<br />to <span>right at home.</span></h2>
				<ol className={styles.steps}>
					{gettingStarted.map(({ number, title, description, icon: Icon }) => (
						<li key={number} className={styles.step}>
							<span className={styles.stepNumber} aria-hidden="true">{number}</span>
							<div className={styles.stepCopy}>
								<h3>{title}</h3>
								<p>{description}</p>
								{number === "01" && <span className={styles.currentStep}>YOU’RE HERE</span>}
							</div>
							<Icon className={styles.stepIcon} size={20} strokeWidth={1.5} aria-hidden="true" />
						</li>
					))}
				</ol>
				<div className={styles.guideEnd}><Check size={14} aria-hidden="true" /><span>One account. Your whole community.</span></div>
			</aside>
		);
	}

	return (
		<aside className={`${styles.aside} ${styles.loginAside}`} aria-labelledby="auth-aside-title">
			<p className={styles.kicker}><span className={styles.kickerLine} aria-hidden="true" />THE EVERYDAY, CONNECTED</p>
			<div className={styles.communityDiagram}>
				<div className={styles.communityHub}>
					<span className={styles.hubIcon}><Home size={29} strokeWidth={1.4} aria-hidden="true" /></span>
					<span>Your community</span>
				</div>
				<svg className={styles.connections} viewBox="0 0 480 90" preserveAspectRatio="none" fill="none" aria-hidden="true">
					<path d="M240 0V28M80 90V48Q80 28 100 28H380Q400 28 400 48V90M240 28V90" />
					<path className={styles.connectionTrace} pathLength="1" d="M240 0V28M80 90V48Q80 28 100 28H380Q400 28 400 48V90M240 28V90" />
				</svg>
				<div className={styles.communityFeatures}>
					{communityFeatures.map(({ icon: Icon, label }) => (
						<div key={label} className={styles.communityFeature}>
							<span className={styles.featureIcon}><Icon size={24} strokeWidth={1.5} aria-hidden="true" /></span>
							<span>{label}</span>
						</div>
					))}
				</div>
			</div>
			<div className={styles.loginCopy}>
				<h2 id="auth-aside-title">Everything at home.<br /><span>A little more connected.</span></h2>
				<p>Pick up a conversation, follow a repair, or find a familiar face. Your community is right here.</p>
			</div>
		</aside>
	);
}
