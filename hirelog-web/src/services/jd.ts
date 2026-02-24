// Mock Service for JD List

export interface JdItem {
    id: number;
    company: string;
    position: string;
    date: string;
    status: 'Analysis Complete' | 'Processing';
    tags: string[];
    reviewCount?: number;
}

export interface JdListResponse {
    content: JdItem[];
    totalPages: number;
    totalElements: number;
    pageable: {
        pageNumber: number;
        pageSize: number;
    };
}

const MOCK_DATA: JdItem[] = [
    { id: 1, company: 'KAKAO', position: 'Backend Engineer', date: '2026.02.05', status: 'Analysis Complete', tags: ['Java', 'Spring Boot'], reviewCount: 20 },
    { id: 2, company: 'Toss', position: 'Frontend Developer', date: '2026.02.04', status: 'Analysis Complete', tags: ['React', 'TypeScript'], reviewCount: 15 },
    { id: 3, company: 'Line', position: 'Data Scientist', date: '2026.02.03', status: 'Processing', tags: ['Python', 'TensorFlow'], reviewCount: 0 },
    { id: 4, company: 'Coupang', position: 'Product Designer', date: '2026.02.01', status: 'Analysis Complete', tags: ['Figma', 'UX/UI'], reviewCount: 5 },
    { id: 5, company: 'Baemin', position: 'DevOps Engineer', date: '2026.01.28', status: 'Analysis Complete', tags: ['AWS', 'Kubernetes'], reviewCount: 8 },
    { id: 6, company: 'Naver', position: 'iOS Developer', date: '2026.01.25', status: 'Analysis Complete', tags: ['Swift', 'UIKit'], reviewCount: 12 },
    { id: 7, company: 'Danggeun', position: 'Android Developer', date: '2026.01.20', status: 'Analysis Complete', tags: ['Kotlin', 'Jetpack'], reviewCount: 3 },
    { id: 8, company: 'Samsung within', position: 'Embedded Engineer', date: '2026.01.15', status: 'Processing', tags: ['C++', 'Linux'], reviewCount: 0 },
    { id: 9, company: 'LG CNS', position: 'Cloud Architect', date: '2026.01.10', status: 'Analysis Complete', tags: ['GCP', 'Azure'], reviewCount: 10 },
    { id: 10, company: 'SK Telecom', position: 'Network Engineer', date: '2026.01.05', status: 'Analysis Complete', tags: ['Cisco', 'Python'], reviewCount: 7 },
    { id: 11, company: 'KT', position: 'Security Analyst', date: '2026.01.01', status: 'Analysis Complete', tags: ['Security', 'Network'], reviewCount: 2 },
];

export const jdService = {
    getList: async (page: number = 0, size: number = 5, query: string = ''): Promise<JdListResponse> => {
        // Simulate API delay
        await new Promise(resolve => setTimeout(resolve, 500));

        const filtered = MOCK_DATA.filter(item =>
            item.company.toLowerCase().includes(query.toLowerCase()) ||
            item.position.toLowerCase().includes(query.toLowerCase())
        );

        const start = page * size;
        const end = start + size;
        const content = filtered.slice(start, end);

        return {
            content,
            totalPages: Math.ceil(filtered.length / size),
            totalElements: filtered.length,
            pageable: {
                pageNumber: page,
                pageSize: size
            }
        };
    }
};
