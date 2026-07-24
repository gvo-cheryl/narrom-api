# API 커버리지 체크리스트

상태: 진행 중 추적 문서 (계약 아님)
기준: `docs/foundation/naroom_beta1_prototype.html` (2866줄, 전량 검토), `docs/database/reference/naroom_beta1_full_schema_reference.sql`, `docs/foundation/# Naroom Beta 1 IA 최종본.md`

## 문서 목적

프로토타입 HTML을 현재 제품의 가장 정확한 로직·플로우·화면 뎁스 기준으로 삼아, 화면/흐름 단위로 필요한 API를 전부 나열하고 실제 구현 상태와 대조한다. 새 기능을 시작하기 전에 이 표에서 관련 항목을 먼저 확인해 누락을 방지한다.

이 문서는 승인된 API 계약이 아니다. 실제 계약은 각 도메인 문서(`authentication.md`, `content.md` 등)와 `openapi.yaml`이 최종 기준이다. 여기 적힌 "필요 API (제안)"은 구현 착수 전 검토용 초안이다.

## 먼저 확인이 필요한 충돌·공백

구현 순서와 무관하게, 아래 항목은 계약을 쓰기 전에 먼저 결정이 필요하다.

1. **Google 로그인 — 프로토타입과 CLAUDE.md가 직접 충돌한다.** 프로토타입 L861 주석: "Beta 1은 카카오·Google 각각 별도 회원으로 생성됩니다." 반면 `naroom-api/CLAUDE.md`와 `PRODUCT_CONTEXT.md`는 "Beta 1은 카카오 로그인만 구현, Google은 데이터 구조만 유지"로 명시한다. 프로토타입은 데이터 구조 확장성 시연용으로 보이지만, 화면(A02 로그인, M02 계정 정보, M08 탈퇴 확인)에 Google 버튼이 실제로 그려져 있다. → **Google 로그인 버튼을 화면에 노출할지, 카카오만 노출할지 프론트 작업 전에 확정 필요.**
2. **감정 강도/에너지 스케일 불일치.** 프로토타입 내부 값은 0/25/50/75/100(5단계, L361 부근). 스키마 `check_ins.emotion_intensity`/`energy_level`은 `smallint 1~5` CHECK 제약. → 0~100 ↔ 1~5 변환 규칙을 API 응답/요청 계약에 명시해야 한다.
3. **"나의 정리"(L08) 데이터 모델 불일치.** 프로토타입은 단일 문서(`myNote`)를 계속 덮어쓰고 별도 `noteHistory`로 이력을 로컬 보관하는 구조. 스키마 `personal_summaries`는 scope별로 행이 계속 쌓이는 누적형이며 이력 전용 테이블이 없다. → "현재 문서" 조회와 "이력 조회"를 어떤 쿼리로 구성할지 결정 필요.
4. **질문형 기록(PROMPT) 콘텐츠 소스가 스키마에 없다.** 프로토타입은 질문 풀을 하드코딩(L524 부근)한다. 스키마엔 `entries.prompt_snapshot`(작성 시점 스냅샷)만 있고 질문 마스터 테이블이 없다. → 정적 상수로 유지할지 콘텐츠 테이블을 새로 추가할지 확인 필요.
5. **정책·약관 문서 본문 소스가 스키마에 없다.** X03(정책 5종 화면)이 문서 본문을 렌더링하는데, `member_consents.document_version`만 있고 본문 저장소가 없다. → 정적 자산 배포 vs `GET /content/policies` API 여부 결정 필요.
6. **AI 피드백·신고 사유 코드값 매핑.** 프로토타입 `AGREE/DIFF/UNSURE` ↔ 스키마 `ai_user_feedback = RESONATES/DIFFERENT/UNSURE`. 신고 사유 프로토타입 `JUDGING/INAPPROPRIATE/RISKY/IRRELEVANT/ETC` ↔ 스키마 `reason_code`(자유 varchar). → 코드 표준 확정 필요.
7. **`RecordController`가 `docs/api/openapi.yaml`에 미반영.** 코드로는 이미 구현돼 있는데(태그·기록·정리 CRUD 다수) OpenAPI 스냅샷 재생성이 밀려 있다. → `./gradlew generateOpenApiDocs` 재생성 대상.
8. **홈/타임라인 카드용 응답이 원문 전용 DTO뿐이다.** 프로토타입 홈(L1033 부근)·타임라인(L1499 부근) 카드는 태그·대표 감정·AI 정리 상태·나의 생각 여부까지 한 번에 표시하는데, 현재 `EntryResponse`는 원문·메타만 반환한다. → 요약 전용 응답을 새로 만들지, 프론트에서 다중 호출로 조합할지 결정 필요(모바일 왕복 비용 고려).

