import {create} from "zustand";

const useGameStore = create((set) => ({
    messages: [],
    addMessage: (message) => set((state) => ({messages: [...state.messages, message]})),
    removeMessage: (index) => set((state) => ({messages: state.messages.filter((_, i) => i !== index)})),
}));

export default useGameStore