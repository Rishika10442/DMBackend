package com.rishika.backend.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pipeline")
public class Pipeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pid")
    private Long pid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", nullable = false)
    private User user;

    @Column(name = "p_name", nullable = false)
    private String pName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Column(name = "status")
    private String status;  // values: "created" or "disabled"

    @OneToOne
    @JoinColumn(name = "first_sid") // references Stage.sid
    private Stage firstStage;


    @Lob
    @Column(name = "dag", columnDefinition = "TEXT")
    private String dag; // JSON string representation of DAG
}
