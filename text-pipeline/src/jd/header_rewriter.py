# jd/header_rewriter.py
from common.section.loader import load_section_keywords

def rewrite_broken_headers(lines: list[dict]) -> list[dict]:
    section_keywords = load_section_keywords()

    rewritten = []

    for line in lines:
        text = line["text"].strip()
        lowered = text.lower()

        for section, keywords in section_keywords.items():
            for kw in keywords:
                # 짧고 명사형이면 헤더로 판단
                if kw in lowered and len(text) <= 20:
                    rewritten.append({
                        **line,
                        "text": section,     # 헤더용 canonical text
                        "is_header": True
                    })
                    break
            else:
                continue
            break
        else:
            rewritten.append(line)

    return rewritten
