package com.rishika.backend.entity;

import com.rishika.backend.enums.StageOutcome;
import com.rishika.backend.enums.DependencyType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stage_x_dependency")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageXDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sx_id", nullable = false)
    private StageX stageX;

    @ManyToOne
    @JoinColumn(name = "depends_on_sxid", nullable = false)
    private StageX dependsOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_outcome", nullable = false)
    private StageOutcome stageOutcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false)
    private DependencyType dependencyType;
}
