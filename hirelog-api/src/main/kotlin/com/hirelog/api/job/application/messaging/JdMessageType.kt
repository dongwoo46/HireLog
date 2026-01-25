enum class JdMessageType {

    /**
     * JD 전처리 요청
     * - Python Worker가 처리 시작
     */
    JD_PREPROCESS_REQUEST,

    /**
     * JD 전처리 완료
     * - 정규화 텍스트
     * - canonicalHash 계산 완료
     */
    JD_PREPROCESS_RESULT,

    /**
     * JD 전처리 실패
     */
    JD_PREPROCESS_FAILED,

    /**
     * JD 요약 요청 (LLM)
     */
    JD_SUMMARY_REQUEST,

    /**
     * JD 요약 완료
     */
    JD_SUMMARY_RESULT,

    /**
     * JD 요약 실패
     */
    JD_SUMMARY_FAILED
}
