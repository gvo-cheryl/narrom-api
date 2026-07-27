package com.naroom.api.ai.result;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class EntryReflectionResponseParserTest {

	@Autowired
	private EntryReflectionResponseParser parser;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void parse_matchesSystemTagsAndVerifiesEvidenceOwnership() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		// 시스템 태그 이름은 uq_tags_system_name(category, normalized_name) 유니크 제약이 있고 V6 마이그레이션이
		// 이미 표준 감정 태그를 시딩해뒀으므로, 실제 시드 데이터와 충돌하지 않도록 유니크한 이름을 만들어 쓴다.
		String emotionName = "답답함-" + System.nanoTime();
		String tagName = "직장-" + System.nanoTime();
		tagRepository.save(Tag.createSystemTag(TagCategory.EMOTION, emotionName, emotionName));
		tagRepository.save(Tag.createSystemTag(TagCategory.SITUATION, tagName, tagName));

		String json = """
				{
				  "summary": "오늘은 직장에서 답답함을 느꼈다",
				  "emotionCandidates": [{"name": "%s", "confidence": 0.8}],
				  "suggestedTagNames": ["%s"],
				  "reflectionQuestion": "그 상황에서 실제로 원했던 건 무엇이었나요?",
				  "evidenceEntryIds": ["%s"],
				  "safetyStatus": "NORMAL"
				}
				""".formatted(emotionName, tagName, entry.getId());

		EntryReflectionResult result = parser.parse(json, member.getId());

		assertEquals("오늘은 직장에서 답답함을 느꼈다", result.summary());
		assertEquals(1, result.emotionCandidates().size());
		assertTrue(result.emotionCandidates().get(0).isMapped());
		assertEquals(emotionName, result.emotionCandidates().get(0).matchedTag().getName());
		assertEquals(1, result.suggestedTags().size());
		assertEquals(tagName, result.suggestedTags().get(0).getName());
		assertEquals(List.of(entry.getId()), result.evidenceEntryIds());
		assertEquals(AiSafetyGrade.NORMAL, result.modelReportedSafetyStatus());
	}

	@Test
	void parse_unmappedEmotionAndTagNames_areKeptSeparately() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		String json = """
				{
				  "summary": "요약",
				  "emotionCandidates": [{"name": "존재하지않는감정", "confidence": 0.5}],
				  "suggestedTagNames": ["존재하지않는태그"],
				  "reflectionQuestion": "질문",
				  "evidenceEntryIds": [],
				  "safetyStatus": "NORMAL"
				}
				""";

		EntryReflectionResult result = parser.parse(json, member.getId());

		assertTrue(result.emotionCandidates().get(0).matchedTag() == null);
		assertEquals(0, result.suggestedTags().size());
		assertEquals(1, result.unmappedTagNames().size());
		assertEquals("존재하지않는태그", result.unmappedTagNames().get(0));
	}

	@Test
	void parse_confidenceOutOfRange_isFilteredOut() {
		Member member = memberRepository.save(Member.create("지연"));

		String json = """
				{
				  "summary": "요약",
				  "emotionCandidates": [{"name": "답답함", "confidence": 1.5}],
				  "suggestedTagNames": [],
				  "reflectionQuestion": "질문",
				  "evidenceEntryIds": [],
				  "safetyStatus": "NORMAL"
				}
				""";

		EntryReflectionResult result = parser.parse(json, member.getId());

		assertEquals(0, result.emotionCandidates().size());
	}

	@Test
	void parse_evidenceEntryNotOwnedByMember_isExcluded() {
		Member owner = memberRepository.save(Member.create("소유자"));
		Member intruder = memberRepository.save(Member.create("다른회원"));
		Entry entry = entryRepository.save(
				Entry.create(owner, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		String json = """
				{
				  "summary": "요약",
				  "emotionCandidates": [],
				  "suggestedTagNames": [],
				  "reflectionQuestion": "질문",
				  "evidenceEntryIds": [%s],
				  "safetyStatus": "NORMAL"
				}
				""".formatted("\"" + entry.getId() + "\"");

		EntryReflectionResult result = parser.parse(json, intruder.getId());

		assertEquals(0, result.evidenceEntryIds().size());
	}

	@Test
	void parse_blankSummary_throwsIllegalArgumentException() {
		Member member = memberRepository.save(Member.create("지연"));
		String json = """
				{
				  "summary": "",
				  "emotionCandidates": [],
				  "suggestedTagNames": [],
				  "reflectionQuestion": "질문",
				  "evidenceEntryIds": [],
				  "safetyStatus": "NORMAL"
				}
				""";

		assertThrows(IllegalArgumentException.class, () -> parser.parse(json, member.getId()));
	}

	@Test
	void parse_malformedJson_throwsIllegalArgumentException() {
		Member member = memberRepository.save(Member.create("지연"));

		assertThrows(IllegalArgumentException.class, () -> parser.parse("이건 JSON이 아니다", member.getId()));
	}

	@Test
	void parse_unknownSafetyStatus_returnsNullModelReportedSafetyStatus() {
		Member member = memberRepository.save(Member.create("지연"));
		String json = """
				{
				  "summary": "요약",
				  "emotionCandidates": [],
				  "suggestedTagNames": [],
				  "reflectionQuestion": "질문",
				  "evidenceEntryIds": [],
				  "safetyStatus": "알수없음"
				}
				""";

		EntryReflectionResult result = parser.parse(json, member.getId());

		assertNull(result.modelReportedSafetyStatus());
	}

}
