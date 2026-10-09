"use client";

import {
	setCurrentPage,
	setSearchTerm,
	setTechCurrentPage,
	setTechSearchTerm,
} from "@/lib/redux/features/users/userSlice";
import { useAppDispatch, useAppSelector } from "@/lib/redux/hooks/typedHooks";
import { Search } from "lucide-react";
import React from "react";

type UsersSearchProps = {
	placeholder?: string;
	label?: string;
	// Tenants and technicians keep their own search, so one directory's filter never carries into the other.
	variant?: "tenant" | "technician";
};

const UsersSearch = ({
	placeholder = "Search by name or username",
	label = "Search the directory",
	variant = "tenant",
}: UsersSearchProps) => {
	const dispatch = useAppDispatch();
	const technician = variant === "technician";
	const searchTerm = useAppSelector((state) => (technician ? state.user.techSearchTerm : state.user.searchTerm));

	const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
		dispatch(technician ? setTechCurrentPage(1) : setCurrentPage(1));
		dispatch(technician ? setTechSearchTerm(event.target.value) : setSearchTerm(event.target.value));
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
