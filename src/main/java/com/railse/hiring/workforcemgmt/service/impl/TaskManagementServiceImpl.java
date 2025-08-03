package com.railse.hiring.workforcemgmt.service.impl;


import com.railse.hiring.workforcemgmt.common.exception.ResourceNotFoundException;
import com.railse.hiring.workforcemgmt.dto.*;
import com.railse.hiring.workforcemgmt.mapper.ITaskManagementMapper;
import com.railse.hiring.workforcemgmt.model.TaskManagement;
import com.railse.hiring.workforcemgmt.model.enums.Task;
import com.railse.hiring.workforcemgmt.model.enums.TaskStatus;
import com.railse.hiring.workforcemgmt.repository.TaskRepository;
import com.railse.hiring.workforcemgmt.service.TaskManagementService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class TaskManagementServiceImpl implements TaskManagementService {


   private final TaskRepository taskRepository;
   private final ITaskManagementMapper taskMapper;


   public TaskManagementServiceImpl(TaskRepository taskRepository, ITaskManagementMapper taskMapper) {
       this.taskRepository = taskRepository;
       this.taskMapper = taskMapper;
   }


   @Override
   public TaskManagementDto findTaskById(Long id) {
       TaskManagement task = taskRepository.findById(id)
               .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
       return taskMapper.modelToDto(task);
   }


   @Override
   public List<TaskManagementDto> createTasks(TaskCreateRequest createRequest) {
    List<TaskManagement> createdTasks = new ArrayList<>();
    for (TaskCreateRequest.RequestItem item : createRequest.getRequests()) {
        // BUG #1: while creating new tasks the same check needs to be done to prevent the same bug while task creation.
        // 1. Fetch all tasks for this reference
        List<TaskManagement> existingTasks = taskRepository.findByReferenceIdAndReferenceType(
                item.getReferenceId(), item.getReferenceType());

        // 2. Cancel existing tasks of the same task type (not completed)
        for (TaskManagement existing : existingTasks) {
            if (existing.getTask().equals(item.getTask())  && existing.getStatus() != TaskStatus.COMPLETED) {
                existing.setStatus(TaskStatus.CANCELLED);
                taskRepository.save(existing);
            }
        }

        // 3. Create new task
        TaskManagement newTask = new TaskManagement();
        newTask.setReferenceId(item.getReferenceId());
        newTask.setReferenceType(item.getReferenceType());
        newTask.setTask(item.getTask());
        newTask.setAssigneeId(item.getAssigneeId());
        newTask.setPriority(item.getPriority());
        newTask.setTaskDeadlineTime(item.getTaskDeadlineTime());
        newTask.setStatus(TaskStatus.ASSIGNED);
        newTask.setDescription("New task created.");
        createdTasks.add(taskRepository.save(newTask));
    }

    return taskMapper.modelListToDtoList(createdTasks);
}



   @Override
   public List<TaskManagementDto> updateTasks(UpdateTaskRequest updateRequest) {
       List<TaskManagement> updatedTasks = new ArrayList<>();
       for (UpdateTaskRequest.RequestItem item : updateRequest.getRequests()) {
           TaskManagement task = taskRepository.findById(item.getTaskId())
                   .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + item.getTaskId()));


           if (item.getTaskStatus() != null) {
               task.setStatus(item.getTaskStatus());
           }
           if (item.getDescription() != null) {
               task.setDescription(item.getDescription());
           }
           updatedTasks.add(taskRepository.save(task));
       }
       return taskMapper.modelListToDtoList(updatedTasks);
   }


   @Override
   public String assignByReference(AssignByReferenceRequest request) {
       List<Task> applicableTasks = Task.getTasksByReferenceType(request.getReferenceType());
       List<TaskManagement> existingTasks = taskRepository.findByReferenceIdAndReferenceType(request.getReferenceId(), request.getReferenceType());


       for (Task taskType : applicableTasks) {
           List<TaskManagement> tasksOfType = existingTasks.stream()
                   .filter(t -> t.getTask() == taskType && t.getStatus() != TaskStatus.COMPLETED)
                   .collect(Collectors.toList());


           // BUG #1 is here.
           // NOTE: This is a design choice for this example.
           // I would usually want to do this in a transaction when in real life scenario with a database.
           // But for this example, we do not have a database hence i am not considering the disk write failures.
           if (!tasksOfType.isEmpty()) {

                // Cancel all previous tasks
                for (TaskManagement oldTask : tasksOfType) {
                    oldTask.setStatus(TaskStatus.CANCELLED);
                    taskRepository.save(oldTask);
                }

                // Create a new task for the new assignee
                TaskManagement newTask = new TaskManagement();
                newTask.setReferenceId(request.getReferenceId());
                newTask.setReferenceType(request.getReferenceType());
                newTask.setTask(taskType);
                newTask.setAssigneeId(request.getAssigneeId());
                newTask.setStatus(TaskStatus.ASSIGNED);
                taskRepository.save(newTask);
            } else {
               // Create a new task if none exist
               TaskManagement newTask = new TaskManagement();
               newTask.setReferenceId(request.getReferenceId());
               newTask.setReferenceType(request.getReferenceType());
               newTask.setTask(taskType);
               newTask.setAssigneeId(request.getAssigneeId());
               newTask.setStatus(TaskStatus.ASSIGNED);
               taskRepository.save(newTask);
           }
       }
       return "Tasks assigned successfully for reference " + request.getReferenceId();
   }


   @Override
   public List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request) {
       List<TaskManagement> tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());


       // BUG #2 is here. It should filter out CANCELLED tasks but doesn't.
       List<TaskManagement> filteredTasks = tasks.stream()
               .filter(task -> {
                   // This logic is incomplete for the assignment.
                   // It should check against startDate and endDate.
                   // For now, it just returns all tasks for the assignees.
                   return true;
               })
               .collect(Collectors.toList());


       return taskMapper.modelListToDtoList(filteredTasks);
   }
}
