"""
부하 테스트용 HireLog 시드 스크립트

목적
- OpenSearch 검색 API 부하 테스트용 job_summary 데이터 생성
- 로그인 사용자 enrichment 테스트용 member / member_job_summary 데이터 생성

지원 기능
- OpenSearch streaming bulk insert (메모리 효율, 100만 건 대응)
- PostgreSQL bulk insert
- 최근 공고 편중 분포 (최근 7일 40%, 30일 35%, 90일 20%, 180일 5%)
- Hot keyword 강제 주입 (백엔드/Spring/Kotlin 등 30% 문서에 명시 삽입)
- 회원별 saved state 분포

예시 실행
1) OpenSearch 100만 건 적재 (기본값)
python seed_opensearch.py --seed-os

2) OpenSearch + PostgreSQL 함께 적재
python seed_opensearch.py \\
  --seed-os \\
  --seed-db \\
  --job-count 1000000 \\
  --member-count 1000 \\
  --member-job-summary-count 80000 \\
  --pg-dsn "host=localhost port=5432 dbname=hirelog user=postgres password=postgres"

3) 기존 데이터 초기화 후 재적재
python seed_opensearch.py \\
  --seed-os \\
  --seed-db \\
  --reset-os \\
  --reset-db \\
  --job-count 1000000 \\
  --pg-dsn "host=localhost port=5432 dbname=hirelog user=postgres password=postgres"
"""

from __future__ import annotations

import argparse
import random
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Iterator, Sequence

from opensearchpy import OpenSearch, helpers

try:
    import psycopg2
    from psycopg2.extras import execute_values
except ImportError:
    psycopg2 = None
    execute_values = None


# -----------------------------------------------------------------------------
# 고정 시드
# -----------------------------------------------------------------------------

RANDOM_SEED = 42


# -----------------------------------------------------------------------------
# 도메인 데이터 풀
# -----------------------------------------------------------------------------

BRANDS = [
    ((1, "카카오"), 18),
    ((2, "네이버"), 16),
    ((3, "라인"), 8),
    ((4, "쿠팡"), 14),
    ((5, "배달의민족"), 9),
    ((6, "토스"), 12),
    ((7, "당근"), 8),
    ((8, "하이퍼커넥트"), 4),
    ((9, "크래프톤"), 6),
    ((10, "넥슨"), 5),
]

POSITIONS = [
    ((1, "백엔드 엔지니어", 1, "서버/백엔드"), 30),
    ((2, "프론트엔드 엔지니어", 2, "웹 프론트엔드"), 14),
    ((3, "풀스택 엔지니어", 1, "서버/백엔드"), 9),
    ((4, "데이터 엔지니어", 3, "데이터"), 10),
    ((5, "ML 엔지니어", 3, "데이터"), 7),
    ((6, "DevOps 엔지니어", 4, "인프라/DevOps"), 8),
    ((7, "SRE", 4, "인프라/DevOps"), 5),
    ((8, "안드로이드 엔지니어", 5, "모바일"), 6),
    ((9, "iOS 엔지니어", 5, "모바일"), 5),
    ((10, "보안 엔지니어", 6, "보안"), 2),
    ((11, "플랫폼 엔지니어", 1, "서버/백엔드"), 4),
]

TECH_STACK_GROUPS = [
    (["Java", "Spring", "Spring Boot", "JPA", "MySQL", "Redis"], 20),
    (["Kotlin", "Spring Boot", "JPA", "PostgreSQL", "Redis"], 18),
    (["Java", "Spring Boot", "Kafka", "Redis", "PostgreSQL"], 12),
    (["Python", "FastAPI", "PostgreSQL", "Kafka", "Airflow"], 10),
    (["Python", "PyTorch", "MLflow", "Airflow", "Docker"], 7),
    (["TypeScript", "React", "Next.js", "GraphQL"], 9),
    (["Go", "gRPC", "Kubernetes", "AWS", "Prometheus"], 8),
    (["Terraform", "Kubernetes", "AWS", "ArgoCD", "Prometheus"], 7),
    (["Java", "Spring Boot", "OpenSearch", "Redis", "Kafka"], 9),
]

CAREER_TYPES = [
    ("NEWCOMER", 20),
    ("EXPERIENCED", 80),
]

DOMAINS = [
    ("대규모 채용 플랫폼", 18),
    ("실시간 데이터 파이프라인", 12),
    ("검색 서비스", 15),
    ("추천 시스템", 10),
    ("광고/정산 백엔드", 7),
    ("커머스 주문 시스템", 12),
    ("개발자 생산성 플랫폼", 6),
    ("모바일 서비스 플랫폼", 8),
    ("AI 데이터 처리 플랫폼", 12),
]

