package com.hireconnect.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.notification.dto.request.SendNotificationRequest;
import com.hireconnect.notification.dto.response.NotificationResponse;
import com.hireconnect.notification.dto.response.UnreadCountResponse;
import com.hireconnect.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private NotificationService notificationService;

	@Autowired
	private ObjectMapper objectMapper;

	private Authentication authentication() {
		Authentication auth = Mockito.mock(Authentication.class);

		when(auth.getName()).thenReturn("1");

		return auth;
	}

	private NotificationResponse notificationResponse() {

		return NotificationResponse.builder().notificationId(1L).userId(1L).type("INFO").title("Test Notification")
				.message("Test Message").isRead(false).referenceId(10L).referenceType("JOB").actionUrl("/jobs/10")
				.createdAt(LocalDateTime.now()).build();
	}

	@Test
	@DisplayName("should get my unread count")
	void shouldGetMyUnreadCount() throws Exception {

		when(notificationService.getUnreadCount(1L)).thenReturn(new UnreadCountResponse(1L, 5));

		mockMvc.perform(get("/api/v1/notifications/unread/count").principal(authentication()))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("should send notification")
	void shouldSendNotification() throws Exception {

		SendNotificationRequest request = new SendNotificationRequest();

		request.setUserId(1L);
		request.setTitle("Test Title");
		request.setMessage("Test Message");
		request.setType("INFO");

		when(notificationService.sendNotification(any())).thenReturn(notificationResponse());

		mockMvc.perform(post("/api/v1/notifications").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("should get my notifications")
	void shouldGetMyNotifications() throws Exception {

		when(notificationService.getNotificationsByUser(eq(1L), any()))
				.thenReturn(new PageImpl<>(List.of(notificationResponse())));

		mockMvc.perform(get("/api/v1/notifications/my").principal(authentication())).andExpect(status().isOk());
	}

	@Test
	@DisplayName("should get notifications by user")
	void shouldGetNotificationsByUser() throws Exception {

		when(notificationService.getNotificationsByUser(eq(1L), any()))
				.thenReturn(new PageImpl<>(List.of(notificationResponse())));

		mockMvc.perform(get("/api/v1/notifications/user/1")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("should get unread notifications")
	void shouldGetUnreadNotifications() throws Exception {

		when(notificationService.getUnreadNotificationsByUser(1L)).thenReturn(List.of(notificationResponse()));

		mockMvc.perform(get("/api/v1/notifications/user/1/unread")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("should get unread count")
	void shouldGetUnreadCount() throws Exception {

		when(notificationService.getUnreadCount(1L)).thenReturn(new UnreadCountResponse(1L, 3));

		mockMvc.perform(get("/api/v1/notifications/user/1/unread/count")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("should mark notification as read")
	void shouldMarkAsRead() throws Exception {

		when(notificationService.markAsRead(1L, 1L)).thenReturn(notificationResponse());

		mockMvc.perform(patch("/api/v1/notifications/1/read").header("X-User-Id", 1)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("should mark my notifications as read")
	void shouldMarkMyNotificationsAsRead() throws Exception {

		when(notificationService.markAllAsRead(1L)).thenReturn(5);

		mockMvc.perform(patch("/api/v1/notifications/read-all").principal(authentication())).andExpect(status().isOk());
	}

	@Test
	@DisplayName("should mark all notifications as read")
	void shouldMarkAllNotificationsAsRead() throws Exception {

		when(notificationService.markAllAsRead(1L)).thenReturn(3);

		mockMvc.perform(patch("/api/v1/notifications/user/1/read-all")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("should delete notification")
	void shouldDeleteNotification() throws Exception {

		doNothing().when(notificationService).deleteNotification(1L, 1L);

		mockMvc.perform(delete("/api/v1/notifications/1").header("X-User-Id", 1)).andExpect(status().isOk());
	}
}