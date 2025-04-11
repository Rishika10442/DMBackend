package com.rishika.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record PipelineRequest(

        @NotNull(message = "User ID is required")
        @JsonProperty("userId")
        Long userId,

        @NotBlank(message = "Pipeline name is required")
        @JsonProperty("pName")
        String pName,



        @NotBlank(message = "DAG must be provided")
        @JsonProperty("dag")
        String dag,

        @NotNull(message = "Stages must not be null")
        @JsonProperty("stages")
        List<StageData> stages

) {
    public record StageData(

            @JsonProperty("userStageID")
            int userStageID,

            @NotBlank(message = "Stage name is required")
            @JsonProperty("stageName")
            String stageName,

            @NotNull(message = "Action ID is required")
            @JsonProperty("actionId")
            Long actionId,

            @JsonProperty("nextSidSuccess")
            List<Long> nextSidSuccess,

            @JsonProperty("nextSidFaliure")
            List<Long> nextSidFaliure,

            @JsonProperty("payload")
            String payload, // JSON string

            @NotBlank(message = "Flag is required")
            @JsonProperty("CFlag")
            Boolean CFlag

    ) {}
}
