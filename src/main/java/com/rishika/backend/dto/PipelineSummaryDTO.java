package com.rishika.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PipelineSummaryDTO {
    private Long pid;
    private String pName;
    private LocalDateTime createdAt;
}
