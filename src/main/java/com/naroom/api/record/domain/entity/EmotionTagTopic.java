package com.naroom.api.record.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

// 체크인 감정 선택 화면에서 감정 태그를 묶어 보여주는 표시 주제.
// tags 테이블과는 EmotionTagTopicLink를 통해서만 연결되며, tags 자체는 참조하지 않는다.
@Entity
@Table(name = "emotion_tag_topics")
public class EmotionTagTopic {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "code", nullable = false, updatable = false, length = 50)
	private String code;

	@Column(name = "name", nullable = false, length = 80)
	private String name;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	protected EmotionTagTopic() {
	}

	private EmotionTagTopic(String code, String name, int displayOrder) {
		this.code = code;
		this.name = name;
		this.displayOrder = displayOrder;
	}

	public static EmotionTagTopic create(String code, String name, int displayOrder) {
		return new EmotionTagTopic(code, name, displayOrder);
	}

	public UUID getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

}
