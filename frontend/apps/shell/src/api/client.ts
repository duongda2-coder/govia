import { createGoviaHttpClient } from "@govia/ui-kit";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";

export const httpClient = createGoviaHttpClient(API_BASE_URL);
