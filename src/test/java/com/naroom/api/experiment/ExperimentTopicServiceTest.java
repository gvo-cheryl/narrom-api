package com.naroom.api.experiment;

import com.naroom.api.experiment.dto.ExperimentTopicResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// V16 시드(13개 주제)가 이미 적용돼 있다는 전제로, 별도 데이터를 만들지 않고 시드를 그대로 검증한다.
@SpringBootTest
@Transactional
class ExperimentTopicServiceTest {

	@Autowired
	private ExperimentTopicService experimentTopicService;

	@Test
	void listActive_returnsAllSeededTopicsOrderedByDisplayOrder() {
		List<ExperimentTopicResponse> topics = experimentTopicService.listActive();

		assertEquals(13, topics.size());
		assertEquals("EMOTION", topics.get(0).code());
		for (int i = 1; i < topics.size(); i++) {
			assertTrue(topics.get(i - 1).displayOrder() <= topics.get(i).displayOrder());
		}
	}

}
