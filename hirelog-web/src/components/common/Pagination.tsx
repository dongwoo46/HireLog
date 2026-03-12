import React from 'react';
import { TbChevronLeft, TbChevronRight } from 'react-icons/tb';

interface PaginationProps {
    currentPage: number;
    totalPages: number;
    onPageChange: (page: number) => void;
}

export const Pagination: React.FC<PaginationProps> = ({
    currentPage,
    totalPages,
    onPageChange,
}) => {
    if (totalPages <= 1) return null;

    const getPageNumbers = () => {
        const pages = [];
        const maxVisiblePages = 5;

        let startPage = Math.max(0, currentPage - Math.floor(maxVisiblePages / 2));
        let endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);

        if (endPage - startPage + 1 < maxVisiblePages) {
            startPage = Math.max(0, endPage - maxVisiblePages + 1);
        }

        for (let i = startPage; i <= endPage; i++) {
            pages.push(i);
        }
        return pages;
    };

    return (
        <div className="flex justify-center items-center gap-2 mt-20">
            <button
                onClick={() => onPageChange(Math.max(0, currentPage - 1))}
                disabled={currentPage === 0}
                className="w-10 h-10 flex items-center justify-center rounded-lg border border-gray-100 hover:bg-gray-50 disabled:opacity-20 disabled:cursor-not-allowed transition-all"
            >
                <TbChevronLeft size={18} className="text-gray-400" />
            </button>

            {getPageNumbers().map((page) => (
                <button
                    key={page}
                    onClick={() => onPageChange(page)}
                    className={`w-10 h-10 flex items-center justify-center rounded-lg font-bold text-sm transition-all ${currentPage === page
                        ? 'bg-[#4CDFD5] text-white shadow-sm'
                        : 'text-gray-400 hover:bg-gray-50'
                        }`}
                >
                    {page + 1}
                </button>
            ))}

            <button
                onClick={() => onPageChange(Math.min(totalPages - 1, currentPage + 1))}
                disabled={currentPage >= totalPages - 1}
                className="w-10 h-10 flex items-center justify-center rounded-lg border border-gray-100 hover:bg-gray-50 disabled:opacity-20 disabled:cursor-not-allowed transition-all"
            >
                <TbChevronRight size={18} className="text-gray-400" />
            </button>
        </div>
    );
};
