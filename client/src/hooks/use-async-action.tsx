import { useState, useCallback } from 'react'

export function useAsyncAction<T extends any[], R>(
  asyncFn: (...args: T) => Promise<R>
) {
  const [isLoading, setIsLoading] = useState(false)
  const [hasError, setHasError] = useState(false)

  const execute = useCallback(async (...args: T): Promise<R> => {
    setIsLoading(true)
    setHasError(false)
    try {
      const result = await asyncFn(...args)
      return result
    } catch (error) {
      setHasError(true)
      throw error // Rethrow so original error handling still works
    } finally {
      setIsLoading(false)
    }
  }, [asyncFn])

  const reset = useCallback(() => {
    setHasError(false)
  }, [])

  return { execute, isLoading, hasError, reset }
}