package com.rishika.backend.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PipelineSummaryDTO {
    private Long pid;
    private String pName;
}
