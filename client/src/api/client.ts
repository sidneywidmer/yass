import * as generatedApi from './generated';
import {
  AnonLinkData,
  AnonSignupData, ChooseTrumpRequest,
  CreateCustomGameRequest,
  JoinGameRequest, MessageRequest, PingSeatRequest,
  PlayCardRequest, SchiebeRequest,
  SubscribeData, WeisenRequest
} from './generated';
import {createClient} from "@hey-api/client-axios";

const customClient = createClient({
  baseURL: import.meta.env.VITE_YASS_API_URL,
  withCredentials: true,
  validateStatus: (status) => status >= 200 && status < 300,
  throwOnError: true
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

// TODO: The openapi spec is very different for auth and game, clean up is needed
export const api = {
  // Auth
  whoami: () => generatedApi.whoami({client: customClient}),
  subscribe: (data: SubscribeData['body']) => generatedApi.subscribe({client: customClient, body: data}),
  anonSignup: (data: AnonSignupData['body']) => generatedApi.anonSignup({client: customClient, body: data}),
  anonLogout: () => generatedApi.anonLogout({client: customClient}),
  anonLink: (data: AnonLinkData['body']) => generatedApi.anonLink({client: customClient, body: data}),
  connect: () => generatedApi.connect({client: customClient}),

  // Game
  createGame: (data: CreateCustomGameRequest) => generatedApi.postGameCreate({client: customClient, body: data}),
  joinGame: (data: JoinGameRequest) => generatedApi.postGameJoin({client: customClient, body: data}),
  playCard: (data: PlayCardRequest) => generatedApi.postGamePlay({client: customClient, body: data}),
  chooseTrump: (data: ChooseTrumpRequest) => generatedApi.postGameTrump({client: customClient, body: data}),
  weisen: (data: WeisenRequest) => generatedApi.postGameWeisen({client: customClient, body: data}),
  schiebe: (data: SchiebeRequest) => generatedApi.postGameSchiebe({client: customClient, body: data}),
  pingSeat: (data: PingSeatRequest) => generatedApi.postGamePing({client: customClient, body: data}),

  // Admin
  analyzeGame: (code: string) => generatedApi.getAdminAnalyzeGameByCode({
    client: customClient,
    path: {code: code},  // For URL parameters
    params: {}  // For query parameters (empty in this case)
  }),
  submitMessage: (data: MessageRequest) => generatedApi.postAdminMessage({
    client: customClient,
    body: data
  }),
};