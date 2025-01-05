"use client";

import { AuthFormHeader, LoginForm } from "@/components/forms/auth";
import OauthButtons from "@/components/shared/OauthButtons";
import GuestButton from "@/components/shared/GuestButton"; 
import { useRedirectIfAuthenticated } from "@/hooks";
import { useForm } from "react-hook-form";
import { useLoginUserMutation } from "@/lib/redux/features/auth/authApiSlice"; 
import { toast } from "react-toastify";
import { useRouter } from "next/navigation";
import { useAppDispatch } from "@/lib/redux/hooks/typedHooks";
import { setAuth } from "@/lib/redux/features/auth/authSlice"; 
import { extractErrorMessage } from "@/utils"
export default function LoginPage() {
	
	useRedirectIfAuthenticated();
	const [loginUser, { isLoading }] = useLoginUserMutation();
	const router = useRouter();
	const dispatch = useAppDispatch();
	const { setValue } = useForm();

	const handleGuestLogin = async () => {
		const guestCredentials = {
			email: "testuser@gmail.com",
			password: "example1234",
		};

		try {
			
			await loginUser(guestCredentials).unwrap();
			dispatch(setAuth());
			toast.success("Login Successful");
			router.push("/welcome");
		} catch (error) {
			const errorMessage = extractErrorMessage(error);
			toast.error(errorMessage || "An error occurred");
		}
	};

	return (
		<div>
			<AuthFormHeader
				title="Login to your account"
				staticText="Don't have an account?"
				linkText="Register Here"
				linkHref="/register"
			/>
			<div className="mt-7 sm:mx-auto sm:w-full sm:max-w-[480px]">
				<div className="bg-lightGrey dark:bg-deepBlueGrey rounded-xl px-6 py-12 shadow sm:rounded-lg sm:px-12 md:rounded-3xl">
					<LoginForm />
					<div className="flex-center mt-5 space-x-2">
						<div className="bg-richBlack dark:bg-platinum h-px flex-1"></div>
						<span className="dark:text-platinum px-2 text-sm">Or</span>
						<div className="bg-richBlack dark:bg-platinum h-px flex-1"></div>
					</div>
					<OauthButtons />
					<GuestButton onClick={handleGuestLogin} /> {/* Use the updated GuestButton */}
				</div>
			</div>
		</div>
	);
}
