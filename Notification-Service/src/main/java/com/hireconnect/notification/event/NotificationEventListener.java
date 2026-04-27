package com.hireconnect.notification.event;

import com.hireconnect.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notification.queue")
    public void handleNotificationEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            log.info("Received event: {}", eventType);

            switch (eventType) {
                case "APPLICATION_SUBMITTED"      -> handleApplicationSubmitted(event);
                case "APPLICATION_STATUS_CHANGED" -> handleApplicationStatusChanged(event);
                case "APPLICATION_WITHDRAWN"      -> handleApplicationWithdrawn(event);
                case "INTERVIEW_SCHEDULED"        -> handleInterviewScheduled(event);
                case "INTERVIEW_CONFIRMED"        -> handleInterviewConfirmed(event);
                case "INTERVIEW_RESCHEDULED"      -> handleInterviewRescheduled(event);
                case "INTERVIEW_CANCELLED"        -> handleInterviewCancelled(event);
                default -> log.warn("Unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
        }
    }

    private void handleApplicationSubmitted(Map<String, Object> event) {
        Long candidateId  = toLong(event.get("candidateId"));
        String email      = (String) event.get("candidateEmail");
        String jobTitle   = (String) event.get("jobTitle");
        String company    = (String) event.get("companyName");
        Long appId        = toLong(event.get("applicationId"));

        // Notify candidate
        notificationService.sendNotification(
                candidateId, email,
                "APPLICATION_SUBMITTED",
                "Application Submitted Successfully",
                "Your application for " + jobTitle + " at " + company + " has been submitted.",
                appId, "APPLICATION", "/candidate/applications/" + appId
        );

        // Notify recruiter
        Long recruiterId  = toLong(event.get("recruiterId"));
        String candidateName = (String) event.get("candidateName");
        if (recruiterId != null) {
            notificationService.sendNotification(
                    recruiterId, null,
                    "NEW_APPLICATION",
                    "New Application Received",
                    candidateName + " has applied for " + jobTitle + ".",
                    appId, "APPLICATION", "/recruiter/applications/" + appId
            );
        }
    }

    private void handleApplicationStatusChanged(Map<String, Object> event) {
        Long candidateId = toLong(event.get("candidateId"));
        String email     = (String) event.get("candidateEmail");
        String jobTitle  = (String) event.get("jobTitle");
        String newStatus = (String) event.get("newStatus");
        Long appId       = toLong(event.get("applicationId"));

        String title   = buildStatusChangeTitle(newStatus);
        String message = buildStatusChangeMessage(jobTitle, newStatus);

        notificationService.sendNotification(
                candidateId, email,
                "APPLICATION_STATUS_CHANGED",
                title, message,
                appId, "APPLICATION", "/candidate/applications/" + appId
        );
    }

    private void handleApplicationWithdrawn(Map<String, Object> event) {
        Long recruiterId = toLong(event.get("recruiterId"));
        String jobTitle  = (String) event.get("jobTitle");
        String cName     = (String) event.get("candidateName");
        Long appId       = toLong(event.get("applicationId"));

        if (recruiterId != null) {
            notificationService.sendNotification(
                    recruiterId, null,
                    "APPLICATION_WITHDRAWN",
                    "Application Withdrawn",
                    cName + " has withdrawn their application for " + jobTitle + ".",
                    appId, "APPLICATION", "/recruiter/applications/" + appId
            );
        }
    }

    private void handleInterviewScheduled(Map<String, Object> event) {
        Long candidateId   = toLong(event.get("candidateId"));
        String email       = (String) event.get("candidateEmail");
        String jobTitle    = (String) event.get("jobTitle");
        String company     = (String) event.get("companyName");
        String scheduledAt = (String) event.get("scheduledAt");
        String mode        = (String) event.get("mode");
        Long interviewId   = toLong(event.get("interviewId"));

        notificationService.sendNotification(
                candidateId, email,
                "INTERVIEW_SCHEDULED",
                "Interview Scheduled — " + jobTitle,
                "Your interview for " + jobTitle + " at " + company +
                " is scheduled on " + scheduledAt + " (" + mode + "). Please confirm.",
                interviewId, "INTERVIEW", "/candidate/interviews/" + interviewId
        );
    }

    private void handleInterviewConfirmed(Map<String, Object> event) {
        Long recruiterId   = toLong(event.get("recruiterId"));
        String candidateName = (String) event.get("candidateName");
        String jobTitle    = (String) event.get("jobTitle");
        Long interviewId   = toLong(event.get("interviewId"));

        if (recruiterId != null) {
            notificationService.sendNotification(
                    recruiterId, null,
                    "INTERVIEW_CONFIRMED",
                    "Interview Confirmed",
                    candidateName + " has confirmed the interview for " + jobTitle + ".",
                    interviewId, "INTERVIEW", "/recruiter/interviews/" + interviewId
            );
        }
    }

    private void handleInterviewRescheduled(Map<String, Object> event) {
        Long candidateId   = toLong(event.get("candidateId"));
        String email       = (String) event.get("candidateEmail");
        Long recruiterId   = toLong(event.get("recruiterId"));
        String jobTitle    = (String) event.get("jobTitle");
        String scheduledAt = (String) event.get("scheduledAt");
        Long interviewId   = toLong(event.get("interviewId"));

        notificationService.sendNotification(
                candidateId, email,
                "INTERVIEW_RESCHEDULED",
                "Interview Rescheduled — " + jobTitle,
                "Your interview for " + jobTitle + " has been rescheduled to " + scheduledAt + ".",
                interviewId, "INTERVIEW", "/candidate/interviews/" + interviewId
        );
        if (recruiterId != null) {
            notificationService.sendNotification(
                    recruiterId, null,
                    "INTERVIEW_RESCHEDULED",
                    "Interview Rescheduled",
                    "Interview for " + jobTitle + " has been rescheduled to " + scheduledAt + ".",
                    interviewId, "INTERVIEW", "/recruiter/interviews/" + interviewId
            );
        }
    }

    private void handleInterviewCancelled(Map<String, Object> event) {
        Long candidateId = toLong(event.get("candidateId"));
        String email     = (String) event.get("candidateEmail");
        Long recruiterId = toLong(event.get("recruiterId"));
        String jobTitle  = (String) event.get("jobTitle");
        Long interviewId = toLong(event.get("interviewId"));

        notificationService.sendNotification(
                candidateId, email,
                "INTERVIEW_CANCELLED",
                "Interview Cancelled — " + jobTitle,
                "Your interview for " + jobTitle + " has been cancelled.",
                interviewId, "INTERVIEW", "/candidate/interviews/" + interviewId
        );
        if (recruiterId != null) {
            notificationService.sendNotification(
                    recruiterId, null,
                    "INTERVIEW_CANCELLED",
                    "Interview Cancelled",
                    "The interview for " + jobTitle + " has been cancelled.",
                    interviewId, "INTERVIEW", "/recruiter/interviews/" + interviewId
            );
        }
    }

    // ---------- helpers ----------

    private String buildStatusChangeTitle(String status) {
        return switch (status) {
            case "SHORTLISTED"          -> "🎉 You've been Shortlisted!";
            case "INTERVIEW_SCHEDULED"  -> "📅 Interview Scheduled";
            case "OFFERED"              -> "🏆 Offer Received!";
            case "REJECTED"             -> "Application Update";
            default                     -> "Application Status Updated";
        };
    }

    private String buildStatusChangeMessage(String jobTitle, String status) {
        return switch (status) {
            case "SHORTLISTED"          ->
                "Congratulations! You have been shortlisted for " + jobTitle + ". The recruiter will contact you soon.";
            case "INTERVIEW_SCHEDULED"  ->
                "An interview has been scheduled for your application to " + jobTitle + ". Please check your interview details.";
            case "OFFERED"              ->
                "Great news! You have received a job offer for " + jobTitle + ". Please check your dashboard for details.";
            case "REJECTED"             ->
                "Thank you for applying for " + jobTitle + ". Unfortunately, the recruiter has decided not to move forward at this time.";
            default                     ->
                "Your application status for " + jobTitle + " has been updated to " + status + ".";
        };
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString()); } catch (Exception e) { return null; }
    }
}