TEAMS = [
    ("플랫폼", 18),
    ("프로덕트", 12),
    ("데이터", 15),
    ("인프라", 10),
    ("검색", 12),
    ("추천", 6),
    ("개발생산성", 7),
    ("AI Platform", 10),
    ("Growth", 5),
    ("Core Backend", 5),
]

# 검색 집중 hot keyword — 30% 문서에 명시 주입 (부하 테스트 시 hot key 집중 재현)
HOT_KEYWORDS = [
    ("백엔드", 18),
    ("Spring", 16),
    ("Kotlin", 12),
    ("Java", 12),
    ("Redis", 8),
    ("Kafka", 8),
    ("Python", 7),
    ("OpenSearch", 5),
    ("React", 5),
    ("AWS", 9),
]

# Hot keyword 강제 주입 시 사용하는 표현 — summaryText/responsibilities에 포함
HOT_KEYWORD_PHRASES = {
    "백엔드": [
        "백엔드 시스템 설계 및 개발 경험",
        "백엔드 서비스 안정성 개선",
        "백엔드 엔지니어로서의 역할",
    ],
    "Spring": [
        "Spring Framework 기반 서버 개발",
        "Spring Boot 서비스 아키텍처 설계",
        "Spring Batch 기반 데이터 처리",
    ],
    "Kotlin": [
        "Kotlin 기반 서버 개발 경험 보유",
        "Kotlin 코루틴을 활용한 비동기 처리",
        "Kotlin + Spring Boot 조합의 프로덕션 경험",
    ],
    "Java": [
        "Java 기반의 대규모 서비스 개발 경험",
        "Java 멀티스레딩 및 동시성 처리",
        "Java 성능 최적화 및 GC 튜닝 경험",
    ],
    "Redis": [
        "Redis 캐시 레이어 설계 및 운영 경험",
        "Redis Cluster 기반 분산 캐싱 구성",
        "Redis Pub/Sub 및 분산 락 활용",
    ],
    "Kafka": [
        "Kafka 기반 이벤트 드리븐 아키텍처 설계",
        "Kafka Consumer Group 운영 및 lag 모니터링",
        "Kafka 메시지 보장과 재처리 전략 수립",
    ],
    "Python": [
        "Python 기반 데이터 파이프라인 개발",
        "Python FastAPI 서버 개발 및 운영",
        "Python 비동기 처리 및 성능 최적화",
    ],
    "OpenSearch": [
        "OpenSearch 기반 검색 서비스 개발 및 운영",
        "OpenSearch 인덱스 설계 및 쿼리 최적화",
        "OpenSearch 클러스터 운영 및 장애 대응",
    ],
    "React": [
        "React 기반 프론트엔드 개발 경험",
        "React 컴포넌트 설계 및 상태 관리",
        "React + TypeScript 프로젝트 리드 경험",
    ],
    "AWS": [
        "AWS 기반 클라우드 인프라 설계 및 운영",
        "AWS EKS/ECS 컨테이너 환경 구성",
        "AWS 비용 최적화 및 가용성 설계",
    ],
}

RESPONSIBILITY_TEMPLATES = [
    "{domain}의 핵심 API를 설계하고 개발합니다.",
    "{team} 팀에서 대규모 트래픽 환경의 서비스 안정성을 개선합니다.",
    "검색 품질 향상과 응답 속도 최적화를 담당합니다.",
    "배치/비동기 파이프라인을 설계하고 운영합니다.",
    "장애 대응 자동화 및 운영 효율 개선을 수행합니다.",
]

REQUIRED_QUALIFICATIONS = [
    "자료구조, 운영체제, 네트워크, 데이터베이스에 대한 기본 이해",
    "Java 또는 Kotlin 기반 서버 개발 경험",
    "Spring Boot 또는 이에 준하는 웹 프레임워크 경험",
    "RDBMS 설계 및 SQL 최적화 경험",
    "분산 시스템 또는 대용량 트래픽 처리 경험",
]

PREFERRED_QUALIFICATIONS = [
    "Redis, Kafka, OpenSearch 운영 경험",
    "클라우드 환경(AWS/GCP)에서 서비스 운영 경험",
    "테스트 자동화 및 관측성 개선 경험",
    "JPA/Hibernate 성능 최적화 경험",
    "검색/추천/데이터 파이프라인 관련 경험",
]

IDEAL_CANDIDATES = [
    "문제의 표면이 아니라 구조를 개선하는 엔지니어",
    "운영 이슈를 코드와 데이터 관점에서 함께 풀 수 있는 엔지니어",
    "비즈니스 요구사항을 안정적인 시스템으로 번역할 수 있는 엔지니어",
    "성능과 유지보수성의 균형을 이해하는 엔지니어",
]

