package com.naroom.api.experiment.domain.entity;

// ExperimentProgram.sourceType과 UserExperimentProgram.configurationSource가 공유한다 - 코스 원본의
// 출처와 사용자가 실제로 시작한 경로는 같은 값 집합이다.
public enum ExperimentSourceType {
	TEMPLATE,
	RANDOM,
	AI_RECOMMENDED,
	USER_COMPOSED
}