## 기준 정보

프로토타입 실제 화면 구조(PRODUCT_CONTEXT.md의 Home/Records/Challenge/Me 명칭과 다르다):

| 프로토타입 코드 | 의미 | PRODUCT_CONTEXT 대응 |
|---|---|---|
| A/D | 인증·온보딩·탈퇴대기 | (공통) |
| H | 홈 | Home |
| C | 오늘의 체크인 | Home 하위 |
| R | 기록 작성 | Records |
| L | LifeTime(타임라인·통계·회고) | Records |
| E | 작은 실험 | Challenge |
| M | 내 정보 | Me |
| X | 공통 안전(위기·신고·정책) | (공통) |
| W | 위젯 | (공통) |

현재 구현된 백엔드 표면(컨트롤러 기준):

| 도메인 | 구현된 엔드포인트 | OpenAPI 반영 |
|---|---|---|
| Auth | `POST /auth/kakao/login`, `POST /auth/refresh`, `GET /auth/session`, `POST /auth/logout` | 반영됨 |
| Account | `POST /account/onboarding/complete` | 반영됨 |
| Content | `GET /content/quotes/today`, `GET /content/quotes/{id}`, `GET /content/topics`, `GET /content/topics/{id}/quotes`, `POST\|DELETE /content/quotes/{id}/save`, `GET /content/quotes/saved` | 반영됨 |
| Record | `GET /record/tags/system`, `GET /record/tags/mine`, `POST /record/tags`, `POST /record/entries`, `GET /record/entries`, `GET\|PATCH\|DELETE /record/entries/{id}`, `POST /record/entries/{id}/publish`, `GET\|POST /record/entries/{id}/tags`, `POST /entries/{id}/tags/{tagId}/confirm\|reject`, `GET\|POST /entries/{id}/reflections`, `PATCH /entries/{id}/reflections/{rid}` | **미반영** (§공백 7) |
| Health | `GET /api/v1/health` | 반영됨 |

## A. 시작 · 인증

| 흐름 | 필요 API (제안) | 관련 스키마 | 상태 | 비고 |
|---|---|---|---|---|
| A01 스플래시 분기(로그인/온보딩/정상/삭제대기) | `GET /auth/session` | members.status, onboarding_completed_at | 구현됨 | 응답에 status·온보딩 완료·삭제예정일 포함 여부 확인 필요 |
| A02 카카오 로그인 | `POST /auth/kakao/login` | social_identities, auth_sessions, members | 구현됨 | |
| A02 Google 로그인 | `POST /auth/google/login`(제안) | social_provider='GOOGLE' | 미구현 / 범위 확인 필요 | §공백 1 |
| A03–A05 온보딩(소개·안내·동의) | `POST /account/onboarding/complete`가 흡수 또는 별도 `POST /account/consents` | member_consents(TERMS/PRIVACY/AI_PROCESSING) | 부분 구현 | 동의 이력 저장 방식 확인 필요 |
| A06 온보딩 완료 → 홈 | `POST /account/onboarding/complete` | members.onboarding_completed_at | 구현됨 | |
| D01 삭제 대기 안내 | 세션 응답 확장 또는 `GET /account/me` | members.status, scheduled_deletion_at | 미구현 | |
| D02 탈퇴 철회 | `POST /account/withdrawal/cancel`(제안) | members, auth_sessions | 미구현 | 로그인만으로 자동 복구 금지, 명시적 확인 필요(계정 삭제 규칙) |

## H. 홈 · 오늘의 문장

