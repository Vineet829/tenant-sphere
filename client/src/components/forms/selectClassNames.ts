import type { ClassNamesConfig } from "react-select";

/** Keep react-select's keyboard and positioning logic; Tailwind owns its appearance. */
const selectClassNames: ClassNamesConfig<{ value: string; label: string }, false> = {
	control: ({ isFocused }) => `es-select-control${isFocused ? " es-select-control--focused" : ""}`,
	valueContainer: () => "es-select-value",
	indicatorSeparator: () => "hidden",
	dropdownIndicator: ({ selectProps }) =>
		`es-select-indicator${selectProps.menuIsOpen ? " es-select-indicator--open" : ""}`,
	option: ({ isSelected, isFocused }) =>
		`es-select-option${isSelected ? " es-select-option--selected" : isFocused ? " es-select-option--focused" : ""}`,
	input: () => "es-select-input",
	placeholder: () => "es-select-hint",
	singleValue: () => "es-select-selected",
	menu: () => "es-select-menu",
	menuList: () => "es-select-options",
	noOptionsMessage: () => "px-[12px] py-[8px] text-center text-estate-text-3",
};

export default selectClassNames;
