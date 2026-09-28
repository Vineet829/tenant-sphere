import type { CSSProperties } from "react";

function Bone({ w, h = 12, className = "" }: { w: string | number; h?: number; className?: string }) {
	const style = { "--bone-width": typeof w === "number" ? `${w}px` : w, "--bone-height": `${h}px` } as CSSProperties;
	return <span className={`es-skeleton h-[var(--bone-height)] w-[var(--bone-width)] ${className}`.trim()} style={style} />;
}

function Status() {
	return <span className="es-sr">Loading…</span>;
}

export function FeedSkeleton({ rows = 4 }: { rows?: number }) {
	return (
		<div className="es-loading es-loading--rows" role="status">
			<Status />
			{Array.from({ length: rows }, (_, index) => (
				<div key={index} className="es-loading-block">
					<div className="es-loading-row">
						<Bone w={28} h={28} className="es-skeleton--circle" />
						<Bone w={120} />
						<Bone w={70} />
					</div>
					<Bone w="58%" h={18} />
					<Bone w="92%" />
					<Bone w="74%" />
					<div className="es-loading-row">
						<Bone w={64} h={30} className="es-skeleton--pill" />
						<Bone w={96} h={30} className="es-skeleton--pill" />
					</div>
				</div>
			))}
		</div>
	);
}

export function PeopleSkeleton({ cards = 6 }: { cards?: number }) {
	return (
		<div className="es-loading-grid es-people-loading" role="status">
			<Status />
			{Array.from({ length: cards }, (_, index) => (
				<div key={index} className="es-loading-block">
					<div className="es-loading-row">
						<Bone w={52} h={52} className="es-skeleton--circle" />
						<div className="grid flex-1 gap-[8px]">
							<Bone w="62%" h={14} />
							<Bone w="40%" />
						</div>
					</div>
					<div className="es-people-loading-facts">
						{[0, 1].map((fact) => (
							<div key={fact}>
								<Bone w="48%" h={10} />
								<Bone w="82%" h={14} />
							</div>
						))}
					</div>
					<Bone w={112} h={16} />
				</div>
			))}
		</div>
	);
}

export function CardsSkeleton({ cards = 3 }: { cards?: number }) {
	return (
		<div className="es-loading-grid" role="status">
			<Status />
			{Array.from({ length: cards }, (_, index) => (
				<div key={index} className="es-loading-block">
					<div className="es-loading-row">
						<Bone w={84} h={22} className="es-skeleton--pill" />
						<Bone w={104} h={22} className="es-skeleton--pill" />
					</div>
					<Bone w="70%" h={18} />
					<Bone w="96%" />
					<Bone w="60%" />
				</div>
			))}
		</div>
	);
}

export function DetailSkeleton() {
	return (
		<div className="es-loading" role="status">
			<Status />
			<Bone w={150} h={14} />
			<Bone w="64%" h={40} />
			<div className="es-loading-row">
				<Bone w={36} h={36} className="es-skeleton--circle" />
				<Bone w={140} />
				<Bone w={90} />
			</div>
			<div className="es-loading-block">
				<Bone w="96%" />
				<Bone w="90%" />
				<Bone w="72%" />
			</div>
		</div>
	);
}
