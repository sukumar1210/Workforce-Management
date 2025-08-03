package com.railse.hiring.workforcemgmt.dto;

import com.railse.hiring.workforcemgmt.model.enums.Priority;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;

@Data
public class UpdatePriorityRequest {
    
    @NotNull(message = "performedBy is required")
    private @Getter Integer performedBy;
    private @Getter Priority priority;
}
