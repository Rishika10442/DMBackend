package com.rishika.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stage_x")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageX {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sx_id")
    private Long sxId;

    @ManyToOne
    @JoinColumn(name = "sid", nullable = false)
    private Stage stage;

    @Column(name = "user_stage_id", nullable = false)
    private Integer userStageId;

    @ManyToOne
    @JoinColumn(name = "px_id", nullable = false)
    private PipelineX pipelineX;

    @ManyToOne
    @JoinColumn(name = "uid", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "pid", nullable = false)
    private Pipeline pipeline;

    @ManyToOne
    @JoinColumn(name = "actid", nullable = false)
    private Action action;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "stage_name", nullable = false)
    private String stageName;

    @Lob
    @Column(name = "payload", columnDefinition = "LONGTEXT")
    private String payload;
}
