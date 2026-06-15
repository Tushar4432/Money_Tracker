package com.tracker.MoneyTracker.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {

    @Id
    private String id;
    private String userId;

    @Column(name = "type")
    private String type;

    private String title;
    private String message;
    private boolean read;
    private String referenceId;
    private LocalDateTime createdAt;
}
