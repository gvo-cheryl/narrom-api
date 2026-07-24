package com.naroom.api.record.domain.entity;

// source(어느 기능이 만들었는가: USER/AI/CHECK_IN/REFLECTION/EXPERIMENT)와는 다른 축.
// 이 태그를 처음 제안한 주체가 누구인지를 나타낸다.
public enum TagInitiator {
	USER_SELECTED,
	USER_ENTERED,
	AI_INFERRED
}
