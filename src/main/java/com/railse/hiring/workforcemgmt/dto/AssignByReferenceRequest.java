package com.railse.hiring.workforcemgmt.dto;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.railse.hiring.workforcemgmt.common.model.enums.ReferenceType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;


@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AssignByReferenceRequest {

    @NotNull(message = "performedBy is required")
    private @Getter Integer performedBy;
   private Long referenceId;
   private ReferenceType referenceType;
   private Long assigneeId;
}