| 흐름 | 필요 API (제안) | 관련 스키마 | 상태 | 비고 |
|---|---|---|---|---|
| 오늘 체크인 요약 카드 | `GET /checkins/today` | check_ins, check_in_emotions | 미구현 | |
| 오늘의 문장 카드 | `GET /content/quotes/today` | quotes, quote_topic_links | 구현됨 | |
| 최근 기록 3건(태그·대표감정 포함) | `GET /record/entries?limit=3` 응답 확장 | entries, entry_tags, tags | 부분 구현 | §공백 8과 동일 이슈 |
| 진행 중 작은 실험 요약 | `GET /experiments/active`(제안) | user_experiment_programs, user_program_missions | 미구현 | |
| 주간 회고 유도 배너 | `GET /weekly-reflections/current`(제안) | weekly_reflections | 미구현 | |
| 복귀 배너(마지막 기록 7일 이상 공백) | 홈 요약 응답 파생 필드 | entries | 미구현 | |
| 오늘의 문장 새로고침 | `GET /content/quotes/random`(제안) | quotes | 부분 구현 | 현재는 클라이언트 로컬 로테이션 |
| 문장 저장/취소 | `POST\|DELETE /content/quotes/{id}/save` | member_saved_quotes | 구현됨 | |
| 저장한 문장 목록 | `GET /content/quotes/saved` | member_saved_quotes | 구현됨 | |

## C. 오늘의 체크인 (전 구간 미구현)

| 흐름 | 필요 API (제안) | 관련 스키마 | 상태 | 비고 |
|---|---|---|---|---|
| 감정 팔레트 로드 | `GET /record/tags/system?category=EMOTION` | tags(scope=SYSTEM, category=EMOTION) | 부분 구현 | category 필터 파라미터 확인 필요 |
| 감정 직접 입력 | 커스텀 태그 or 체크인 자유 입력 | tags(scope=USER) / check_ins | 미구현 | |
| 체크인 저장(감정·강도·에너지·기억·감사·필요) | `PUT /checkins/{date}`(upsert, 제안) | check_ins, check_in_emotions | 미구현 | §공백 2(스케일 매핑) |
| 체크인 → entry 봉투 자동 생성 | 체크인 저장 시 entry_type=CHECK_IN 생성 | entries, check_ins.entry_id(1:1 UNIQUE) | 미구현 | |
| 체크인 완료 요약(+최근 7일 에너지) | `GET /checkins/today`, `GET /checkins?range=7`(제안) | check_ins | 미구현 | |

## R. 기록 작성

| 흐름 | 필요 API (제안) | 관련 스키마 | 상태 | 비고 |
|---|---|---|---|---|
| 기록 유형 선택(FREE/EMOTION/GRATITUDE/PROMPT/QUOTE_REFLECTION/EXPERIMENT_MISSION) | PROMPT용 `GET /content/prompts/today`(제안) | entry_type enum | 부분 구현 / 스키마 공백 | §공백 4 |
| 작성 저장 | `POST /record/entries` | entries | 구현됨 | OpenAPI 미반영(§공백 7) |
| "정리 없이 저장" 토글 | 요청 DTO에 aiProcessingAllowed 추가 필요 | entries.ai_processing_allowed | 부분 구현 / 계약 공백 | `EntryCreateRequest`에 필드 없음(엔티티엔 있음) |
| 임시 저장 | `POST /record/entries`(status=DRAFT) | entries.status='DRAFT' | 부분 구현 | 서버 DRAFT 흐름 매핑 결정 필요 |
| 수정 저장(최초 작성일 보존) | `PATCH /record/entries/{id}` | entries | 구현됨 | |
| 나로움의 정리 — 생성 요청/폴링 | `POST /record/entries/{id}/reflections:generate`, `GET .../reflection`(제안) | ai_reflections(status QUEUED→…→COMPLETED/FAILED) | 미구현 | |
| 정리 피드백(공감/다름/모름) | `PATCH /record/entries/{id}/reflection` | ai_reflections.user_feedback | 미구현 | §공백 6(코드값 매핑) |
| 정리 숨기기/다시보기 | `POST .../reflection/hide`, `/show`(제안) | ai_reflections.hidden_at | 미구현 | |
| 정리 재생성(자동 덮어쓰기 금지) | `POST .../reflection:regenerate`(제안) | ai_reflections.version_no(UNIQUE entry_id, version_no) | 미구현 | |
| 내 생각 추가(AI와 분리 저장) | `POST /record/entries/{id}/reflections` | entry_self_reflections | 구현됨 | |
| 키워드(태그) 확인 목록 | `GET /record/entries/{id}/tags` | entry_tags, tags | 구현됨 | |
| 태그 확인/거절/추가 | `POST .../confirm`, `/reject`, `POST /record/entries/{id}/tags` | entry_tags.state | 구현됨 | 남은 SUGGESTED 일괄 확정 벌크 API는 미확인 |
| 기록 완료 후 코스 추천 | `GET /experiments/recommendations?entryId=`(제안) | experiment_recommendations | 미구현 | 추천은 선택형, 자동 시작 금지 |

