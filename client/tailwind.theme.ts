/** Shared visual tokens and motion for the public, entry and community interfaces. */
export const tenantTheme = {
	screens: {
		"rail-hidden": {
			raw: "(max-width: 1534.8px)",
		},
		"nav-compact": {
			raw: "(max-width: 1227.6px)",
		},
		"estate-mobile": {
			raw: "(max-width: 766.8px)",
		},
		"motion-reduced": {
			raw: "(prefers-reduced-motion: reduce)",
		},
		"page-tablet": {
			raw: "(max-width: 912px)",
		},
		"page-small": {
			raw: "(max-width: 504px)",
		},
		"home-short": {
			raw: "(min-width: 1321.2px) and (max-height: 984px)",
		},
		"home-compact": {
			raw: "(max-width: 1320px)",
		},
		"home-nav-compact": {
			raw: "(max-width: 576px)",
		},
		"home-mobile": {
			raw: "(max-width: 468px)",
		},
		"auth-short": {
			raw: "(min-width: 1081.2px) and (max-height: 1020px)",
		},
		"auth-compact": {
			raw: "(max-width: 1380px)",
		},
		"auth-tablet": {
			raw: "(max-width: 1080px)",
		},
		"auth-mobile": {
			raw: "(max-width: 624px)",
		},
		"auth-small": {
			raw: "(max-width: 432px)",
		},
		"auth-aside-compact": {
			raw: "(min-width: 1081.2px) and (max-width: 1380px)",
		},
	},
	colors: {
		estate: {
			canvas: "var(--es-canvas)",
			surface: "var(--es-surface)",
			"surface-2": "var(--es-surface-2)",
			"surface-3": "var(--es-surface-3)",
			sunken: "var(--es-sunken)",
			line: "var(--es-line)",
			"line-strong": "var(--es-line-strong)",
			"field-line": "var(--es-field-line)",
			text: "var(--es-text)",
			"text-2": "var(--es-text-2)",
			"text-3": "var(--es-text-3)",
			accent: "var(--es-accent)",
			"accent-hover": "var(--es-accent-hover)",
			"accent-ink": "var(--es-accent-ink)",
			"accent-text": "var(--es-accent-text)",
			"accent-soft": "var(--es-accent-soft)",
			"accent-line": "var(--es-accent-line)",
			ok: "var(--es-ok)",
			"ok-soft": "var(--es-ok-soft)",
			warn: "var(--es-warn)",
			"warn-soft": "var(--es-warn-soft)",
			danger: "var(--es-danger)",
			"danger-soft": "var(--es-danger-soft)",
			info: "var(--es-info)",
			"info-soft": "var(--es-info-soft)",
			star: "var(--es-star)",
			ring: "var(--es-ring)",
			"tone-0-bg": "var(--es-tone-0-bg)",
			"tone-0-fg": "var(--es-tone-0-fg)",
			"tone-1-bg": "var(--es-tone-1-bg)",
			"tone-1-fg": "var(--es-tone-1-fg)",
			"tone-2-bg": "var(--es-tone-2-bg)",
			"tone-2-fg": "var(--es-tone-2-fg)",
			"tone-3-bg": "var(--es-tone-3-bg)",
			"tone-3-fg": "var(--es-tone-3-fg)",
			"tone-4-bg": "var(--es-tone-4-bg)",
			"tone-4-fg": "var(--es-tone-4-fg)",
		},
		brand: {
			petrol: "#102d32",
			ivory: "#f0f3e7",
			jade: "#bfdaaa",
			"jade-hover": "#d1e7be",
			"jade-ink": "#15362e",
		},
	},
	fontFamily: {
		"estate-sans": ["var(--es-sans)"],
		"estate-serif": ["var(--es-serif)"],
		"home-sans": ["var(--home-sans)", "sans-serif"],
		"home-editorial": ["var(--home-editorial)", "Georgia", "serif"],
		"auth-sans": ["var(--auth-sans)", "sans-serif"],
		"auth-editorial": ["var(--auth-editorial)", "Georgia", "serif"],
	},
	borderRadius: {
		"estate-xs": "var(--es-radius-xs)",
		"estate-sm": "var(--es-radius-sm)",
		estate: "var(--es-radius)",
		"estate-lg": "var(--es-radius-lg)",
	},
	boxShadow: {
		"estate-1": "var(--es-shadow-1)",
		"estate-2": "var(--es-shadow-2)",
		"estate-3": "var(--es-shadow-3)",
	},
	keyframes: {
		"home-arrive": {
			from: {
				opacity: "0",
				transform: "translateY(14px)",
			},
			to: {
				opacity: "1",
				transform: "translateY(0)",
			},
		},
		"home-photoSettle": {
			from: {
				transform: "scale(1.035)",
			},
			to: {
				transform: "scale(1)",
			},
		},
		"home-connectSteps": {
			from: {
				transform: "scaleX(0)",
			},
			to: {
				transform: "scaleX(1)",
			},
		},
		"home-stepComplete": {
			from: {
				background: "#3b5b58",
				color: "#3b5b58",
				transform: "scale(.86)",
			},
			to: {
				background: "#bfdaaa",
				color: "#19382a",
				transform: "scale(1)",
			},
		},
		"auth-authArrive": {
			from: {
				opacity: "0",
				transform: "translateY(12px)",
			},
			to: {
				opacity: "1",
				transform: "translateY(0)",
			},
		},
		"auth-authSpin": {
			to: {
				transform: "rotate(360deg)",
			},
		},
		"auth-aside-arrive": {
			from: {
				opacity: "0",
				transform: "translateY(9px)",
			},
			to: {
				opacity: "1",
				transform: "translateY(0)",
			},
		},
		"auth-aside-connect": {
			to: {
				"stroke-dashoffset": "0",
			},
		},
		"es-rise": {
			from: {
				opacity: "0",
				transform: "translateY(12px)",
			},
			to: {
				opacity: "1",
				transform: "translateY(0)",
			},
		},
		"es-fade": {
			from: {
				opacity: "0",
			},
			to: {
				opacity: "1",
			},
		},
		"es-shimmer": {
			from: {
				"background-position": "200% 0",
			},
			to: {
				"background-position": "-200% 0",
			},
		},
		"es-spin": {
			to: {
				transform: "rotate(360deg)",
			},
		},
		"es-grow": {
			from: {
				transform: "scaleX(0)",
			},
			to: {
				transform: "scaleX(1)",
			},
		},
		"es-pop": {
			from: {
				opacity: "0",
				transform: "scale(0.8)",
			},
			to: {
				opacity: "1",
				transform: "scale(1)",
			},
		},
		"es-person-dialog-in": {
			from: {
				opacity: "0",
				transform: "translate(-50%, -48%) scale(0.985)",
			},
			to: {
				opacity: "1",
				transform: "translate(-50%, -50%) scale(1)",
			},
		},
	},
	animation: {
		"home-copy-in": "home-arrive .65s ease-out both",
		"home-visual-in": "home-arrive .85s .12s ease-out both",
		"home-photo-in": "home-photoSettle 1.4s cubic-bezier(.2,.7,.2,1) both",
		"home-card-in": "home-arrive .7s .35s ease-out both",
		"home-step-in": "home-stepComplete .5s .8s backwards",
		"home-connect": "home-connectSteps .75s 1.1s both",
		"home-features-in": "home-arrive .75s .2s ease-out both",
		"auth-panel-in": "auth-authArrive .7s .12s ease-out both",
		"auth-spin": "auth-authSpin .8s linear infinite",
		"auth-aside-in": "auth-aside-arrive .65s ease-out both",
		"auth-connect": "auth-aside-connect 1.5s .35s ease-out forwards",
		"auth-feature-in": "auth-aside-arrive .65s .35s backwards",
		"auth-step-in": "auth-aside-arrive .6s .15s backwards",
		"estate-page-in": "es-rise 0.55s var(--es-ease) both",
		"estate-stagger-in": "es-rise 0.5s var(--es-ease) both",
		"estate-spin": "es-spin 0.7s linear infinite",
		"estate-tab-in": "es-rise 0.4s var(--es-ease) both",
		"estate-shimmer": "es-shimmer 1.6s linear infinite",
		"estate-loading-in": "es-fade 0.3s both",
		"estate-loader-spin": "es-spin 0.8s linear infinite",
		"estate-gate-in": "es-fade 0.3s 0.35s both",
		"estate-step-in": "es-pop 0.4s var(--es-ease) both",
		"estate-progress": "es-grow 0.6s var(--es-ease) both",
		"estate-confirm": "es-pop 0.25s var(--es-ease) both",
		"estate-dialog-in": "es-person-dialog-in 180ms var(--es-ease) both",
		"estate-meter": "es-grow 0.9s var(--es-ease) 0.3s both",
	},
};

