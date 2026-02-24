INSERT INTO position_category (
    name,
    normalized_name,
    status,
    description,
    created_at,
    updated_at
)
VALUES
    ('IT / Software', 'it_software', 'ACTIVE', '소프트웨어 개발 및 IT 기술 직군', now(), now()),
    ('Data / AI', 'data_ai', 'ACTIVE', '데이터 분석 및 인공지능 관련 직군', now(), now()),
    ('Infrastructure / Platform', 'infrastructure_platform', 'ACTIVE', '인프라 및 플랫폼 엔지니어링', now(), now()),
    ('Security / Quality', 'security_quality', 'ACTIVE', '보안 및 품질 관련 직군', now(), now()),
    ('IT Planning / Support', 'it_planning_support', 'ACTIVE', 'IT 기획 및 기술 지원', now(), now()),
    ('Product / Planning', 'product_planning', 'ACTIVE', '프로덕트 및 서비스 기획', now(), now()),
    ('Business / Strategy', 'business_strategy', 'ACTIVE', '경영 및 전략', now(), now()),
    ('HR', 'hr', 'ACTIVE', '인사 및 채용', now(), now()),
    ('Finance / Accounting', 'finance_accounting', 'ACTIVE', '재무 및 회계', now(), now()),
    ('Marketing', 'marketing', 'ACTIVE', '마케팅 직군', now(), now()),
    ('Sales', 'sales', 'ACTIVE', '영업 직군', now(), now()),
    ('Design', 'design', 'ACTIVE', '디자인 직군', now(), now()),
    ('Content', 'content', 'ACTIVE', '콘텐츠 제작', now(), now()),
    ('Operations / Admin', 'operations_admin', 'ACTIVE', '운영 및 사무', now(), now()),
    ('Manufacturing / Engineering', 'manufacturing_engineering', 'ACTIVE', '제조 및 엔지니어링', now(), now()),
    ('Logistics', 'logistics', 'ACTIVE', '물류 및 유통', now(), now()),
    ('Service / Hospitality', 'service_hospitality', 'ACTIVE', '서비스 및 외식/관광', now(), now());


INSERT INTO position (
    name, normalized_name, status, category_id,
    created_at, updated_at
)
VALUES
    ('Backend Engineer', 'backend_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_software'),
     now(), now()),

    ('Frontend Engineer', 'frontend_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_software'),
     now(), now()),

    ('Fullstack Engineer', 'fullstack_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_software'),
     now(), now()),

    ('Mobile App Engineer', 'mobile_app_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_software'),
     now(), now()),

    ('Game Developer', 'game_developer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_software'),
     now(), now()),

    ('System Engineer', 'system_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_software'),
     now(), now()),

    ('Embedded Engineer', 'embedded_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_software'),
     now(), now());

INSERT INTO position (
    name, normalized_name, status, category_id,
    created_at, updated_at
)
VALUES
    ('Data Engineer', 'data_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='data_ai'),
     now(), now()),

    ('Data Analyst', 'data_analyst', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='data_ai'),
     now(), now()),

    ('Data Scientist', 'data_scientist', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='data_ai'),
     now(), now()),

    ('Machine Learning Engineer', 'machine_learning_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='data_ai'),
     now(), now()),

    ('AI Engineer', 'ai_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='data_ai'),
     now(), now());

INSERT INTO position (
    name, normalized_name, status, category_id,
    created_at, updated_at
)
VALUES
    ('DevOps Engineer', 'devops_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='infrastructure_platform'),
     now(), now()),

    ('Site Reliability Engineer', 'site_reliability_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='infrastructure_platform'),
     now(), now()),

    ('Platform Engineer', 'platform_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='infrastructure_platform'),
     now(), now()),

    ('Cloud Engineer', 'cloud_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='infrastructure_platform'),
     now(), now());

INSERT INTO position (
    name, normalized_name, status, category_id,
    created_at, updated_at
)
VALUES
    ('Security Engineer', 'security_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='security_quality'),
     now(), now()),

    ('QA Engineer', 'qa_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='security_quality'),
     now(), now()),

    ('Test Automation Engineer', 'test_automation_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='security_quality'),
     now(), now());

INSERT INTO position (
    name, normalized_name, status, category_id,
    created_at, updated_at
)
VALUES
    ('IT Planner', 'it_planner', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_planning_support'),
     now(), now()),

    ('Solutions Engineer', 'solutions_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_planning_support'),
     now(), now()),

    ('Technical Support Engineer', 'technical_support_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_planning_support'),
     now(), now()),

    ('Network Engineer', 'network_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_planning_support'),
     now(), now()),

    ('Database Administrator', 'database_administrator', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_planning_support'),
     now(), now());

INSERT INTO position (
    name, normalized_name, status, category_id,
    created_at, updated_at
)
VALUES
    ('Product Manager', 'product_manager', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='product_planning'),
     now(), now()),

    ('Product Owner', 'product_owner', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='product_planning'),
     now(), now()),

    ('Project Manager', 'project_manager', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='product_planning'),
     now(), now()),

    ('Service Planner', 'service_planner', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='product_planning'),
     now(), now()),

    ('Business Planner', 'business_planner', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='product_planning'),
     now(), now());

INSERT INTO position (
    name, normalized_name, status, category_id,
    created_at, updated_at
)
VALUES
    ('Business Manager', 'business_manager', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='business_strategy'),
     now(), now()),

    ('Strategy Manager', 'strategy_manager', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='business_strategy'),
     now(), now()),

    ('HR Manager', 'hr_manager', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='hr'),
     now(), now()),

    ('HR Specialist', 'hr_specialist', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='hr'),
     now(), now()),

    ('Recruiter', 'recruiter', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='hr'),
     now(), now()),

    ('Accountant', 'accountant', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='finance_accounting'),
     now(), now()),

    ('Financial Analyst', 'financial_analyst', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='finance_accounting'),
     now(), now()),

    ('Finance Manager', 'finance_manager', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='finance_accounting'),
     now(), now());


-- 기본 카테고리 (예: GENERAL)
INSERT INTO position_category (
    name,
    normalized_name,
    status,
    created_at,
    updated_at
)
VALUES (
           'GENERAL',
           'general',
           'ACTIVE',
           now(),
           now()
       )
    ON CONFLICT (normalized_name) DO NOTHING;


INSERT INTO position (
    name,
    normalized_name,
    status,
    category_id,
    description,
    created_at,
    updated_at
)
SELECT
    'UNKNOWN',
    'unknown',
    'ACTIVE',
    pc.id,
    'LLM 매핑 실패 시 fallback 포지션',
    now(),
    now()
FROM position_category pc
WHERE pc.normalized_name = 'general'
    ON CONFLICT (normalized_name) DO NOTHING;
