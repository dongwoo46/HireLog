import logging
from inputs.jd_preprocess_input import JdPreprocessInput
from preprocess.worker.pipeline.text_preprocess_pipeline import TextPreprocessPipeline
from url.fetcher import UrlFetcher
from url.playwright_fetcher import PlaywrightFetcher
from url.parser import UrlParser

logger = logging.getLogger(__name__)

class UrlPipeline:
    """
    URL 기반 JD 전처리 파이프라인
    
    책임:
    - URL Fetching & Parsing 실행 (Hybrid: Requests -> Fallback Playwright)
    - 파싱된 텍스트를 TextPreprocessPipeline에 연결
    """

    def __init__(self):
        self.fetcher = UrlFetcher()
        self.dynamic_fetcher = PlaywrightFetcher()
        self.parser = UrlParser()
        self.text_pipeline = TextPreprocessPipeline()

    def process(self, input: JdPreprocessInput) -> dict:
        """
        URL → Fetch → Parse → Text Preprocess 흐름 실행
        """
        if not input.url:
            raise ValueError("url is required for UrlPipeline")

        # 1. Fetch (Hybrid Strategy)
        try:
            html_content = self.fetcher.fetch(input.url)
            
            # Check for JS Rendering Requirement
            if self.fetcher._needs_js_rendering(html_content):
                logger.info(f"JS Rendering required for {input.url}. Switching to Playwright.")
                html_content = self.dynamic_fetcher.fetch(input.url)
                
        except Exception as e:
            logger.warning(f"Static fetch failed for {input.url}, retrying with Playwright. Error: {e}")
            html_content = self.dynamic_fetcher.fetch(input.url)

        # [DEBUG] HTML 내용 확인을 위한 출력
        # print(f"================ [DEBUG] Raw HTML (First 3000 chars) for {input.url} ================")
        # print(html_content[:3000]) 
        # print("================ [DEBUG] End of Preview ================")
        
        # 2. Parse
        parsed_data = self.parser.parse(html_content)
        title = parsed_data.get("title", "")
        body_text = parsed_data.get("body", "")
        
        if not body_text:
            logger.warning(f"No body text extracted from URL: {input.url}")

        # 3. Construct JD Input for Text Pipeline
        # 제목과 본문을 합쳐서 처리할지, 본문만 처리할지 결정 필요.
        # 일반적으로 JD는 본문에 핵심 내용이 있으므로 본문을 넘김.
        # 필요하다면 title을 메타데이터로 활용 가능.
        
        jd_input = JdPreprocessInput(
            request_id=input.request_id,
            brand_name=input.brand_name,
            position_name=input.position_name,
            source=input.source,
            created_at=input.created_at,
            message_version=input.message_version,
            text=body_text, # 파싱된 텍스트 전달
            url=input.url
        )

        # 4. Text Pipeline 실행
        jd_result = self.text_pipeline.process(jd_input)

        return {
            "url_meta": {
                "url": input.url,
                "title": title,
                "fetched_length": len(html_content),
                "parsed_length": len(body_text)
            },
            "jd": jd_result
        }
