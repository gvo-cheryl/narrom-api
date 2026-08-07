package com.naroom.api.notification.infra.expo;

import java.util.List;

public record ExpoPushTicketResponse(List<Ticket> data) {

	public record Ticket(String status, String id, String message) {
	}

}