MUST_HAVE_SIGNALS = [
    "대용량 데이터 처리 경험",
    "분산 시스템 설계 경험",
    "검색 최적화 경험",
    "운영 자동화 경험",
    "장애 대응 및 재처리 경험",
]

SUMMARY_TEMPLATES = [
    "{brand}의 {team} 팀에서 {domain}를 담당할 {position}를 찾습니다. "
    "{hot_keyword} 기반의 기술 스택을 활용하여 사용자 경험과 운영 안정성을 함께 개선합니다.",

    "{brand}에서 {position}로 합류하여 {domain}의 핵심 시스템을 개발합니다. "
    "{hot_keyword} 및 분산 시스템 기반의 설계 경험이 있다면 강한 장점이 됩니다.",

    "{brand} {team} 팀은 {domain}를 고도화하고 있습니다. "
    "이번 포지션은 {hot_keyword} 중심의 백엔드/플랫폼 개선 과제를 담당합니다.",
]

PREPARATION_FOCUS = [
    "시스템 설계와 트레이드오프 설명 능력",
    "장애 대응 및 재처리 전략 설명 능력",
    "대용량 트래픽 처리 경험 정리",
    "DB 인덱싱과 성능 개선 사례 정리",
]

TRANSFERABLE_STRENGTHS = [
    "유사한 트래픽 패턴의 서비스 운영 경험을 강조",
    "비동기 처리와 데이터 정합성 설계 경험을 강조",
    "검색/캐시/DB 튜닝 경험을 연결해서 설명",
    "레거시 리팩터링과 운영 안정화 경험을 활용",
]

PROOF_POINTS = [
    "p95 레이턴시 개선 경험",
    "처리량 증가 경험",
    "실패율 감소 경험",
    "운영 자동화로 인한 작업 시간 절감 경험",
]

STORY_ANGLES = [
    "검색 성능 최적화",
    "이벤트 기반 아키텍처 개선",
    "레거시 구조 재설계",
    "장애 대응 자동화",
    "운영 비용 절감",
]

KEY_CHALLENGES = [
    "급격한 트래픽 증가에 따른 성능 이슈",
    "검색 정확도와 응답 속도의 균형",
    "운영 복잡도 증가",
    "데이터 정합성과 이벤트 유실 방지",
]

QUESTIONS_TO_ASK = [
    "검색 품질과 검색 성능은 어떤 지표로 측정하나요?",
    "OpenSearch 운영 이슈는 주로 어떤 유형이 많나요?",
    "로그인 사용자 personalization은 어느 계층에서 처리하나요?",
    "Redis 캐시는 어떤 키 전략과 만료 전략을 사용하나요?",
]


# -----------------------------------------------------------------------------
# 유틸
# -----------------------------------------------------------------------------

def weighted_choice(weighted_items: Sequence[tuple[object, int]]):
    items = [item for item, _ in weighted_items]
    weights = [weight for _, weight in weighted_items]
    return random.choices(items, weights=weights, k=1)[0]


def random_recent_datetime() -> datetime:
    """
    최근 공고에 편중된 분포 생성
    - 최근 7일: 40%
    - 최근 30일: 35%
    - 최근 90일: 20%
    - 최근 180일: 5%
    """
    now = datetime.now(timezone.utc)
    bucket = random.random()

    if bucket < 0.40:
        delta_days = random.randint(0, 7)
    elif bucket < 0.75:
        delta_days = random.randint(8, 30)
    elif bucket < 0.95:
        delta_days = random.randint(31, 90)
    else:
        delta_days = random.randint(91, 180)

    delta_seconds = random.randint(0, 86400 - 1)
    return now - timedelta(days=delta_days, seconds=delta_seconds)


