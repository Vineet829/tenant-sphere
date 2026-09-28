import SegmentedField from "@/components/estate/SegmentedField";
import { useGetUserProfileQuery } from "@/lib/redux/features/users/usersApiSlice";
import { TProfileSchema } from "@/lib/validationSchemas";
import { useEffect } from "react";
import { Control, Controller, UseFormSetValue } from "react-hook-form";

type Gender = "male" | "female" | "other";

function isGender(value: any): value is Gender {
	return ["male", "female", "other"].includes(value);
}

const genderOptions = [
	{ value: "male", label: "Male" },
	{ value: "female", label: "Female" },
	{ value: "other", label: "Other" },
];

interface GenderSelectFieldProps {
	setValue: UseFormSetValue<TProfileSchema>;
	control: Control<TProfileSchema>;
}

export default function GenderSelectField({ setValue, control }: GenderSelectFieldProps) {
	const { data: profileData } = useGetUserProfileQuery();
	const profile = profileData?.profile;

	useEffect(() => {
		if (profile?.gender) {
			const genderValue = genderOptions.find((option) => option.value === profile.gender);

			if (genderValue && isGender(genderValue.value)) {
				setValue("gender", genderValue.value);
			}
		}
	}, [profile, setValue]);

	return (
		<Controller
			control={control}
			name="gender"
			render={({ field }) => (
				<SegmentedField
					legend="Gender"
					name="gender"
					options={genderOptions}
					value={field.value}
					onChange={field.onChange}
					onBlur={field.onBlur}
					inputRef={field.ref}
					plain
				/>
			)}
		/>
	);
}
