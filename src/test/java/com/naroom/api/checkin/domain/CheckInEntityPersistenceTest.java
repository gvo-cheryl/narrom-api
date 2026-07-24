package com.naroom.api.checkin.domain;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.checkin.domain.entity.CheckIn;
import com.naroom.api.checkin.domain.repository.CheckInRepository;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryTag;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.entity.TagInitiator;
import com.naroom.api.record.domain.entity.TagSource;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.EntryTagRepository;
import com.naroom.api.record.domain.repository.TagRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * check_ins의 UNIQUE(member_id, check_in_date), 체크인 감정이 entry_tags(source=CHECK_IN)로
 * 통합 저장되는 것처럼 스키마 검증만으로는 확인되지 않는 실제 저장/조회 왕복을 검증한다.
 */
@SpringBootTest
@Transactional
@DirtiesContext
class CheckInEntityPersistenceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private CheckInRepository checkInRepository;

	@Autowired
	private EntryTagRepository entryTagRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void checkInAggregate_roundTripsThroughAllTables() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.CHECK_IN, null, null, LocalDate.now(), null, null, null));
		entry.publish();
		entryRepository.saveAndFlush(entry);

		CheckIn checkIn = checkInRepository.save(CheckIn.create(member, entry, LocalDate.now()));
		checkIn.update((short) 3, (short) 4, "기억에 남는 일", "감사한 일", "쉼", "자유메모");
		checkInRepository.saveAndFlush(checkIn);

		Tag emotionTag = tagRepository.save(
				Tag.createSystemTag(TagCategory.EMOTION, "테스트감정", "테스트감정" + System.nanoTime()));
		EntryTag selection = entryTagRepository.save(EntryTag.attachSystem(entry, emotionTag, TagSource.CHECK_IN));

		entityManager.flush();
		entityManager.clear();

		CheckIn reloaded = checkInRepository.findByMember_IdAndCheckInDate(member.getId(), LocalDate.now()).orElseThrow();
		assertEquals(entry.getId(), reloaded.getEntry().getId());
		assertEquals((short) 3, reloaded.getEmotionIntensity());
		assertEquals((short) 4, reloaded.getEnergyLevel());

		EntryTag reloadedSelection = entryTagRepository.findById(selection.getId()).orElseThrow();
		assertEquals(emotionTag.getId(), reloadedSelection.getTag().getId());
		assertEquals(TagSource.CHECK_IN, reloadedSelection.getSource());
		assertEquals(TagInitiator.USER_SELECTED, reloadedSelection.getInitiatedBy());
		assertEquals(1, entryTagRepository.findByEntry_IdAndSource(entry.getId(), TagSource.CHECK_IN).size());
	}

}
