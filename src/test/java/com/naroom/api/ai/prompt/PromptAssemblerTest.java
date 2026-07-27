package com.naroom.api.ai.prompt;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.MemberAiPreference;
import com.naroom.api.ai.domain.repository.MemberAiPreferenceRepository;
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

	@Test
	void assembleForEntryReflection_setsVersionsAndIncludesEntryBody() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, "제목", "오늘은 힘든 하루였다", LocalDate.now(), null, null, null));

		AssembledPrompt prompt = promptAssembler.assembleForEntryReflection(entry.getId());

		assertEquals(AiInstructionCatalog.COMMON_INSTRUCTIONS_VERSION, prompt.commonInstructionsVersion());
		assertEquals("entry-reflection-v1", prompt.featureInstructionsVersion());
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
