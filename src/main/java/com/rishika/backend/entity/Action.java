package com.rishika.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "action")
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "act_id")
    private Long actId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type;

    @Lob
    @Column(name = "source_code", columnDefinition = "TEXT")
    private String sourceCode;

    @Column(name = "domain", nullable = false)
    private String domain;


    @Column(name = "message")
    private String message;// "sql", "business", etc.

    @Column(name = "flag", nullable = false)
    private Boolean flag; // "sql", "business", etc.

    @Lob
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; // JSON stored as string
}
