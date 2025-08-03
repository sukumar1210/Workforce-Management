package com.railse.hiring.workforcemgmt.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddCommentRequest {
    private String comment;
    
    @NotNull(message = "createdBy is required")
    private Integer createdBy;
}
