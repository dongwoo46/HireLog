import { useEffect, useMemo, useRef, useState } from 'react';
import { TbMessageCircle, TbRobot, TbSend, TbUser } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { Modal } from '../common/Modal';
import { getRagErrorMessage, isRagRateLimitError, ragService } from '../../services/ragService';
import { useRagChatStore } from '../../store/ragChatStore';
import type { RagIntent } from '../../types/rag';

interface RagChatModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const INTENT_LABEL: Record<RagIntent, string> = {
  DOCUMENT_SEARCH: '\uBB38\uC11C \uAC80\uC0C9',
  EXPERIENCE_ANALYSIS: '\uACBD\uD5D8 \uBD84\uC11D',
  STATISTICS: '\uD1B5\uACC4 \uBD84\uC11D',
  SUMMARY: '\uC694\uC57D',
};

const EXAMPLE_QUESTIONS = [
  '\uBC31\uC5D4\uB4DC \uC2E0\uC785 \uACF5\uACE0\uC5D0\uC11C \uC790\uC8FC \uB098\uC624\uB294 \uAE30\uC220\uC2A4\uD0DD \uC54C\uB824\uC918',
  '\uB0B4\uAC00 \uC800\uC7A5\uD55C \uACF5\uACE0 \uAE30\uC900\uC73C\uB85C \uC9C0\uC6D0 \uC804\uB7B5 \uCD94\uCC9C\uD574\uC918',
  '\uBA74\uC811 \uB2E8\uACC4\uC5D0\uC11C \uB5A8\uC5B4\uC9C0\uB294 \uBE44\uC728\uC774 \uB192\uC740 \uAD6C\uAC04\uC774 \uC5B4\uB514\uC57C?',
];

