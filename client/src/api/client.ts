import * as generatedApi from './generated';
import {AnonLinkData, AnonSignupData, SubscribeData} from './generated';
import {createClient} from "@hey-api/client-axios";

const customClient = createClient({
  baseURL: import.meta.env.VITE_YASS_API_URL,
  withCredentials: true,
  validateStatus: (status) => status >= 200 && status < 300,

});

const getAnonToken = () => {
  const stored = localStorage.getItem('anon-token-storage');
  if (!stored) return null;
  return JSON.parse(stored).state.token;
};

customClient.instance.interceptors.request.use((request) => {
  const token = getAnonToken()
  if (token) {
    request.headers['X-Anon-Token'] = token;
  }
  return request;
});

export const api = {
  whoami: () => generatedApi.whoami({client: customClient}),
  subscribe: (data: SubscribeData['body']) => generatedApi.subscribe({client: customClient, body: data}),
  anonSignup: (data: AnonSignupData['body']) => generatedApi.anonSignup({client: customClient, body: data}),
  anonLink: (data: AnonLinkData['body']) => generatedApi.anonLink({client: customClient, body: data}),
  connect: () => generatedApi.connect({client: customClient})
};