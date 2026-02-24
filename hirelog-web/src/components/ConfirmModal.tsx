import React, { useEffect, useState } from 'react';
import { TbAlertCircle } from 'react-icons/tb';

interface ConfirmModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm: () => void;
  onCancel: () => void;
  variant?: 'danger' | 'primary';
}

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
  isOpen,
  title,
  message,
  confirmText = '확인',
  cancelText = '취소',
  onConfirm,
  onCancel,
  variant = 'primary'
}) => {
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setIsVisible(true);
    } else {
      const timer = setTimeout(() => setIsVisible(false), 200);
      return () => clearTimeout(timer);
    }
  }, [isOpen]);

  if (!isOpen && !isVisible) return null;

  return (
    <div className={`fixed inset-0 z-[100] flex items-center justify-center p-6 transition-all duration-300 ${
      isOpen ? 'opacity-100' : 'opacity-0'
    }`}>
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-[#0f172a]/60 backdrop-blur-sm"
        onClick={onCancel}
      />

      {/* Modal Card */}
      <div className={`
        relative w-full max-w-sm bg-white rounded-[2.5rem] shadow-2xl overflow-hidden transition-all duration-300
        ${isOpen ? 'scale-100 translate-y-0' : 'scale-95 translate-y-4'}
      `}>
        <div className="p-8 pb-4 flex flex-col items-center text-center">
          <div className={`
            w-16 h-16 rounded-2xl flex items-center justify-center mb-6
            ${variant === 'danger' ? 'bg-red-50 text-red-500' : 'bg-[#89cbb6]/10 text-[#89cbb6]'}
          `}>
            <TbAlertCircle size={32} />
          </div>
          
          <h3 className="text-xl font-black text-gray-900 mb-2 italic tracking-tight">{title}</h3>
          <p className="text-gray-500 font-medium leading-relaxed">{message}</p>
        </div>

        <div className="p-8 pt-6 flex gap-3">
          <button 
            onClick={onCancel}
            className="flex-1 py-4 bg-gray-50 hover:bg-gray-100 text-gray-400 font-bold rounded-2xl transition-all"
          >
            {cancelText}
          </button>
          <button 
            onClick={onConfirm}
            className={`
              flex-1 py-4 font-black rounded-2xl transition-all shadow-lg active:scale-95
              ${variant === 'danger' 
                ? 'bg-red-500 text-white shadow-red-500/20 hover:bg-red-600' 
                : 'bg-[#0f172a] text-white shadow-[#0f172a]/20 hover:scale-[1.02]'}
            `}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
};
