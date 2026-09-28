"use client";
import { AppStore, makeStore } from "@/lib/redux/store";
import React, { useEffect, useRef } from "react";
import { setupListeners } from "@reduxjs/toolkit/query";
import { Provider } from "react-redux";

interface ProviderProps {
	children: React.ReactNode;
}


export default function ReduxProvider({ children }: ProviderProps) {
	const storeRef = useRef<AppStore>();
	if (!storeRef.current) {
		storeRef.current = makeStore();
	}
	useEffect(() => setupListeners(storeRef.current!.dispatch), []);
	return <Provider store={storeRef.current}>{children}</Provider>;
}