## X. 공통 · 안전

| 흐름 | 필요 API (제안) | 관련 스키마 | 상태 | 비고 |
|---|---|---|---|---|
| 위기 감지 안내 | AI 정리 응답에 safety_code 분기 | ai_reflections.safety_code | 미구현 | AI 원칙: 단정 금지, 기록 우선 저장 유지 |
| 응답 신고 | `POST /record/ai-reports`(제안) | ai_feedback_reports(member+ai_reflection UNIQUE) | 미구현 | §공백 6 |
| 정책·약관 문서 조회 | `GET /content/policies/{type}`(제안) 또는 정적 배포 | (문서 본문 소스 없음) | 미구현 / 스키마 공백 | §공백 5 |
| 알림 권한 안내 | `GET\|PUT /account/notification-preferences`(제안) | notification_preferences | 미구현 | |

## L. LifeTime

| 흐름 | 필요 API (제안) | 관련 스키마 | 상태 | 비고 |
|---|---|---|---|---|
| 타임라인(유형·감정·태그·AI상태 임베드) | `GET /record/entries` 리치 응답 확장 | entries, entry_tags, ai_reflections, check_ins | 부분 구현 | §공백 8 |
| 캘린더(월별 기록/체크인 유무) | `GET /record/calendar?year=&month=`(제안) | entries, check_ins | 미구현 | |
| 감정·에너지 통계(7/14/30일) | `GET /record/analytics/emotion-energy?range=`(제안) | check_ins, entry_tags | 미구현 | 순위/점수화 금지 원칙 반영 |
| 키워드 탐색(분포·카테고리별) | `GET /record/analytics/keywords`(제안) | entry_tags, tags | 미구현 | |
| 키워드 상세(관련 기록·동시 등장) | `GET /record/tags/{id}/entries`(제안) | entry_tags | 미구현 | "동시 등장 ≠ 인과" 카피 필요 |
| 기록 상세(원문+체크인+정리+내생각+태그) | 조합 응답 1콜 또는 다중 콜(결정 필요) | entries, check_ins, ai_reflections, entry_self_reflections, entry_tags | 부분 구현 | |
| 주간 회고(요약→키워드→흐름 확인→한문장) | `POST /weekly-reflections`, `GET/PATCH /weekly-reflections/{weekStart}`(제안) | weekly_reflections, weekly_reflection_entries | 미구현 | 주 7일 중 3건 미만 시 분기 필요 |
| 작은 실험 기록 상세 | `GET /experiments/{userProgramId}`(제안) | user_experiment_programs, user_program_missions, experiment_mission_records | 미구현 | |
| 나의 정리(현재 문서+이력) | `GET\|PUT /personal-summaries/current`(제안) | personal_summaries(scope=CURRENT_SELF) | 미구현 | §공백 3 |

## E. 작은 실험 (전 구간 미구현 — 가장 큰 미구현 영역)

