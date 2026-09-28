"use client";

import { setSearchTerm } from "@/lib/redux/features/users/userSlice";
import { useAppDispatch, useAppSelector } from "@/lib/redux/hooks/typedHooks";
import { Search } from "lucide-react";
import React from "react";

type UsersSearchProps = {
	placeholder?: string;
	label?: string;
};

const UsersSearch = ({
	placeholder = "Search by name or username",
	label = "Search the directory",
}: UsersSearchProps) => {
	const dispatch = useAppDispatch();
	const searchTerm = useAppSelector((state) => state.user.searchTerm);

	const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
		dispatch(setSearchTerm(event.target.value));
	};

	return (
		<label className="es-search">
			<span className="es-sr">{label}</span>
			<Search aria-hidden="true" strokeWidth={1.75} />
			<input
				type="search"
				placeholder={placeholder}
				value={searchTerm}
				onChange={handleInputChange}
				autoComplete="off"
				spellCheck={false}
			/>
		</label>
	);
};

export default UsersSearch;
