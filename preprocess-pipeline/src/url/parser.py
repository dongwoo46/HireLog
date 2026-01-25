from bs4 import BeautifulSoup
import logging
import re
from typing import Dict

logger = logging.getLogger(__name__)

class UrlParser:
    """
    URL Content Parser
    
    책임:
    - HTML에서 유의미한 텍스트(제목, 본문) 추출
    - 불필요한 태그(script, style 등) 제거
    """

    def parse(self, html: str) -> Dict[str, str]:
        """
        HTML을 파싱하여 제목과 본문을 반환한다.
        
        [Robust Extraction Strategy]
        특정 ID/Class에 의존하는 방식은 범용성이 떨어진다.
        따라서 '점수 기반(Score-based)' 알고리즘을 사용하여 본문을 찾는다.
        
        Score Factors:
        1. Text Density: 태그 대비 텍스트가 많은가?
        2. JD Keywords: '자격요건', 'Responsibilities' 등이 포함되어 있는가? (가장 강력한 시그널)
        3. Link Density Penalty: 텍스트 중 링크가 차지하는 비중이 높은가? (메뉴/푸터 필터링)
        """
        if not html:
            return {"title": "", "body": ""}

        try:
            soup = BeautifulSoup(html, "html.parser")
            
            # 0. 제목 추출
            title = ""
            if soup.title and soup.title.string:
                title = soup.title.string.strip()

            # 1. 노이즈 제거 (Script, Style, Hidden)
            # iframe, svg, path 등 텍스트와 무관한 태그 제거
            for tag in soup(["script", "style", "noscript", "iframe", "svg", "path", "header", "footer", "nav"]):
                tag.decompose()
                
            # display: none 스타일이나 hidden 속성도 거르면 좋지만, BS4로는 한계가 있음.
            # (일단 태그 기반으로만 진행)

            # 2. 후보군(Candidates) 선정 및 점수 산정
            # 텍스트를 담을 수 있는 블록 태그들
            block_tags = ["div", "section", "article", "main", "td"]
            candidates = {} # {element: score}

            # JD 관련 핵심 키워드 (가중치 부여용)
            jd_keywords = [
                 "자격요건", "우대사항", "담당업무", "주요업무", "지원자격", "복리후생", "전형절차",
                 "Requirements", "Responsibilities", "Qualifications", "Preferred", "Description", "Benefits"
            ]

            for tag_name in block_tags:
                for element in soup.find_all(tag_name):
                    text = element.get_text(separator=" ", strip=True)
                    if len(text) < 50: # 너무 짧은 블록 무시 (상향 조정)
                        continue
                        
                    # --- Scoring Logic ---
                    score = 0
                    
                    # A. 텍스트 길이 (기본 점수)
                    score += len(text) * 0.1 # 길이 가중치 축소
                    
                    # B. 문단(Paragraph) 가산점 (핵심)
                    # JD는 긴 문장이 많음. 사이드바/메뉴는 짧은 단어 나열이 많음.
                    # 50자 이상인 "문장"의 개수를 센다.
                    long_paragraphs = [line for line in text.split('.') if len(line.strip()) > 50]
                    score += len(long_paragraphs) * 100
                    
                    # C. JD 키워드 가산점
                    keyword_hits = sum(1 for k in jd_keywords if k in text)
                    score += keyword_hits * 300 
                    
                    # D. Link Density Penalty
                    links = element.find_all("a")
                    link_text_length = sum(len(a.get_text(strip=True)) for a in links)
                    
                    if len(text) > 0:
                        link_density = link_text_length / len(text)
                        # 링크 비중이 높으면 본문 아닐 확률 높음
                        if link_density > 0.3: 
                            score *= (1 - link_density * 2) # 페널티 강화
                            
                    # E. Short Line Density Penalty (List/Menu 감지)
                    # 줄바꿈 기준으로 쪼갰을 때, 짧은 라인(30자 미만)의 비중이 너무 높으면 페널티
                    raw_lines = element.get_text(separator="\n").splitlines()
                    valid_lines = [l.strip() for l in raw_lines if l.strip()]
                    if valid_lines:
                        short_lines = [l for l in valid_lines if len(l) < 30]
                        line_density = len(short_lines) / len(valid_lines)
                        if line_density > 0.8: # 80% 이상이 짧은 줄이면 메뉴일 확률 높음 (JD는 불렛 때문일수도 있으니 조심)
                            # 단, 키워드가 많으면 JD일 수 있음 (Unordered List)
                            if keyword_hits < 2:
                                score *= 0.5

                    candidates[element] = score

            # 3. Best Candidate 선정
            if not candidates:
                body_root = soup.body if soup.body else soup
            else:
                sorted_candidates = sorted(candidates.items(), key=lambda x: x[1], reverse=True)
                body_root = sorted_candidates[0][0]

            # 4. 최종 텍스트 추출 및 정제
            body_text = self._extract_clean_text_from_root(body_root)
            
            return {
                "title": title,
                "body": body_text
            }
            
        except Exception as e:
            logger.error(f"Failed to parse HTML: {e}")
            raise

    def _extract_clean_text_from_root(self, element) -> str:
        """
        선정된 루트 엘리먼트에서 라인 단위로 텍스트를 추출하고 노이즈를 제거한다.
        """
        raw_text = element.get_text(separator="\n")
        lines = []
        
        # 라인별 클리닝
        for line in raw_text.splitlines():
            line = line.strip()
            if not line:
                continue
            
            # 1. UI 노이즈 필터 (명시적)
            if line in ["닫기", "Close", "Share", "공유하기", "지원하기", "Apply", "Filter", "초기화", "검색"]:
                continue
            
            # 2. 너무 짧고 의미 없는 단어 필터링
            # (한글 2글자 미만, 영어 3글자 미만 등. 단, 숫자/특수문자 포함 시 보존)
            if len(line) < 2 and not any(char.isdigit() for char in line):
                 continue
                 
            # 3. 사이드바성 패턴 (메뉴명, 카테고리명 등)
            # JD 본문 라인이라고 보기엔 문맥이 없는 명사 나열... 식별 어려움.
            # 일단은 'Best Candidate' 선정이 잘 되었다고 믿고, 명백한 노이즈만 제거.
                
            lines.append(line)
            
        return "\n".join(lines)
