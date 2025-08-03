package com.railse.hiring.workforcemgmt.model;

import lombok.Data;

@Data
public class TaskComment {
    private String comment;
    private Integer createdBy;
    private Long timestamp;
}
