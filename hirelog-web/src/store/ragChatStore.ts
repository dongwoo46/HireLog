import { create } from 'zustand';
import type { RagAnswer, RagChatMessage } from '../types/rag';

interface RagChatState {
  messages: RagChatMessage[];
  ensureInitialized: () => void;
  addUserMessage: (content: string) => void;
  addAssistantMessage: (content: string, ragAnswer?: RagAnswer) => void;
  clearMessages: () => void;
}

const makeMessageId = () => `${Date.now()}-${Math.random().toString(16).slice(2)}`;

const INITIAL_ASSISTANT_MESSAGE =
  '\uC548\uB155\uD558\uC138\uC694. \uCC44\uC6A9 \uACF5\uACE0 \uC9C8\uBB38 \uB3C4\uC6B0\uBBF8\uC785\uB2C8\uB2E4. \uAD81\uAE08\uD55C \uB0B4\uC6A9\uC744 \uC790\uC720\uB86D\uAC8C \uC9C8\uBB38\uD574 \uC8FC\uC138\uC694.';

export const useRagChatStore = create<RagChatState>((set, get) => ({
  messages: [],

  ensureInitialized: () => {
    if (get().messages.length > 0) return;
    set({
      messages: [
        {
          id: makeMessageId(),
          role: 'assistant',
          content: INITIAL_ASSISTANT_MESSAGE,
        },
      ],
    });
  },

  addUserMessage: (content) =>
    set((state) => ({
      messages: [
        ...state.messages,
        {
          id: makeMessageId(),
          role: 'user',
          content,
        },
      ],
    })),

  addAssistantMessage: (content, ragAnswer) =>
    set((state) => ({
      messages: [
        ...state.messages,
        {
          id: makeMessageId(),
          role: 'assistant',
          content,
          ragAnswer,
        },
      ],
    })),

  clearMessages: () => set({ messages: [] }),
}));
