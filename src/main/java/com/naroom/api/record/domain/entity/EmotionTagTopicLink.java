package com.naroom.api.record.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.UUID;

// 감정(EMOTION) 분류 태그 하나가 속하는 표시 주제. 태그당 주제는 하나뿐이라 tag_id를 그대로 PK로 쓴다.
@Entity
@Table(name = "emotion_tag_topic_links")
public class EmotionTagTopicLink {

	@Id
	@Column(name = "tag_id", nullable = false, updatable = false)
	private UUID tagId;

	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	@JoinColumn(name = "tag_id", nullable = false, updatable = false)
	private Tag tag;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "topic_id", nullable = false)
	private EmotionTagTopic topic;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	protected EmotionTagTopicLink() {
	}

	private EmotionTagTopicLink(Tag tag, EmotionTagTopic topic, int displayOrder) {
		this.tag = tag;
		this.topic = topic;
		this.displayOrder = displayOrder;
	}

	public static EmotionTagTopicLink assign(Tag tag, EmotionTagTopic topic, int displayOrder) {
		return new EmotionTagTopicLink(tag, topic, displayOrder);
	}

	public UUID getTagId() {
		return tagId;
	}

	public Tag getTag() {
		return tag;
	}

	public EmotionTagTopic getTopic() {
		return topic;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

}
