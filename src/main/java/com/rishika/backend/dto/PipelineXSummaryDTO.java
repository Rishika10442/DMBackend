package com.rishika.backend.dto;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PipelineXSummaryDTO {
    private Long pxId;
    private String name;
    private String status;
    private LocalDateTime createdAt;
}