package com.naroom.api.ai.prompt;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.GenerationResult;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.domain.entity.MemberAiPreference;
import com.naroom.api.ai.domain.repository.MemberAiPreferenceRepository;
import com.naroom.api.ai.dto.AiJobResponse;
import com.naroom.api.ai.outcome.EntryReflectionGenerationContext;
import com.naroom.api.ai.outcome.EntryReflectionOutcomeService;
import com.naroom.api.ai.result.EntryReflectionResult;
import com.naroom.api.checkin.CheckInService;
import com.naroom.api.checkin.dto.CheckInUpsertRequest;
import com.naroom.api.record.EntryTagService;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryTag;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.EntrySelfReflection;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.EntrySelfReflectionRepository;
import com.naroom.api.record.domain.repository.EntryTagRepository;
import com.naroom.api.record.domain.repository.TagRepository;
import com.naroom.api.record.dto.EntryTagResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class PromptAssemblerTest {

	@Autowired
	private PromptAssembler promptAssembler;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private EntryTagService entryTagService;

	@Autowired
	private EntryTagRepository entryTagRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private EntrySelfReflectionRepository entrySelfReflectionRepository;

	@Autowired
	private MemberAiPreferenceRepository memberAiPreferenceRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CheckInService checkInService;

	@Autowired
	private AiJobService aiJobService;

	@Autowired
	private EntryReflectionOutcomeService entryReflectionOutcomeService;

	@Test
	void assembleForPeriodReflection_includesCheckInTrendAndEvidenceBody() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate periodStart = LocalDate.of(2026, 7, 6);
		LocalDate periodEnd = LocalDate.of(2026, 7, 12);
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "이번 주 있었던 일", periodStart.plusDays(1), null, null, null));
		entry.publish();
		entryRepository.saveAndFlush(entry);
		checkInService.upsertCheckIn(member.getId(), new CheckInUpsertRequest(
				periodStart, (short) 4, (short) 2, null, null, null, null, List.of()));

		AssembledPrompt prompt = promptAssembler.assembleForPeriodReflection(
				member, AiFeatureType.WEEKLY_REFLECTION, periodStart, periodEnd, List.of(entry));

		assertEquals("weekly-reflection-v1", prompt.featureInstructionsVersion());
		assertEquals("period-reflection-schema-v1", prompt.outputSchemaVersion());
		assertTrue(prompt.contextContent().contains("이번 주 있었던 일"));
		assertTrue(prompt.contextContent().contains("감정 강도=4"));
		assertTrue(prompt.contextContent().contains("에너지=2"));
	}

	@Test
	void assembleForPeriodReflection_includesConfirmedTagsAiSummaryAndSelfReflectionPerEntry() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate periodStart = LocalDate.of(2026, 7, 6);
		LocalDate periodEnd = LocalDate.of(2026, 7, 12);
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", periodStart.plusDays(1), null, null, null));
		entry.publish();
		entryRepository.saveAndFlush(entry);
		Tag emotionTag = tagRepository.save(
				Tag.createUserTag(member, TagCategory.EMOTION, "안도감", "안도감" + System.nanoTime()));
		EntryTagResponse attached = entryTagService.attachUserTag(member.getId(), entry.getId(), emotionTag.getId());
		entryTagService.confirmTag(member.getId(), entry.getId(), attached.id());
		entrySelfReflectionRepository.save(EntrySelfReflection.create(entry, "돌이켜보니 잘 넘겼다"));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		AiJobResponse claimed = aiJobService.claimNextBatch(10).get(0);
		EntryReflectionResult parsedResult = new EntryReflectionResult(
				"차분히 마무리한 하루", List.of(), List.of(), List.of(), "질문", List.of(entry.getId()), AiSafetyGrade.NORMAL);
		entryReflectionOutcomeService.persist(new EntryReflectionGenerationContext(
				claimed.id(), claimed.startedAt(), entry.getId(), 1, "gpt-5.6-luna",
				"v-" + System.nanoTime() + "-common", "v-feature", "v-schema",
				AiSafetyGrade.NORMAL, AiSafetyGrade.NORMAL,
				new GenerationResult("{}", 100, 30), parsedResult, 700));

		AssembledPrompt prompt = promptAssembler.assembleForPeriodReflection(
				member, AiFeatureType.WEEKLY_REFLECTION, periodStart, periodEnd, List.of(entry));

		assertTrue(prompt.contextContent().contains("안도감"));
		assertTrue(prompt.contextContent().contains("돌이켜보니 잘 넘겼다"));
		assertTrue(prompt.contextContent().contains("차분히 마무리한 하루"));
	}

	@Test
	void assembleForEntryReflection_setsVersionsAndIncludesEntryBody() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, "제목", "오늘은 힘든 하루였다", LocalDate.now(), null, null, null));

		AssembledPrompt prompt = promptAssembler.assembleForEntryReflection(entry.getId());

		assertEquals(AiInstructionCatalog.COMMON_INSTRUCTIONS_VERSION, prompt.commonInstructionsVersion());
		assertEquals("entry-reflection-v2", prompt.featureInstructionsVersion());
		assertEquals("entry-reflection-schema-v1", prompt.outputSchemaVersion());
		assertTrue(prompt.contextContent().contains("오늘은 힘든 하루였다"));
		assertTrue(prompt.contextContent().contains("제목"));
	}

	@Test
	void assembleForEntryReflection_includesConfirmedTagsGroupedByCategory() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		Tag emotionTag = tagRepository.save(
				Tag.createUserTag(member, TagCategory.EMOTION, "답답함", "답답함" + System.nanoTime()));
		Tag situationTag = tagRepository.save(
				Tag.createUserTag(member, TagCategory.SITUATION, "직장", "직장" + System.nanoTime()));
		EntryTagResponse attachedEmotion = entryTagService.attachUserTag(member.getId(), entry.getId(), emotionTag.getId());
		EntryTagResponse attachedSituation = entryTagService.attachUserTag(member.getId(), entry.getId(), situationTag.getId());
		entryTagService.confirmTag(member.getId(), entry.getId(), attachedEmotion.id());
		entryTagService.confirmTag(member.getId(), entry.getId(), attachedSituation.id());

		AssembledPrompt prompt = promptAssembler.assembleForEntryReflection(entry.getId());

		assertTrue(prompt.contextContent().contains("[사용자가 확정한 감정] 답답함"));
		assertTrue(prompt.contextContent().contains("[사용자가 확정한 태그] 직장"));
	}

	@Test
	void assembleForEntryReflection_excludesSuggestedAndRejectedTags() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		Tag suggestedTag = tagRepository.save(
				Tag.createUserTag(member, TagCategory.CUSTOM, "제안됨", "제안됨" + System.nanoTime()));
		Tag rejectedTag = tagRepository.save(
				Tag.createUserTag(member, TagCategory.CUSTOM, "거부됨", "거부됨" + System.nanoTime()));
		entryTagRepository.save(EntryTag.suggestByAi(entry, suggestedTag, null, null, null, null));
		EntryTagResponse attachedRejected = entryTagService.attachUserTag(member.getId(), entry.getId(), rejectedTag.getId());
		entryTagService.rejectTag(member.getId(), entry.getId(), attachedRejected.id());

		AssembledPrompt prompt = promptAssembler.assembleForEntryReflection(entry.getId());

		assertFalse(prompt.contextContent().contains("제안됨"));
		assertFalse(prompt.contextContent().contains("거부됨"));
	}

	@Test
	void assembleForEntryReflection_includesSelfReflectionContent() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		entrySelfReflectionRepository.save(EntrySelfReflection.create(entry, "돌이켜보니 그때 내가 원했던 건 휴식이었다"));

		AssembledPrompt prompt = promptAssembler.assembleForEntryReflection(entry.getId());

		assertTrue(prompt.contextContent().contains("돌이켜보니 그때 내가 원했던 건 휴식이었다"));
	}

	@Test
	void assembleForEntryReflection_appliesMemberPreference() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		MemberAiPreference preference = MemberAiPreference.createDefault(member);
		preference.update("DIRECT", "SHORT", true);
		memberAiPreferenceRepository.save(preference);

		AssembledPrompt prompt = promptAssembler.assembleForEntryReflection(entry.getId());

		assertTrue(prompt.preferenceInstructions().contains("DIRECT"));
		assertTrue(prompt.preferenceInstructions().contains("SHORT"));
		assertTrue(prompt.preferenceInstructions().contains("위로·공감 표현을 줄이고"));
	}

	@Test
	void assembleForEntryReflection_noPreferenceSet_returnsEmptyPreferenceInstructions() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		AssembledPrompt prompt = promptAssembler.assembleForEntryReflection(entry.getId());

		assertEquals("", prompt.preferenceInstructions());
	}

	@Test
	void assembleForEntryReflection_aiProcessingDisallowed_throwsIllegalStateException() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null);
		entry.disallowAiProcessing();
		entryRepository.save(entry);

		assertThrows(IllegalStateException.class, () -> promptAssembler.assembleForEntryReflection(entry.getId()));
	}

}
