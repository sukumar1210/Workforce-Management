package com.railse.hiring.workforcemgmt.dto;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.railse.hiring.workforcemgmt.model.enums.TaskStatus;
import lombok.Data;


import java.util.List;


@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateTaskRequest {
   private List<UpdateRequestItem> requests;


   @Data
   @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
   public static class UpdateRequestItem {
       private Long taskId;
       private TaskStatus taskStatus;
       private String description;
   }
}
