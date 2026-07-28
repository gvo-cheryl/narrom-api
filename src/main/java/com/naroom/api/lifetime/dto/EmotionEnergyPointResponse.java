package com.naroom.api.lifetime.dto;

import java.time.LocalDate;

// 체크인이 없는 날은 포함하지 않는다(0점으로 표시하지 않는다는 LifeTime 제품 원칙) - 그래프는 실제 기록이
// 있는 날짜만 점으로 잇는다.
public record EmotionEnergyPointResponse(LocalDate date, Short emotionIntensity, Short energyLevel) {
}
