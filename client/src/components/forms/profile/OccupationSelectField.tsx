import { occupationOptions } from "@/constants";
import { useGetUserProfileQuery } from "@/lib/redux/features/users/usersApiSlice";
import { TProfileSchema } from "@/lib/validationSchemas";
import { Occupation } from "@/types";
import dynamic from "next/dynamic";
import React, { useEffect } from "react";
import { Control, Controller, UseFormSetValue } from "react-hook-form";
import Select from "react-select";
import customStyles from "../selectStyles";

const ClientOnly = dynamic<{ children: React.ReactNode }>(
	() => Promise.resolve(({ children }) => <>{children}</>),
	{ ssr: false, loading: () => <span className="es-select-placeholder es-skeleton" /> },
);

function isOccupation(value: any): value is Occupation {
	return ["mason", "carpenter", "plumber", "roofer", "painter", "electrician", "hvac", "tenant"].includes(value);
}

interface OccupationSelectFieldProps {
	setValue: UseFormSetValue<TProfileSchema>;
	control: Control<TProfileSchema>;
}

export default function OccupationSelectField({ setValue, control }: OccupationSelectFieldProps) {
	const { data: profileData } = useGetUserProfileQuery();
	const profile = profileData?.profile;

	useEffect(() => {
		if (profile?.gender) {
			const occupationValue = occupationOptions.find((option) => option.value === profile.occupation);

			if (occupationValue && isOccupation(occupationValue.value)) {
				setValue("occupation", occupationValue.value);
			}
		}
	}, [profile, setValue]);

	return (
		<div className="es-field">
			<div className="es-field-head">
				<label htmlFor="occupation" className="es-label">
					Occupation
				</label>
			</div>
			<div className="es-select">
				<ClientOnly>
					<Controller
						control={control}
						name="occupation"
						render={({ field }) => (
							<Select
								{...field}
								inputId="occupation"
								options={occupationOptions}
								value={occupationOptions.find((option) => option.value === field.value)}
								onChange={(option) => field.onChange(option?.value)}
								instanceId="occupation-select"
								styles={customStyles}
							/>
						)}
					/>
				</ClientOnly>
			</div>
		</div>
	);
}
