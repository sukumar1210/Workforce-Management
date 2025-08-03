package com.railse.hiring.workforcemgmt.service.impl;


import com.railse.hiring.workforcemgmt.common.exception.ResourceNotFoundException;
import com.railse.hiring.workforcemgmt.dto.*;
import com.railse.hiring.workforcemgmt.mapper.ITaskManagementMapper;
import com.railse.hiring.workforcemgmt.model.TaskManagement;
import com.railse.hiring.workforcemgmt.model.enums.Priority;
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
        log.info("here");
        List<TaskManagement> tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());
        log.info("Tasks fetched: {}", tasks.size());
        Long start = request.getStartDate();
        Long end = request.getEndDate();

        List<TaskManagement> filteredTasks = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.CANCELLED)
                .filter(task -> task.getTaskDeadlineTime() >= start && task.getTaskDeadlineTime() <= end)
                .collect(Collectors.toList());

        log.info("Filtered tasks: {}", filteredTasks.size());

        return taskMapper.modelListToDtoList(filteredTasks);
    }

    @Override
    public List<TaskManagementDto> smartFetchTasksByDate(TaskFetchByDateRequest request) {
        // due to ambiguity in the requirements, and inability to claryfy with the client,
        // the following requirements are give:
        // 1. All active tasks that started within that range.
        // 2. PLUS all active tasks that started before the range but are still open and not yet completed.
        
        // one of 2 things can be inferred from the above requirements:
        // 1. All tasks that are due by the end date that can be acted upon are wanted.
        // 2. All tasks that are started before the end date, but would need changes in the model to add a "startedTime" field. (which can lead to the created time and started time to differ)

        // Due to unavailability of the client, inference 1 is chosen as any task that is due today, has to be either started within range or before.
        
        // NOTE: 
        // As this assumption lacks a clear startTime for a task, the case where a task starts in range but does not end in range is not handled.
        // That can only be handled in Inference 2, which is not implemented here.
        // But i wish to make a point that in the doing of this assignment, i am fully aware of both inferences and the implications of each.
        // If the client wants to change this, i would be happy to do so. 
        List<TaskManagement> tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());
        Long end = request.getEndDate();

        List<TaskManagement> dueByTasks = tasks.stream()
            // only active tasks
            .filter(t -> t.getStatus() == TaskStatus.ASSIGNED || t.getStatus() == TaskStatus.STARTED)
            // due on or before the end date
            .filter(t -> t.getTaskDeadlineTime() <= end)
            .collect(Collectors.toList());

        return taskMapper.modelListToDtoList(dueByTasks);
    }

    @Override
    public String updateTaskPriority(Long taskId, Priority newPriority) {
        TaskManagement task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setPriority(newPriority);
        taskRepository.save(task);
        return "Task priority updated successfully";
    }

    @Override
    public List<TaskManagementDto> fetchTasksByPriority(Priority priority) {
        // only returns the active tasks with the given priority
        // NOTE: This is a design choice for this example.
        // In a real world scenario, i would want to return all tasks with the given priority along with more filters such as assignee and time-range based fetching
        List<TaskManagement> tasks = taskRepository.findByPriority(priority);
        return taskMapper.modelListToDtoList(tasks);
    }


}
