import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { HiringStage, HiringStageResult } from '../types/jobSummary';

type TabType = 'SAVED' | 'APPLY';

interface MyJobsFilterState {
  activeTab: TabType;
  brandQuery: string;
  searchBrandName: string;
  stage?: HiringStage;
  result?: HiringStageResult;
  setActiveTab: (tab: TabType) => void;
  setBrandQuery: (query: string) => void;
  setSearchBrandName: (name: string) => void;
  setStage: (stage?: HiringStage) => void;
  setResult: (result?: HiringStageResult) => void;
}

export const useMyJobsFilterStore = create<MyJobsFilterState>()(
  persist(
    (set) => ({
      activeTab: 'SAVED',
      brandQuery: '',
      searchBrandName: '',
      stage: undefined,
      result: undefined,
      setActiveTab: (activeTab) => set({ activeTab }),
      setBrandQuery: (brandQuery) => set({ brandQuery }),
      setSearchBrandName: (searchBrandName) => set({ searchBrandName }),
      setStage: (stage) => set({ stage }),
      setResult: (result) => set({ result }),
    }),
    {
      name: 'my-jobs-filter-store',
      partialize: (state) => ({
        activeTab: state.activeTab,
        brandQuery: state.brandQuery,
        searchBrandName: state.searchBrandName,
        stage: state.stage,
        result: state.result,
      }),
    },
  ),
);
