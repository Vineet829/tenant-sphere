"use client";
import { estateSans } from "@/components/estate/fonts";
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Check, Monitor, Moon, Sun } from "lucide-react";
import { useTheme } from "next-themes";

const themeOptions = [
	{ value: "light", label: "Light", icon: Sun },
	{ value: "dark", label: "Dark", icon: Moon },
	{ value: "system", label: "System", icon: Monitor },
];

export default function ThemeSwitcher() {
	const { theme, setTheme } = useTheme();

	return (
		<DropdownMenu>
			<DropdownMenuTrigger asChild>
				<button type="button" className="es-icon-button es-theme-toggle" aria-label="Change theme">
					<Sun className="es-theme-sun" aria-hidden="true" strokeWidth={1.75} />
					<Moon className="es-theme-moon" aria-hidden="true" strokeWidth={1.75} />
				</button>
			</DropdownMenuTrigger>
			<DropdownMenuContent align="end" sideOffset={10} className={`estate-popover ${estateSans.variable}`}>
				<p className="es-menu-label">Appearance</p>
				{themeOptions.map(({ value, label, icon: Icon }) => (
					<DropdownMenuItem key={value} onClick={() => setTheme(value)} data-active={theme === value}>
						<Icon aria-hidden="true" strokeWidth={1.75} />
						{label}
						{theme === value && <Check aria-hidden="true" style={{ marginLeft: "auto" }} />}
					</DropdownMenuItem>
				))}
			</DropdownMenuContent>
		</DropdownMenu>
	);
}
