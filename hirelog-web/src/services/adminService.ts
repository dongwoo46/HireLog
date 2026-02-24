import { apiClient } from '../utils/apiClient';
import type {
    AdminPagedResult,
    BrandListView,
    CompanyView,
    MemberSummaryView,
    AdminReviewView
} from '../types/admin';

export const adminService = {
    /* ---------------------- 멤버 관리 ---------------------- */

    getAllMembers: async (page = 0, size = 20): Promise<AdminPagedResult<MemberSummaryView>> => {
        const response = await apiClient.get('/admin/member', {
            params: { page, size }
        });
        return response.data;
    },

    suspendMember: async (memberId: number): Promise<void> => {
        await apiClient.post(`/admin/member/${memberId}/suspend`);
    },

    activateMember: async (memberId: number): Promise<void> => {
        await apiClient.post(`/admin/member/${memberId}/activate`);
    },

    deleteMember: async (memberId: number): Promise<void> => {
        await apiClient.delete(`/admin/member/${memberId}`);
    },

    /* ---------------------- JD(Job Summary) 관리 ---------------------- */

    // 일반 검색과 동일하지만 관리자 전용 필터링이나 액션이 추가될 수 있음
    // 현재는 기존 search API를 활용하거나 필요시 추가
    activateJob: async (id: number): Promise<void> => {
        await apiClient.patch(`/job-summary/${id}/activate`);
    },

    deactivateJob: async (id: number): Promise<void> => {
        await apiClient.patch(`/job-summary/${id}/deactivate`);
    },

    /* ---------------------- 브랜드 관리 ---------------------- */

    getAllBrands: async (page = 0, size = 20): Promise<AdminPagedResult<BrandListView>> => {
        const response = await apiClient.get('/brand', {
            params: { page, size }
        });
        return response.data;
    },

    verifyBrand: async (brandId: number): Promise<void> => {
        await apiClient.patch(`/brand/${brandId}/verify`);
    },

    rejectBrand: async (brandId: number): Promise<void> => {
        await apiClient.patch(`/brand/${brandId}/reject`);
    },

    activateBrand: async (brandId: number): Promise<void> => {
        await apiClient.patch(`/brand/${brandId}/activate`);
    },

    deactivateBrand: async (brandId: number): Promise<void> => {
        await apiClient.patch(`/brand/${brandId}/deactivate`);
    },

    /* ---------------------- 기업 관리 ---------------------- */

    getAllCompanies: async (page = 0, size = 20): Promise<AdminPagedResult<CompanyView>> => {
        const response = await apiClient.get('/companies', {
            params: { page, size }
        });
        return response.data;
    },

    activateCompany: async (companyId: number): Promise<void> => {
        await apiClient.patch(`/companies/${companyId}/activate`);
    },

    deactivateCompany: async (companyId: number): Promise<void> => {
        await apiClient.patch(`/companies/${companyId}/deactivate`);
    },

    /* ---------------------- 사용자 요청 관리 ---------------------- */

    getAllUserRequests: async (page = 0, size = 20, status?: string): Promise<AdminPagedResult<any>> => {
        const response = await apiClient.get('/user-requests/admin', {
            params: { page, size, status }
        });
        return response.data;
    },

    updateUserRequestStatus: async (requestId: number, status: string): Promise<void> => {
        await apiClient.patch(`/user-requests/${requestId}/status`, { status });
    },

    /* ---------------------- 리뷰 관리 ---------------------- */

    getAllReviews: async (page = 0, size = 20): Promise<AdminPagedResult<AdminReviewView>> => {
        const response = await apiClient.get('/job-summary/review/admin', {
            params: { page, size }
        });
        return response.data;
    },

    deleteReview: async (reviewId: number): Promise<void> => {
        await apiClient.delete(`/job-summary/review/${reviewId}`);
    },

    restoreReview: async (reviewId: number): Promise<void> => {
        await apiClient.patch(`/job-summary/review/${reviewId}/restore`);
    },

    /* ---------------------- 관리자 인증 (Deprecated) ---------------------- */
    // 역할 기반 인증(RBAC)으로 전환됨에 따라 더 이상 사용되지 않음
    /*
    verifyAdminPassword: async (password: string): Promise<void> => {
        await apiClient.post('/admin/job-summary/verify', { password });
    },
    */
};