def isoformat_z(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%dT%H:%M:%S")


def random_company_name(brand_name: str) -> str:
    suffixes = ["(주)", "주식회사", "Corp", "Inc"]
    return f"{brand_name} {random.choice(suffixes)}"


def random_tech_stack() -> list[str]:
    base_group = weighted_choice(TECH_STACK_GROUPS)
    extra_candidates = [
        "Docker", "Kubernetes", "Prometheus", "Grafana", "gRPC",
        "OpenSearch", "Elasticsearch", "MongoDB", "TypeScript",
        "GraphQL", "AWS", "GCP", "Terraform", "RabbitMQ",
    ]

    tech = set(base_group)
    extra_count = random.randint(0, 3)
    if extra_count > 0:
        tech.update(random.sample(extra_candidates, k=extra_count))

    return sorted(tech)


def random_hot_keyword(position_name: str, tech_stack: list[str]) -> str:
    candidates = [kw for kw, _ in HOT_KEYWORDS]
    candidates.extend(tech_stack)
    candidates.append(position_name.split()[0])
    return random.choice(candidates)


def pick_hot_injection_keyword() -> str | None:
    """
    30% 확률로 hot keyword 주입 대상 선택.
    검색 집중 시나리오 재현 — '백엔드', 'Spring', 'Kotlin'이 검색 트래픽의 대부분을 차지.
    """
    if random.random() > 0.30:
        return None
    # 상위 3개 키워드에 집중된 분포 (실제 검색 집중 패턴 모사)
    return random.choices(
        population=["백엔드", "Spring", "Kotlin", "Java", "Redis", "Kafka", "Python", "AWS"],
        weights=[25, 22, 18, 15, 7, 6, 4, 3],
        k=1,
    )[0]


def random_sentence_choices(pool: Sequence[str], min_count: int = 2, max_count: int = 3) -> str:
    count = random.randint(min_count, max_count)
    return " ".join(random.sample(pool, k=min(count, len(pool))))


def random_career_type() -> str:
    return weighted_choice(CAREER_TYPES)


def random_career_years(career_type: str) -> str | None:
    if career_type == "NEWCOMER":
        return None
    year = random.choices(
        population=[2, 3, 4, 5, 6, 7, 8, 9, 10],
        weights=[12, 16, 16, 14, 12, 10, 8, 6, 6],
        k=1,
    )[0]
    return f"{year}년"


def random_saved_count_per_member(avg_saved_count: int) -> int:
    """
    DB enrichment 병목 시나리오를 재현하기 위해 member tier를 3단계로 분리.

    Tier 분포:
    - Light (60%): avg의 40% 저장 — 소형 IN-query, DB 부하 낮음
    - Medium (29%): avg 저장 — 일반 사용자
    - Power (1% = 파워유저): avg의 10~20배 저장 → IN-query 폭발
      → enrichment SELECT ... WHERE job_summary_id IN (1000+개)
      → 이 쿼리가 집중될 때 DB connection 고갈 및 p99 급등 재현
    """
    bucket = random.random()
    if bucket < 0.60:
        return max(1, int(random.gauss(avg_saved_count * 0.4, 5)))
    if bucket < 0.89:
        return max(5, int(random.gauss(avg_saved_count, 10)))
    if bucket < 0.99:
        return max(10, int(random.gauss(avg_saved_count * 2.5, 30)))
    # Power user (1%): IN-query 크기 폭발로 DB enrichment 병목 재현
    return max(500, int(random.gauss(avg_saved_count * 15, 200)))


# -----------------------------------------------------------------------------
# OpenSearch 문서 생성
# -----------------------------------------------------------------------------

@dataclass(frozen=True)
class JobSeedMeta:
    """DB seed에서 member_job_summary 비정규화 필드 채우기 위한 메타"""
    id: int
    brand_name: str
    position_name: str
    brand_position_name: str
    position_category_name: str


@dataclass(frozen=True)
class JobSummarySeedConfig:
    index_name: str
    job_count: int
    start_job_id: int


def build_job_document(doc_id: int, index_name: str) -> tuple[dict, JobSeedMeta]:
    brand_id, brand_name = weighted_choice(BRANDS)
    position_id, position_name, category_id, category_name = weighted_choice(POSITIONS)
    career_type = random_career_type()
    career_years = random_career_years(career_type)
    tech_stack = random_tech_stack()
    domain = weighted_choice(DOMAINS)
    team = weighted_choice(TEAMS)
    hot_keyword = random_hot_keyword(position_name, tech_stack)
    created_at = random_recent_datetime()

    brand_position_name = f"{brand_name} {position_name}"

    summary_text = random.choice(SUMMARY_TEMPLATES).format(
        brand=brand_name,
        team=team,
        domain=domain,
        position=position_name,
        hot_keyword=hot_keyword,
    )

    responsibilities = " ".join(
        random.sample(
            [t.format(domain=domain, team=team) for t in RESPONSIBILITY_TEMPLATES],
            k=min(3, len(RESPONSIBILITY_TEMPLATES)),
        )
    )

    # Hot keyword 강제 주입: 30% 문서에 검색 집중 키워드를 명시 삽입
    # 부하 테스트에서 특정 키워드 검색이 집중될 때의 OpenSearch 동작 재현
    inject_kw = pick_hot_injection_keyword()
    if inject_kw and inject_kw in HOT_KEYWORD_PHRASES:
        phrase = random.choice(HOT_KEYWORD_PHRASES[inject_kw])
        summary_text = f"{summary_text} {phrase}."
        responsibilities = f"{phrase}. {responsibilities}"
        # tech_stack에도 해당 키워드 주입 (검색 필드 일치율 향상)
        if inject_kw not in tech_stack:
            tech_stack = sorted(set(tech_stack) | {inject_kw})

    doc = {
        "_index": index_name,
        "_id": str(doc_id),
        "_source": {
            "id": doc_id,
            "jobSnapshotId": doc_id * 10 + random.randint(1, 9),
            "brandId": brand_id,
            "brandName": brand_name,
            "companyId": brand_id * 100 + random.randint(1, 20),
            "companyName": random_company_name(brand_name),
            "positionId": position_id,
            "positionName": position_name,
            "brandPositionId": brand_id * 1000 + position_id,
            "brandPositionName": brand_position_name,
            "positionCategoryId": category_id,
            "positionCategoryName": category_name,
            "careerType": career_type,
            "careerYears": career_years,
            "summaryText": summary_text,
            "responsibilities": responsibilities,
            "requiredQualifications": random_sentence_choices(REQUIRED_QUALIFICATIONS, 2, 3),
            "preferredQualifications": random_sentence_choices(PREFERRED_QUALIFICATIONS, 1, 2),
            "techStack": ", ".join(tech_stack),
            "techStackParsed": tech_stack,
            "recruitmentProcess": "서류 → 코딩테스트 또는 과제 → 기술면접 → 최종면접",
            "idealCandidate": random.choice(IDEAL_CANDIDATES),
            "mustHaveSignals": random.choice(MUST_HAVE_SIGNALS),
            "preparationFocus": random.choice(PREPARATION_FOCUS),
            "transferableStrengthsAndGapPlan": random.choice(TRANSFERABLE_STRENGTHS),
            "proofPointsAndMetrics": random.choice(PROOF_POINTS),
            "storyAngles": random.choice(STORY_ANGLES),
            "keyChallenges": random.choice(KEY_CHALLENGES),
            "technicalContext": ", ".join(tech_stack),
            "questionsToAsk": random.choice(QUESTIONS_TO_ASK),
            "considerations": "검색 성능, 운영 안정성, 협업 밀도",
            "createdAt": isoformat_z(created_at),
        },
    }

    meta = JobSeedMeta(
        id=doc_id,
        brand_name=brand_name,
        position_name=position_name,
        brand_position_name=brand_position_name,
        position_category_name=category_name,
    )

    return doc, meta


def generate_job_documents(
    config: JobSummarySeedConfig,
    collect_metas: bool,
    progress_interval: int = 100_000,
) -> Iterator[dict]:
    """
    스트리밍 문서 generator.
    collect_metas=True 시 내부적으로 meta를 수집하고
    generator 소진 후 .metas 속성으로 접근 가능.

    100만 건 기준 메모리 사용:
    - collect_metas=False: 문서 생성 메모리만 사용 (O(chunk_size))
    - collect_metas=True : JobSeedMeta 100만 개 ~200MB
    """
    generate_job_documents.metas = []
    reported = 0

    for offset in range(config.job_count):
        doc_id = config.start_job_id + offset
        doc, meta = build_job_document(doc_id=doc_id, index_name=config.index_name)

        if collect_metas:
            generate_job_documents.metas.append(meta)

        yield doc

        reported += 1
        if reported % progress_interval == 0:
            print(f"[OpenSearch] 생성 진행: {reported:,} / {config.job_count:,}")


# -----------------------------------------------------------------------------
# OpenSearch 적재
# -----------------------------------------------------------------------------

def create_opensearch_client(host: str, port: int, user: str, password: str) -> OpenSearch:
    return OpenSearch(
        hosts=[{"host": host, "port": port}],
        http_auth=(user, password),
        use_ssl=False,
        verify_certs=False,
    )


def reset_os_index(client: OpenSearch, index_name: str) -> None:
    if client.indices.exists(index=index_name):
        client.indices.delete(index=index_name)

    # 100만 건 기준: shard 3개, replica 0 (부하 테스트 환경)
    client.indices.create(
        index=index_name,
        body={
            "settings": {
                "number_of_shards": 3,
                "number_of_replicas": 0,
                "refresh_interval": "-1",        # bulk 적재 중 refresh 비활성 (속도 최적화)
                "index.translog.durability": "async",  # bulk 중 fsync 비동기
            },
            "mappings": {
                "properties": {
                    "id": {"type": "long"},
                    "jobSnapshotId": {"type": "long"},
                    "brandId": {"type": "integer"},
                    "brandName": {"type": "keyword"},
                    "companyId": {"type": "integer"},
                    "companyName": {"type": "text", "fields": {"keyword": {"type": "keyword"}}},
                    "positionId": {"type": "integer"},
                    "positionName": {"type": "text", "fields": {"keyword": {"type": "keyword"}}},
                    "brandPositionId": {"type": "integer"},
                    "brandPositionName": {"type": "text"},
                    "positionCategoryId": {"type": "integer"},
                    "positionCategoryName": {"type": "keyword"},
                    "careerType": {"type": "keyword"},
                    "careerYears": {"type": "keyword"},
                    "summaryText": {"type": "text"},
                    "responsibilities": {"type": "text"},
                    "requiredQualifications": {"type": "text"},
                    "preferredQualifications": {"type": "text"},
                    "techStack": {"type": "text"},
                    "techStackParsed": {"type": "keyword"},
                    "recruitmentProcess": {"type": "text"},
                    "idealCandidate": {"type": "text"},
                    "mustHaveSignals": {"type": "text"},
                    "preparationFocus": {"type": "text"},
                    "transferableStrengthsAndGapPlan": {"type": "text"},
                    "proofPointsAndMetrics": {"type": "text"},
                    "storyAngles": {"type": "text"},
                    "keyChallenges": {"type": "text"},
                    "technicalContext": {"type": "text"},
                    "questionsToAsk": {"type": "text"},
                    "considerations": {"type": "text"},
                    "createdAt": {"type": "date"},
                }
            },
        },
    )


def restore_os_index_settings(client: OpenSearch, index_name: str) -> None:
    """bulk 완료 후 refresh_interval 복구 및 force merge."""
    client.indices.put_settings(
        index=index_name,
        body={
            "refresh_interval": "1s",
            "index.translog.durability": "request",
        },
    )
    # segment 병합으로 검색 성능 안정화
    client.indices.forcemerge(index=index_name, max_num_segments=5, request_timeout=300)
    client.indices.refresh(index=index_name)


def seed_opensearch(
    client: OpenSearch,
    index_name: str,
    job_count: int,
    start_job_id: int,
    chunk_size: int,
    reset_index: bool,
    collect_metas: bool = True,
) -> list[JobSeedMeta]:
    if reset_index:
        print(f"[OpenSearch] reset index: {index_name}")
        reset_os_index(client, index_name)
    elif not client.indices.exists(index=index_name):
        # --reset-os 없이 인덱스가 없는 경우 자동 생성
        reset_os_index(client, index_name)

    info = client.info()
    print(f"[OpenSearch] cluster={info['cluster_name']}, version={info['version']['number']}")
    print(f"[OpenSearch] {job_count:,}건 bulk 적재 시작 (chunk={chunk_size})")

    config = JobSummarySeedConfig(
        index_name=index_name,
        job_count=job_count,
        start_job_id=start_job_id,
    )

    doc_gen = generate_job_documents(config, collect_metas=collect_metas)

    success, failed = helpers.bulk(
        client,
        doc_gen,
        chunk_size=chunk_size,
        raise_on_error=False,
        request_timeout=180,
    )

    print(f"[OpenSearch] bulk 완료: success={success:,}, failed={len(failed)}")

    # refresh_interval 복구 및 segment 병합
    print("[OpenSearch] index 설정 복구 및 force merge 중...")
    restore_os_index_settings(client, index_name)

    count = client.count(index=index_name)["count"]
    print(f"[OpenSearch] 최종 문서 수: {count:,}")

    return generate_job_documents.metas if collect_metas else []


# -----------------------------------------------------------------------------
# PostgreSQL 적재
# -----------------------------------------------------------------------------

def ensure_psycopg2_installed() -> None:
    if psycopg2 is None or execute_values is None:
        raise RuntimeError(
            "psycopg2-binary가 설치되어 있지 않습니다. "
            "pip install psycopg2-binary 로 설치하세요."
        )


def reset_db_tables(conn, member_table: str, member_job_summary_table: str) -> None:
    with conn.cursor() as cur:
        cur.execute(f"TRUNCATE TABLE {member_job_summary_table} RESTART IDENTITY CASCADE;")
        cur.execute(f"TRUNCATE TABLE {member_table} RESTART IDENTITY CASCADE;")
    conn.commit()


def generate_member_rows(member_count: int, start_member_id: int) -> list[tuple]:
    """
    member 테이블 INSERT용 row 생성

    컬럼: id, email, username, role, status, version, created_at, updated_at
    - role: 'USER' (MemberRole.USER)
    - status: 'ACTIVE' (MemberStatus.ACTIVE)
    - version: 0 (VersionedEntity @Version 초기값)
    """
    now = datetime.now(timezone.utc)
    rows = []
    for i in range(member_count):
        member_id = start_member_id + i
        rows.append((
            member_id,
            f"loadtest_user_{member_id}@hirelog.local",
            f"loadtest-user-{member_id}",
            "USER",
            "ACTIVE",
            0,
            now,
            now,
        ))
    return rows


def insert_members(conn, member_table: str, rows: list[tuple]) -> None:
    with conn.cursor() as cur:
        execute_values(
            cur,
            f"""
            INSERT INTO {member_table} (
                id, email, username, role, status, version, created_at, updated_at
            ) VALUES %s
            ON CONFLICT (id) DO NOTHING
            """,
            rows,
            page_size=1000,
        )
    conn.commit()


def pick_hot_job_ids(all_job_ids: list[int], hot_ratio: float = 0.001) -> list[int]:
    """
    전체 job 중 상위 0.1%를 'hot job'으로 선정.
    hot job은 80% member에 강제 저장 → 특정 row에 DB 읽기 집중 유발.

    DB enrichment 병목 재현 목적:
    - 동일 job_summary_id에 대한 buffer pool hit 집중
    - 해당 rows가 항상 cache에 올라있어 정상 처리되다가,
      power user의 대형 IN-query와 겹치면 lock contention/connection 소진 발생
    """
    hot_count = max(1, int(len(all_job_ids) * hot_ratio))
    return random.sample(all_job_ids, k=hot_count)


def generate_member_job_summary_rows(
    member_count: int,
    start_member_id: int,
    total_saved_count: int,
    job_metas: list[JobSeedMeta],
) -> list[tuple]:
    """
    member_job_summary 테이블 INSERT용 row 생성

    컬럼: member_id, job_summary_id, brand_name, position_name,
          brand_position_name, position_category_name, save_type,
          created_at, updated_at

    DB enrichment 병목 시나리오:
    1. Power user (1%): 500~3000건 저장 → 대형 IN-query → connection 고갈
    2. Hot job (상위 0.1%): 80% member가 공유 저장 → buffer pool 집중 / hot row
    3. job_count > 100k 시 random.choices 기반 O(k) 샘플링 (성능)
    """
    meta_by_id: dict[int, JobSeedMeta] = {m.id: m for m in job_metas}
    all_job_ids = list(meta_by_id.keys())

    # hot job 선정: 전체의 0.1% → 80% 이상의 member에 저장됨
    hot_job_ids = pick_hot_job_ids(all_job_ids)
    hot_job_set = set(hot_job_ids)

    avg_saved_count = max(1, total_saved_count // member_count)
    rows: list[tuple] = []
    large_pool = len(all_job_ids) > 100_000

    for i in range(member_count):
        member_id = start_member_id + i
        saved_count = min(len(all_job_ids), random_saved_count_per_member(avg_saved_count))

        # hot job 80% 확률로 강제 포함
        forced: set[int] = set()
        if random.random() < 0.80:
            forced.update(random.sample(hot_job_ids, k=min(len(hot_job_ids), 5)))

        # 나머지 슬롯 채우기
        remaining = max(0, saved_count - len(forced))
        if remaining > 0:
            if large_pool:
                seen: set[int] = set(forced)
                while len(seen) - len(forced) < remaining:
                    candidates = random.choices(all_job_ids, k=remaining - (len(seen) - len(forced)) + 10)
                    seen.update(candidates)
                extra = list(seen - forced)[:remaining]
            else:
                pool = [j for j in all_job_ids if j not in forced]
                extra = random.sample(pool, k=min(remaining, len(pool)))
        else:
            extra = []

        saved_job_ids = list(forced) + extra

        for job_summary_id in saved_job_ids:
            meta = meta_by_id[job_summary_id]
            created_at = random_recent_datetime()
            rows.append((
                member_id,
                job_summary_id,
                meta.brand_name,
                meta.position_name,
                meta.brand_position_name,
                meta.position_category_name,
                "SAVED",
                created_at,
                created_at,
            ))

    return rows


def insert_member_job_summary(conn, member_job_summary_table: str, rows: list[tuple]) -> None:
    with conn.cursor() as cur:
        execute_values(
            cur,
            f"""
            INSERT INTO {member_job_summary_table} (
                member_id, job_summary_id,
                brand_name, position_name, brand_position_name, position_category_name,
                save_type,
                created_at, updated_at
            ) VALUES %s
            ON CONFLICT (member_id, job_summary_id) DO NOTHING
            """,
            rows,
            page_size=2000,
        )
    conn.commit()


def ensure_db_indexes(conn, member_job_summary_table: str) -> None:
    """로그인 검색 enrichment의 핵심 인덱스."""
    with conn.cursor() as cur:
        cur.execute(
            f"""
            CREATE UNIQUE INDEX IF NOT EXISTS uq_mjs_member_job
            ON {member_job_summary_table} (member_id, job_summary_id)
            """
        )
        cur.execute(
            f"""
            CREATE INDEX IF NOT EXISTS idx_mjs_member_id
            ON {member_job_summary_table} (member_id)
            """
        )
    conn.commit()


def seed_postgres(
    pg_dsn: str,
    member_table: str,
    member_job_summary_table: str,
    member_count: int,
    start_member_id: int,
    member_job_summary_count: int,
    job_metas: list[JobSeedMeta],
    reset_db: bool,
) -> None:
    ensure_psycopg2_installed()

    conn = psycopg2.connect(pg_dsn)

    try:
        if reset_db:
            print("[PostgreSQL] reset tables")
            reset_db_tables(conn, member_table, member_job_summary_table)

        ensure_db_indexes(conn, member_job_summary_table)

        member_rows = generate_member_rows(
            member_count=member_count,
            start_member_id=start_member_id,
        )
        insert_members(conn, member_table, member_rows)
        print(f"[PostgreSQL] inserted members={len(member_rows):,}")

        saved_rows = generate_member_job_summary_rows(
            member_count=member_count,
            start_member_id=start_member_id,
            total_saved_count=member_job_summary_count,
            job_metas=job_metas,
        )
        insert_member_job_summary(conn, member_job_summary_table, saved_rows)
        print(f"[PostgreSQL] inserted member_job_summary={len(saved_rows):,}")

    finally:
        conn.close()


# -----------------------------------------------------------------------------
# CLI
# -----------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="HireLog load test seed generator")

    parser.add_argument("--seed-os", action="store_true", help="OpenSearch 시드 적재")
    parser.add_argument("--seed-db", action="store_true", help="PostgreSQL 시드 적재")

    # OpenSearch
    parser.add_argument("--os-host", default="localhost")
    parser.add_argument("--os-port", type=int, default=9200)
    parser.add_argument("--os-user", default="admin")
    parser.add_argument("--os-password", default="admin")
    parser.add_argument("--os-index", default="job_summary")
    parser.add_argument("--job-count", type=int, default=1_000_000)
    parser.add_argument("--start-job-id", type=int, default=1)
    parser.add_argument("--os-chunk-size", type=int, default=2000)
    parser.add_argument("--reset-os", action="store_true", help="OpenSearch 인덱스 재생성")

    # PostgreSQL
    parser.add_argument("--pg-dsn", default="")
    parser.add_argument("--member-table", default="member")
    parser.add_argument("--member-job-summary-table", default="member_job_summary")
    parser.add_argument("--member-count", type=int, default=1000)
    parser.add_argument("--start-member-id", type=int, default=1)
    parser.add_argument("--member-job-summary-count", type=int, default=80000)
    parser.add_argument("--reset-db", action="store_true", help="DB 테이블 truncate")

    return parser.parse_args()


def main() -> None:
    random.seed(RANDOM_SEED)
    args = parse_args()

    if not args.seed_os and not args.seed_db:
        raise ValueError("--seed-os 또는 --seed-db 중 하나는 반드시 지정해야 합니다.")

    job_metas: list[JobSeedMeta] = []

    if args.seed_os:
        os_client = create_opensearch_client(
            host=args.os_host,
            port=args.os_port,
            user=args.os_user,
            password=args.os_password,
        )
        job_metas = seed_opensearch(
            client=os_client,
            index_name=args.os_index,
            job_count=args.job_count,
            start_job_id=args.start_job_id,
            chunk_size=args.os_chunk_size,
            reset_index=args.reset_os,
            collect_metas=args.seed_db,  # DB seed 불필요 시 meta 수집 생략
        )
    else:
        # OS seed 없이 DB만 실행하는 경우: job_id 범위로 더미 메타 생성
        print("[seed] --seed-os 없이 --seed-db 실행 — job_id 범위로 더미 메타 생성")
        for job_id in range(args.start_job_id, args.start_job_id + args.job_count):
            brand_id, brand_name = weighted_choice(BRANDS)
            pos_id, position_name, cat_id, category_name = weighted_choice(POSITIONS)
            job_metas.append(JobSeedMeta(
                id=job_id,
                brand_name=brand_name,
                position_name=position_name,
                brand_position_name=f"{brand_name} {position_name}",
                position_category_name=category_name,
            ))

    if args.seed_db:
        if not args.pg_dsn:
            raise ValueError("--seed-db 사용 시 --pg-dsn 은 필수입니다.")

        seed_postgres(
            pg_dsn=args.pg_dsn,
            member_table=args.member_table,
            member_job_summary_table=args.member_job_summary_table,
            member_count=args.member_count,
            start_member_id=args.start_member_id,
            member_job_summary_count=args.member_job_summary_count,
            job_metas=job_metas,
            reset_db=args.reset_db,
        )


if __name__ == "__main__":
    main()
