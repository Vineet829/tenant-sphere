import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { setLogout } from "@/lib/redux/features/auth/authSlice";

const initialState = {
	page: 1,
};

const postSlice = createSlice({
	name: "post",
	initialState,
	reducers: {
		setCurrentPage: (state, action: PayloadAction<number>) => {
			state.page = action.payload;
		},
	},
	// The next person to sign in starts without anyone else's search or page.
	extraReducers: (builder) => {
		builder.addCase(setLogout, () => initialState);
	},
});


export const { setCurrentPage } = postSlice.actions;
export default postSlice.reducer;
