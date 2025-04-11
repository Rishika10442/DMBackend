package com.rishika.backend.entity;

import com.rishika.backend.enums.DependencyType;
import com.rishika.backend.enums.StageOutcome;
import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stage_dependencies")
public class StageDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // The stage that depends on another stage (i.e., child)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sid", nullable = false)
    private Stage stage;

    // The stage that this one depends on (i.e., parent)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depends_on_sid", nullable = false)
    private Stage dependsOn;

    @Column(name = "stage_outcome", nullable = false)
    @Enumerated(EnumType.STRING)
    private StageOutcome stageOutcome;

    @Enumerated(EnumType.STRING)
    private DependencyType dependencyType; // Example values: ALWAYS, CONDITIONAL


}
