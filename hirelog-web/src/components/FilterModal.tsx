import { useState } from 'react';

interface Props {
  onClose: () => void;
  sortBy: string;
  setSortBy: (value: string) => void;
}

const FilterModal = ({ onClose, sortBy, setSortBy }: Props) => {
  const [brandName, setBrandName] = useState('');
  const [positionName, setPositionName] = useState('');
  const [categoryName, setCategoryName] = useState('');

  return (
    <div className="fixed inset-0 bg-black/40 flex justify-center items-center z-50">
      <div className="bg-white w-[700px] rounded-2xl p-10 shadow-2xl relative">

        {/* 닫기 버튼 */}
        <button
          onClick={onClose}
          className="absolute right-6 top-6 text-gray-400 text-xl"
        >
          ✕
        </button>

        <h2 className="text-xl font-bold mb-10">
          상세 필터 설정
        </h2>

        {/* 기업 / 브랜드 */}
        <div className="mb-8">
          <p className="text-sm text-gray-400 mb-2">
            기업명 / 브랜드
          </p>
          <input
            value={brandName}
            onChange={(e) => setBrandName(e.target.value)}
            placeholder="검색할 이름을 입력하세요..."
            className="w-full bg-gray-100 rounded-xl px-4 py-3 outline-none"
          />
        </div>

        {/* 직무 카테고리 */}
        <div className="mb-8">
          <p className="text-sm text-gray-400 mb-2">
            직무 카테고리
          </p>
          <input
            value={categoryName}
            onChange={(e) => setCategoryName(e.target.value)}
            placeholder="카테고리를 입력하세요..."
            className="w-full bg-gray-100 rounded-xl px-4 py-3 outline-none"
          />
        </div>

        {/* 포지션 직접 입력 */}
        <div className="mb-10">
          <p className="text-sm text-gray-400 mb-2">
            포지션 / 역할
          </p>
          <input
            value={positionName}
            onChange={(e) => setPositionName(e.target.value)}
            placeholder="포지션을 입력하세요..."
            className="w-full bg-gray-100 rounded-xl px-4 py-3 outline-none"
          />
        </div>

        {/* 정렬 기준 */}
        <div className="flex items-center gap-6 border-t pt-6">

          <span className="text-sm text-gray-500">
            정렬 기준
          </span>

          <button
            onClick={() => setSortBy('CREATED_AT_DESC')}
            className={`px-4 py-2 rounded-full ${sortBy === 'CREATED_AT_DESC'
                ? 'bg-black text-white'
                : 'bg-gray-200'
              }`}
          >
            최신순
          </button>

          <button
            onClick={() => setSortBy('CREATED_AT_ASC')}
            className={`px-4 py-2 rounded-full ${sortBy === 'CREATED_AT_ASC'
                ? 'bg-black text-white'
                : 'bg-gray-200'
              }`}
          >
            오래된순
          </button>

          <button
            onClick={() => setSortBy('SAVE_COUNT_DESC')}
            className={`px-4 py-2 rounded-full ${sortBy === 'SAVE_COUNT_DESC'
                ? 'bg-black text-white'
                : 'bg-gray-200'
              }`}
          >
            저장순
          </button>
        </div>

        {/* 하단 버튼 */}
        <div className="flex justify-end gap-4 mt-10">
          <button
            onClick={onClose}
            className="text-gray-400"
          >
            필터 초기화
          </button>

          <button
            onClick={onClose}
            className="bg-[#1e293b] text-white px-6 py-3 rounded-full shadow-lg"
          >
            필터 적용하기
          </button>
        </div>
      </div>
    </div>
  );
};

export default FilterModal;