/** Scoped variables also consumed by portalled menus and third-party controls. */
export const estateVariables = {
	"--es-sans": 'var(--estate-sans), ui-sans-serif, system-ui, -apple-system, "Segoe UI", sans-serif',
	"--es-serif": 'var(--estate-editorial), "Iowan Old Style", Georgia, serif',
	"--es-canvas": "#102d32",
	"--es-canvas-glow": "radial-gradient(ellipse at 5% 0%, #194249 0, transparent 52%)",
	"--es-surface": "#17393d",
	"--es-surface-2": "#1c4246",
	"--es-surface-3": "#244d4f",
	"--es-sunken": "#0e292e",
	"--es-line": "#29494d",
	"--es-line-strong": "#3d6061",
	"--es-field-line": "#5a7f7f",
	"--es-text": "#f0f3e7",
	"--es-text-2": "#b9cdc6",
	"--es-text-3": "#97b1aa",
	"--es-accent": "#bfdaaa",
	"--es-accent-hover": "#d1e7be",
	"--es-accent-ink": "#15362e",
	"--es-accent-text": "#c9e1b6",
	"--es-accent-soft": "rgb(191 218 170 / 0.1)",
	"--es-accent-line": "rgb(191 218 170 / 0.3)",
	"--es-ok": "#a8dcae",
	"--es-ok-soft": "rgb(168 220 174 / 0.12)",
	"--es-warn": "#ecd08a",
	"--es-warn-soft": "rgb(236 208 138 / 0.12)",
	"--es-danger": "#f4aa98",
	"--es-danger-soft": "rgb(244 170 152 / 0.11)",
	"--es-info": "#9dd0de",
	"--es-info-soft": "rgb(157 208 222 / 0.12)",
	"--es-star": "#e7c876",
	"--es-shadow-1": "0 1px 2px rgb(0 0 0 / 0.2)",
	"--es-shadow-2": "0 16px 38px -18px rgb(0 18 21 / 0.7), 0 2px 6px rgb(0 0 0 / 0.16)",
	"--es-shadow-3": "0 28px 64px -22px rgb(0 18 21 / 0.8), 0 4px 14px rgb(0 0 0 / 0.2)",
	"--es-sheen": "linear-gradient(180deg, rgb(240 243 231 / 0.045), transparent 38%)",
	"--es-ring": "rgb(191 218 170 / 0.2)",
	"--es-navigation": "transparent",
	"--es-topbar": "color-mix(in srgb, var(--es-canvas) 78%, transparent)",
	"--es-topbar-shadow": "none",
	"--es-nav-active": "var(--es-accent-soft)",
	"--es-nav-active-shadow": "none",
	"--es-cover-background":
		"radial-gradient(ellipse at 88% -40%, var(--es-accent-line), transparent 62%),\n\t\tradial-gradient(circle at 1px 1px, var(--es-line-strong) 1px, transparent 1.5px) 0 0 / 22px 22px,\n\t\tlinear-gradient(120deg, var(--es-surface-3), var(--es-surface-2))",
	"--es-cover-line": "var(--es-accent-line)",
	"--es-cover-mask": "linear-gradient(180deg, #000 40%, rgb(0 0 0 / 0.72))",
	"--es-tone-0-bg": "#2f5547",
	"--es-tone-0-fg": "#d3e9c1",
	"--es-tone-1-bg": "#27515a",
	"--es-tone-1-fg": "#bfe1e8",
	"--es-tone-2-bg": "#55492d",
	"--es-tone-2-fg": "#f0dca6",
	"--es-tone-3-bg": "#5a4239",
	"--es-tone-3-fg": "#f5cdbd",
	"--es-tone-4-bg": "#3f4262",
	"--es-tone-4-fg": "#d7d6f5",
	"--es-radius-xs": "6px",
	"--es-radius-sm": "8px",
	"--es-radius": "12px",
	"--es-radius-lg": "16px",
	"--es-topbar-h": "64px",
	"--es-sidebar-zoom": "1.2",
	"--es-sidebar-w": "calc(256px * var(--es-sidebar-zoom))",
	"--es-rail-w": "320px",
	"--es-ease": "cubic-bezier(0.2, 0.7, 0.2, 1)",
	"--select-background-color": "var(--es-sunken)",
	"--select-border-color": "var(--es-field-line)",
	"--select-text-color": "var(--es-text)",
	"--select-border-hover-color": "var(--es-text-3)",
	"--select-option-selected-background-color": "var(--es-accent)",
	"--select-option-selected-text-color": "var(--es-accent-ink)",
	"--select-option-background-color": "var(--es-surface)",
	"--select-option-text-color": "var(--es-text)",
	"--select-option-hover-background-color": "var(--es-surface-3)",
	"--select-value-text-color": "var(--es-text)",
	"--select-menu-background-color": "var(--es-surface)",
	"--select-control-height": "48px",
	"--select-border-radius": "8px",
	"--select-focus-ring": "var(--es-ring)",
	"--select-placeholder-color": "var(--es-text-3)",
};
