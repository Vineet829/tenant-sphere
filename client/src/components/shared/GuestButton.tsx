"use client";

import React from "react";

interface GuestButtonProps {
	onClick: () => void;
}

export default function GuestButton({ onClick }: GuestButtonProps) {
	return (
		<button
			className="mt-5 w-full bg-blue-600 text-white hover:bg-blue-700 dark:bg-blue-800 dark:text-white dark:hover:bg-blue-700 transition duration-200 py-2 rounded-lg shadow-lg font-medium"
			onClick={onClick}
		>
			Login as Guest
		</button>
	);
}
