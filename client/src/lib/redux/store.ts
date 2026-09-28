import { rootReducer } from "@/lib/redux/features/rootReducer";
import { configureStore, createListenerMiddleware } from "@reduxjs/toolkit";
import { baseApiSlice } from "./features/api/baseApiSlice";
import { setLogout } from "./features/auth/authSlice";

export const makeStore = () => {
	const sessionListener = createListenerMiddleware();
	sessionListener.startListening({
		actionCreator: setLogout,
		effect: (_, api) => { api.dispatch(baseApiSlice.util.resetApiState()); },
	});
	return configureStore({
		reducer: rootReducer,
		middleware: (getDefaultMiddleware) =>
			getDefaultMiddleware().prepend(sessionListener.middleware).concat(baseApiSlice.middleware),
		devTools: process.env.NODE_ENV !== "production",
	});
};


export type AppStore = ReturnType<typeof makeStore>;
export type RootState = ReturnType<AppStore["getState"]>;
export type AppDispatch = AppStore["dispatch"];
