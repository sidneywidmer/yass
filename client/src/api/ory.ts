import { Configuration, FrontendApi } from '@ory/client'

const ory = new FrontendApi(
  new Configuration({
    basePath: import.meta.env.VITE_ORY_API_URL,
    baseOptions: {
      withCredentials: true
    }
  })
)

export { ory }