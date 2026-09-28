/* react-select, dressed in the interior tokens. */
const customStyles = {
	control: (provided: any, state: any) => ({
		...provided,
		minHeight: "var(--select-control-height, 48px)",
		paddingLeft: "5px",
		borderRadius: "var(--select-border-radius, 8px)",
		borderColor: state.isFocused ? "var(--es-accent)" : "var(--select-border-color)",
		backgroundColor: "var(--select-background-color)",
		color: "var(--select-text-color)",
		fontSize: "0.9375rem",
		cursor: "pointer",
		boxShadow: state.isFocused ? "0 0 0 3px var(--select-focus-ring, transparent)" : "none",
		transition: "border-color 0.16s, box-shadow 0.16s",
		"&:hover": {
			borderColor: state.isFocused ? "var(--es-accent)" : "var(--select-border-hover-color)",
		},
	}),
	valueContainer: (provided: any) => ({
		...provided,
		padding: "2px 10px",
	}),
	indicatorSeparator: () => ({
		display: "none",
	}),
	dropdownIndicator: (provided: any, state: any) => ({
		...provided,
		padding: "8px 14px 8px 8px",
		color: "var(--select-placeholder-color)",
		transform: state.selectProps.menuIsOpen ? "rotate(180deg)" : "none",
		transition: "transform 0.2s, color 0.16s",
		"&:hover": {
			color: "var(--select-text-color)",
		},
	}),
	option: (provided: any, state: any) => ({
		...provided,
		padding: "10px 12px",
		borderRadius: "6px",
		fontSize: "0.9375rem",
		cursor: "pointer",
		backgroundColor: state.isSelected
			? "var(--select-option-selected-background-color)"
			: state.isFocused
				? "var(--select-option-hover-background-color)"
				: "transparent",
		color: state.isSelected
			? "var(--select-option-selected-text-color, var(--select-option-text-color))"
			: "var(--select-option-text-color)",
		fontWeight: state.isSelected ? 650 : 500,
		"&:active": {
			backgroundColor: state.isSelected
				? "var(--select-option-selected-background-color)"
				: "var(--select-option-hover-background-color)",
		},
	}),
	input: (provided: any) => ({
		...provided,
		color: "var(--select-text-color)",
	}),
	placeholder: (provided: any) => ({
		...provided,
		color: "var(--select-placeholder-color, var(--select-text-color))",
		opacity: 1,
	}),
	singleValue: (provided: any) => ({
		...provided,
		color: "var(--select-value-text-color)",
		fontWeight: 500,
	}),
	menu: (provided: any) => ({
		...provided,
		zIndex: 30,
		marginTop: "6px",
		overflow: "hidden",
		border: "1px solid var(--es-line-strong)",
		borderRadius: "12px",
		backgroundColor: "var(--select-menu-background-color)",
		boxShadow: "var(--es-shadow-3)",
	}),
	menuList: (provided: any) => ({
		...provided,
		padding: "6px",
	}),
};

export default customStyles;
