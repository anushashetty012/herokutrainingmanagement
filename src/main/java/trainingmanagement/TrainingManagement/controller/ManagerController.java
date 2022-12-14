package trainingmanagement.TrainingManagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import trainingmanagement.TrainingManagement.entity.Course;
import trainingmanagement.TrainingManagement.service.ManagerService;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT, RequestMethod.PATCH},
        origins = {"http://localhost:4200","https://thriving-croissant-5df179.netlify.app"})
@RequestMapping("/manager")
public class ManagerController
{
    @Autowired
    private ManagerService managerService;

    //Get List of courses assigned to manager based on the completion status
    @GetMapping("/assignedCourses/{completionStatus}")
    @PreAuthorize("hasRole('manager')")
    public ResponseEntity<?> getCourse(Authentication authentication, @PathVariable String completionStatus, @RequestParam int page, @RequestParam int limit)
    {
        String empId = authentication.getName();
        Map<Integer,List<Course>> courseList = managerService.getAssignedCoursesForManagerByStatus(empId,completionStatus,page,limit);
        if(courseList == null)
        {
            return ResponseEntity.status(HttpStatus.OK).body("No "+completionStatus+" course");
        }
        return ResponseEntity.of(Optional.of(courseList));
    }

    @GetMapping("/assignedCourses/count/{completionStatus}")
    @PreAuthorize("hasRole('manager')")
    public ResponseEntity<?> getCourseCountForManagerByStatus(Authentication authentication, @PathVariable String completionStatus)
    {
        String empId = authentication.getName();
        int count  = managerService.getCourseCountForManagerByStatus(empId,completionStatus);
        return ResponseEntity.of(Optional.of(count));
    }
}
