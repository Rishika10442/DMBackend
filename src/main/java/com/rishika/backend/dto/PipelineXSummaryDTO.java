package com.rishika.backend.dto;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PipelineXSummaryDTO {
    private Long pxId;
    private String name;
    private String status;
}