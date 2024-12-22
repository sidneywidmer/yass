import * as generatedApi from './generated';
import {createClient} from "@hey-api/client-axios";
import {AnonLinkData, AnonSignupData, SubscribeData} from "./generated";

const customClient = createClient({
  baseURL: import.meta.env.VITE_YASS_API_URL,
  withCredentials: true,
});

export const api = {
  whoami: () => generatedApi.whoami({client: customClient}),
  subscribe: (data: SubscribeData['body']) => generatedApi.subscribe({client: customClient, body: data}),
  anonSignup: (data: AnonSignupData['body']) => generatedApi.anonSignup({client: customClient, body: data}),
  anonLink: (data: AnonLinkData['body']) => generatedApi.anonLink({client: customClient, body: data}),
  connect: () => generatedApi.connect({client: customClient})
};