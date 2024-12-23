import {create} from 'zustand'
import {toast} from 'sonner'
import {nanoid} from 'nanoid'

export interface ErrorNotification {
  id: string
  title: string
  description: string
}

interface ErrorStore {
  errors: ErrorNotification[]
  addError: (error: Omit<ErrorNotification, 'id'>) => void
  removeError: (id: string) => void
}

export const useErrorStore = create<ErrorStore>((set, get) => ({
  errors: [],
  addError: (error) => {
    const id = nanoid()
    set((state) => ({errors: [...state.errors, {...error, id}]}))

    toast.error(error.title, {
      description: error.description,
      onDismiss: () => get().removeError(id)
    })
  },
  removeError: (id) => {
    set((state) => ({
      errors: state.errors.filter((error) => error.id !== id)
    }))
  }
}))