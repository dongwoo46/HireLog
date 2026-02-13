import React, { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { TbX } from 'react-icons/tb';

interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    title?: string;
    children: React.ReactNode;
    maxWidth?: string;
}

export const Modal: React.FC<ModalProps> = ({
    isOpen,
    onClose,
    title,
    children,
    maxWidth = 'max-w-2xl'
}) => {
    const [isVisible, setIsVisible] = useState(false);

    useEffect(() => {
        if (isOpen) {
            setIsVisible(true);
            document.body.style.overflow = 'hidden';
        } else {
            const timer = setTimeout(() => setIsVisible(false), 300);
            document.body.style.overflow = 'unset';
            return () => clearTimeout(timer);
        }
    }, [isOpen]);

    if (!isOpen && !isVisible) return null;

    return createPortal(
        <div className={`fixed inset-0 z-[100] flex items-center justify-center p-4 transition-all duration-300 ${isOpen ? 'opacity-100' : 'opacity-0'
            }`}>
            {/* Backdrop */}
            <div
                className="absolute inset-0 bg-[#0f172a]/60 backdrop-blur-sm transition-opacity"
                onClick={onClose}
            />

            {/* Modal Card */}
            <div className={`
        relative w-full ${maxWidth} bg-white rounded-[2.5rem] shadow-2xl overflow-hidden transition-all duration-500 ease-out flex flex-col max-h-[90vh]
        ${isOpen ? 'scale-100 translate-y-0' : 'scale-95 translate-y-8'}
      `}>
                {/* Header */}
                <div className="flex items-center justify-between px-8 py-6 border-b border-gray-100 bg-white/50 backdrop-blur-md sticky top-0 z-10">
                    <h3 className="text-xl font-bold text-gray-900 tracking-tight">{title}</h3>
                    <button
                        onClick={onClose}
                        className="p-2 rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-900 transition-colors"
                    >
                        <TbX size={24} />
                    </button>
                </div>

                {/* Scrollable Content */}
                <div className="p-8 overflow-y-auto custom-scrollbar">
                    {children}
                </div>
            </div>
        </div>,
        document.body
    );
};
