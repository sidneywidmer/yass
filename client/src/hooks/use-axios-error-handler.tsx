import {AxiosError} from 'axios'
import {useTranslation} from 'react-i18next'
import {useErrorStore} from "@/store/error.ts";
import {useCallback} from "react";
export const useAxiosErrorHandler = () => {
  const {t} = useTranslation()
  const addError = useErrorStore(error => error.addError)

  return useCallback((error: AxiosError) => {
    if (!error.response) {
      addError({
        title: t('errors.network.title'),
        description: t('errors.network.description')
      })
      return
    }

    switch (error.response.status) {
      case 400:
        // We'll handle 400 errors manually in each case, no "catch all" needed
        break

      case 401:
        break

      case 403:
        addError({
          title: t('errors.forbidden.title'),
          description: t('errors.forbidden.description')
        })
        break

      case 422:
        break

      case 500:
        addError({
          title: t('errors.server.title'),
          description: t('errors.server.description')
        })
        break

      default:
        addError({
          title: t('errors.unknown.title'),
          description: t('errors.unknown.description')
        })
    }
  }, [addError]);
}