| 흐름 | 필요 API (제안) | 관련 스키마 | 상태 | 비고 |
|---|---|---|---|---|
| 홈(진행중·저장·추천·3일/7일 목록) | `GET /experiments/home`, `GET /experiment-programs`(제안) | experiment_programs, user_experiment_programs | 미구현 | |
| 추천 코스(근거 문구) | `GET /experiments/recommendations`(제안) | experiment_recommendations | 미구현 | 근거는 단정 금지 |
| 주제별 코스 | `GET /experiment-programs?themeCode=`(제안) | experiment_programs, experiment_themes | 미구현 | |
| 코스 상세 | `GET /experiment-programs/{id}`(제안) | experiment_programs, program_missions | 미구현 | |
| 코스 구성 확인 / 미션 교체(시작 전) | `GET .../missions`, `GET .../missions/{day}/alternatives`(제안) | program_missions(replaceable, replacement_group) | 미구현 | |
| 코스 저장/시작 | `POST /experiments`(제안) | user_experiment_programs, user_program_missions | 미구현 | 활성 코스 1개 제약(UNIQUE) |
| 랜덤 코스 생성 | `POST /experiments/random?days=`(제안) | source_type='RANDOM' | 미구현 | 같은 유형 연속 금지 규칙 |
| 진행 중 코스 조회 | `GET /experiments/{id}`(제안) | user_experiment_programs, experiment_program_pauses | 미구현 | 달성률/연속일수 노출 금지 |
| 오늘의 작은 실험 | `GET /experiments/{id}/today`(제안) | user_program_missions(current_day) | 미구현 | |
| 오늘 미션 교체 / 오늘 쉬기 | `POST .../missions/{day}/replace`, `POST .../rest`(제안) | mission_replacement_events, experiment_program_pauses | 미구현 | 쉬기는 미션 미소비, 기간만 연장 |
| 오늘의 기록(수행상태·회고·감정) | `POST /experiments/{id}/missions/{day}/record`(제안) | experiment_mission_records, entries(EXPERIMENT_MISSION 봉투) | 미구현 | |
| 전체 진행 보기 | `GET /experiments/{id}/missions`(제안) | user_program_missions, experiment_mission_records | 미구현 | |
| 쉬기·변경·조기 종료 | `POST .../pause`, `POST .../end-early`(제안) | experiment_program_pauses, user_experiment_programs.status | 미구현 | |
| 코스 돌아보기 | `POST /experiments/{id}/review`(제안) | user_experiment_programs.review_data, final_review_entry_id | 미구현 | |
| 지난 작은 실험 목록 | `GET /experiments?status=COMPLETED,ENDED_EARLY`(제안) | user_experiment_programs | 미구현 | 성공률/완료율 비노출 |

## M. 내 정보 / W. 위젯

| 흐름 | 필요 API (제안) | 관련 스키마 | 상태 | 비고 |
|---|---|---|---|---|
| 내 정보 요약 | `GET /account/me`(제안) | members, social_identities | 미구현 | |
| 계정 정보 | `GET /account/me` | members, social_identities | 미구현 | |
| 알림 설정 | `GET\|PUT /account/notification-preferences`(제안) | notification_preferences | 미구현 | 기본값 전부 꺼짐 |
| 로그아웃 | `POST /auth/logout` | auth_sessions(revoked) | 구현됨 | |
| 계정·기록 삭제(3스텝+재확인) | `POST /account/withdrawal`(제안) | members(status, withdrawal_requested_at, scheduled_deletion_at), auth_sessions | 미구현 | 즉시 로그아웃, 7일 후 영구 삭제 |
| 기기/푸시 토큰 등록 | `POST /account/device-installations`(제안) | device_installations(push_token_ciphertext) | 미구현 | 프로토타입 화면엔 없으나 알림 기능에 필요 |
| 홈 위젯(문장·딥링크) | `GET /content/quotes/today` | quotes | 구현됨 | 위젯은 원문·감정 등 민감 데이터 비노출 |

## 요약

프로토타입에서 도출한 고유 백엔드 기능(엔드포인트 그룹) 약 55개 기준:

- **구현됨**: 약 13개 — 인증 4, 온보딩 1, 콘텐츠/문장 6, 기록 원문·태그·내생각 CRUD
- **부분 구현**(계약·응답 확장 필요): 약 8개
- **미구현**: 약 34개(약 62%) — 특히 체크인(C) 전체, AI 정리·신고·위기(R03/X01/X02) 전체, 작은 실험(E) 전체, LifeTime 집계/캘린더/주간회고/나의정리 전체, 알림설정·계정정보·탈퇴·기기등록 전체

스키마는 작은 실험·AI 정리 파이프라인 모두 이미 테이블이 갖춰져 있어, 계약만 확정하면 구현 가능한 상태다.
