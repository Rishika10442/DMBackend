package com.rishika.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pipeline_x")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineX {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "px_id")
    private Long pxId;

    @ManyToOne
    @JoinColumn(name = "pid")
    private Pipeline pipeline;

    @ManyToOne
    @JoinColumn(name = "uid", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "first_sx_id")
    private StageX firstStageX;

    @ManyToOne
    @JoinColumn(name = "first_sid")
    private Stage firstStage;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "log_info", columnDefinition = "TEXT")
    private String logInfo;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "curr_sx_id")
    private StageX currentStageX;

    @Column(name = "status", nullable = false)
    private String status;

    @Lob
    @Column(name = "pipeline_progress", columnDefinition = "LONGTEXT")
    private String pipelineProgress; // Store JSON here (e.g., {"1": true, "2": false, ...})

    @Lob
    @Column(name = "dag", columnDefinition = "TEXT")
    private String dag; // JSON string representation of DAG\

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    @Column(name = "finished_at")
    private LocalDateTime finishedAt;


}