export function RagChatModal({ isOpen, onClose }: RagChatModalProps) {
  const messages = useRagChatStore((state) => state.messages);
  const ensureInitialized = useRagChatStore((state) => state.ensureInitialized);
  const addUserMessage = useRagChatStore((state) => state.addUserMessage);
  const addAssistantMessage = useRagChatStore((state) => state.addAssistantMessage);

  const [question, setQuestion] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);

  const canSubmit = useMemo(() => question.trim().length > 0 && !isSubmitting, [question, isSubmitting]);

  useEffect(() => {
    if (!isOpen) return;
    ensureInitialized();
  }, [isOpen, ensureInitialized]);

  useEffect(() => {
    if (!listRef.current) return;
    listRef.current.scrollTop = listRef.current.scrollHeight;
  }, [messages, isSubmitting]);

  const submitQuestion = async (rawQuestion: string) => {
    const trimmed = rawQuestion.trim();
    if (!trimmed || isSubmitting) return;

    setQuestion('');
    setIsSubmitting(true);
    addUserMessage(trimmed);

    try {
      const answer = await ragService.query({ question: trimmed });
      addAssistantMessage(answer.answer, answer);
    } catch (error) {
      if (isRagRateLimitError(error)) {
        toast.warn('\uC624\uB298 \uC9C8\uBB38 3\uD68C\uB97C \uBAA8\uB450 \uC0AC\uC6A9\uD588\uC5B4\uC694. \uB0B4\uC77C \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694.');
      } else {
        toast.error(getRagErrorMessage(error));
      }

      addAssistantMessage('\uC694\uCCAD\uC744 \uCC98\uB9AC\uD558\uC9C0 \uBABB\uD588\uC5B4\uC694. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={'\uCC44\uC6A9 \uB3C4\uC6B0\uBBF8'} maxWidth="max-w-4xl">
      <div className="space-y-4">
        <div className="rounded-2xl border border-[#4CDFD5]/20 bg-gradient-to-r from-[#ecfffd] to-[#f7fffe] p-4">
          <p className="text-sm font-semibold text-gray-700">{'\uC9C8\uBB38 \uC608\uC2DC'}</p>
          <div className="mt-3 flex flex-wrap gap-2">
            {EXAMPLE_QUESTIONS.map((example) => (
              <button
                key={example}
                type="button"
                onClick={() => submitQuestion(example)}
                disabled={isSubmitting}
                className="rounded-full border border-[#4CDFD5]/30 bg-white px-3 py-1.5 text-xs font-semibold text-[#1d7f7a] transition hover:bg-[#f0fffd] disabled:cursor-not-allowed disabled:opacity-60"
              >
                {example}
              </button>
            ))}
          </div>
        </div>

        <div ref={listRef} className="max-h-[48vh] space-y-3 overflow-y-auto rounded-2xl border border-gray-100 bg-[#FAFBFC] p-4">
          {messages.map((message) => (
            <div key={message.id} className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}>
              <div
                className={`max-w-[88%] rounded-2xl px-4 py-3 text-sm shadow-sm ${
                  message.role === 'user' ? 'bg-[#4CDFD5] text-[#083a37]' : 'border border-gray-200 bg-white text-gray-800'
                }`}
              >
                <div className="mb-1 flex items-center gap-1 text-[11px] font-semibold opacity-70">
                  {message.role === 'user' ? <TbUser size={14} /> : <TbRobot size={14} />}
                  {message.role === 'user' ? '\uB098' : '\uCC44\uC6A9 \uB3C4\uC6B0\uBBF8'}
                </div>
                <p className="whitespace-pre-wrap leading-relaxed">{message.content}</p>

                {message.ragAnswer && message.role === 'assistant' && (
                  <div className="mt-3 space-y-2 border-t border-gray-100 pt-3 text-xs">
                    <p className="font-semibold text-gray-500">
                      {'\uC758\uB3C4'}: <span className="text-gray-700">{INTENT_LABEL[message.ragAnswer.intent]}</span>
                    </p>
                    {!!message.ragAnswer.sources?.length && (
                      <div className="space-y-1">
                        <p className="font-semibold text-gray-500">{'\uCD9C\uCC98'}</p>
                        <ul className="space-y-1 text-gray-600">
                          {message.ragAnswer.sources.slice(0, 3).map((source) => (
                            <li key={source.id} className="rounded-lg bg-gray-50 px-2 py-1">
                              <a
                                href={`/jd/${source.id}`}
                                className="underline decoration-gray-300 underline-offset-2 transition hover:text-[#276db8]"
                              >
                                #{source.id}{' '}
                                {source.brandName
                                  ? `${source.brandName} / ${source.positionName}`
                                  : source.positionName}
                              </a>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          ))}

          {isSubmitting && (
            <div className="flex justify-start">
              <div className="rounded-2xl border border-gray-200 bg-white px-4 py-3 text-sm text-gray-500">
                {'\uB2F5\uBCC0\uC744 \uC0DD\uC131\uD558\uACE0 \uC788\uC5B4\uC694...'}
              </div>
            </div>
          )}
        </div>

        <form
          onSubmit={(event) => {
            event.preventDefault();
            submitQuestion(question);
          }}
          className="flex items-end gap-2"
        >
          <div className="relative flex-1">
            <TbMessageCircle className="pointer-events-none absolute left-3 top-3 text-gray-400" size={18} />
            <textarea
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              rows={2}
              maxLength={500}
              placeholder={'\uC9C8\uBB38\uC744 \uC785\uB825\uD558\uC138\uC694. (\uCD5C\uB300 500\uC790)'}
              className="w-full resize-none rounded-2xl border border-gray-200 py-2.5 pl-10 pr-3 text-sm outline-none transition focus:border-[#4CDFD5]"
            />
          </div>
          <button
            type="submit"
            disabled={!canSubmit}
            className="inline-flex h-[42px] items-center gap-1 rounded-xl bg-[#4CDFD5] px-4 text-sm font-bold text-[#08403d] transition hover:brightness-95 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <TbSend size={16} />
            {'\uC804\uC1A1'}
          </button>
        </form>
      </div>
    </Modal>
  );
}
