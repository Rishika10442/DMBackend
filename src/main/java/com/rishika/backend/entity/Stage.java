package com.rishika.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stage")
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sid")
    private Long sid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pid", nullable = false)
    private Pipeline pipeline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actid", nullable = false)
    private Action action;

    @Column(name = "user_stage_id", nullable = false)
    private int userStageId;

    @Column(name = "name", nullable = false)
    private String name;

    @Lob
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; // Stored as JSON string
}
