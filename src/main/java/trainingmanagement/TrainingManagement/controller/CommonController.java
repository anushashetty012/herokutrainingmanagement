package trainingmanagement.TrainingManagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import trainingmanagement.TrainingManagement.customException.CourseNotValidException;
import trainingmanagement.TrainingManagement.entity.Course;
import trainingmanagement.TrainingManagement.request.FilterByDate;
import trainingmanagement.TrainingManagement.response.*;
import trainingmanagement.TrainingManagement.request.MultipleEmployeeRequest;
import trainingmanagement.TrainingManagement.service.CommonService;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT, RequestMethod.PATCH},
        origins = {"http://localhost:4200","https://thriving-croissant-5df179.netlify.app"})
public class CommonController
{
    @Autowired
    CommonService commonService;

    @GetMapping("/company/trainings/count/{completionStatus}")
    @PreAuthorize("hasRole('admin') or hasRole('super_admin')")
    public ResponseEntity<?> trainingCountByStatus(@PathVariable String completionStatus)
    {
        int count = commonService.getCourseCountByStatus(completionStatus);
        if (count == 0)
        {
            return new ResponseEntity<>("No "+completionStatus+" course in the company",HttpStatus.OK);
        }
        return ResponseEntity.of(Optional.of(count));
    }

    //get the list of course based on completion status
    @GetMapping("/company/courses/{completionStatus}")
    @PreAuthorize("hasRole('admin') or hasRole('super_admin')")
    public ResponseEntity<?> getCourse(@PathVariable String completionStatus, @RequestParam int page, @RequestParam int limit)
    {
        Map<Integer,List<CourseList>> courses = commonService.getCourse(completionStatus,page,limit);
        if (courses == null)
        {
            return new ResponseEntity<>("No "+completionStatus+" course in the company",HttpStatus.OK);
        }
        return ResponseEntity.of(Optional.of(courses));
    }

    @GetMapping("/attendees_nonAttendees_count/{courseId}")
    @PreAuthorize("hasRole('admin') or hasRole('manager')")
    public ResponseEntity<?> getAttendeesAndNonAttendeesCount(@PathVariable int courseId,Authentication authentication)
    {
        AttendeesNonAttendeesCount employeeCount = commonService.getAttendeesAndNonAttendeesCount(courseId,authentication.getName());
        if (employeeCount == null)
        {
            return new ResponseEntity<>("This course is not allocated to you or there are no attendees for this course or the course may be deleted",HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.of(Optional.of(employeeCount));
    }

    @GetMapping("/attendees/{courseId}")
    @PreAuthorize("hasRole('admin') or hasRole('manager')")
    public ResponseEntity<?> getAttendees(@PathVariable int courseId,Authentication authentication,@RequestParam int page, int limit)
    {
        Map<Integer,List<EmployeeProfile>> employee = commonService.getAttendingEmployee(courseId,authentication.getName(),page,limit);
        if (employee==null)
        {
            return new ResponseEntity<>("This course is not allocated to you or there are no attendees for this course",HttpStatus.OK);
        }
        return ResponseEntity.of(Optional.of(employee));
    }

    @GetMapping("/nonAttendees/{courseId}")
    @PreAuthorize("hasRole('admin') or hasRole('manager')")
    public ResponseEntity<?> getNonAttendees(@PathVariable int courseId,Authentication authentication,@RequestParam int page, int limit)
    {
        Map<Integer,List<EmployeeProfile>> employee = commonService.getNonAttendingEmployee(courseId,authentication.getName(),page,limit);
        if (employee == null)
        {
            return new ResponseEntity<>("This course is not allocated to you or there are no non-attendees for this course",HttpStatus.OK);
        }
        return ResponseEntity.of(Optional.of(employee));
    }

    //inviting employees - invited=false if they can be invited
    @GetMapping("/employeesToInvite/{courseId}")
    @PreAuthorize("hasRole('admin') or hasRole('manager')")
    public ResponseEntity<?> getEmployeesToInvite(@PathVariable int courseId,Authentication authentication)
    {
        List<EmployeeInvite> employeeList=null;
        try
        {
            employeeList = commonService.getEmployeesToInvite(courseId,authentication.getName());
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        if (employeeList == null)
        {
            return new ResponseEntity<>("There are no employees who are not invited or You cannot invite employees for this course",HttpStatus.OK);
        }
        return ResponseEntity.of(Optional.of(employeeList));
    }

    @GetMapping("/employees/search/{courseId}")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('super_admin')")
    public ResponseEntity<?> getEmployeesToInviteBySearchKey(Authentication authentication,@PathVariable int courseId, @RequestParam String searchKey)
    {
        String empId = authentication.getName();
        List<EmployeeInvite> employeeList;
        employeeList = commonService.searchEmployeesToInvite(courseId,empId,searchKey);
        if (employeeList == null)
        {
            return ResponseEntity.status(HttpStatus.OK).body("No match found");
        }
        return ResponseEntity.of(Optional.of(employeeList));
    }

    @PostMapping("/invite/{courseId}")
    @PreAuthorize("hasRole('admin') or hasRole('manager')")
    public ResponseEntity<String> inviteEmployee(@PathVariable int courseId, @RequestBody List<MultipleEmployeeRequest> inviteToEmployees,Authentication authentication) throws CourseNotValidException {
        String inviteStatus;
        try
        {
            inviteStatus = commonService.inviteEmployees(courseId,inviteToEmployees,authentication.getName());
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        if (inviteStatus == null)
        {
            return new ResponseEntity<>("You cannot invite employees for this course",HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.of(Optional.of(inviteStatus));
    }

    @PutMapping("/deleteInvite/{courseId}")
    @PreAuthorize("hasRole('admin') or hasRole('manager')")
    public ResponseEntity<String> deleteInvite(@PathVariable int courseId,@RequestBody List<MultipleEmployeeRequest> deleteInvites,Authentication authentication)
    {
        String deleteStatus;
        try
        {
            deleteStatus = commonService.deleteInvite(courseId,deleteInvites,authentication.getName());
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        if (deleteStatus == null)
        {
            return new ResponseEntity<>("You cannot delete invite of this employees for this course",HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.of(Optional.of(deleteStatus));
    }

    //Get List of All Employees
    @GetMapping("/employees")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('super_admin')")
    public ResponseEntity<?> getEmployeeList(Authentication authentication,@RequestParam int page, @RequestParam int limit)
    {
        String empId = authentication.getName();
        Map<Integer,List<EmployeeDetails>> empData = commonService.employeeDetails(empId,page,limit);
        if (empData.size() == 0)
        {
            return ResponseEntity.status(HttpStatus.OK).body("No employees found");
        }
        return ResponseEntity.of(Optional.of(empData));
    }

    @GetMapping("/employees/search")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('super_admin')")
    public ResponseEntity<?> getEmployeeListBySearchKey(Authentication authentication, @RequestParam String searchKey,@RequestParam int page, @RequestParam int limit)
    {
        String empId = authentication.getName();
        Map<Integer,List<EmployeeDetails>> empData = commonService.employeeDetailsBySearchKey(empId,searchKey,page,limit);
        if (empData.size() == 0)
        {
            return ResponseEntity.status(HttpStatus.OK).body("No employees found");
        }
        return ResponseEntity.of(Optional.of(empData));
    }

    @GetMapping("/course/filter")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('super_admin')")
    public ResponseEntity<?> filterCourse(Authentication authentication, @ModelAttribute FilterByDate filter,@RequestParam int page, @RequestParam int limit){
        String empID = authentication.getName();

        Map<Integer,List<Course>> filteredCourseList = commonService.filteredCourses(empID,filter,page,limit);
        if (filteredCourseList.size() == 0)
        {
            return ResponseEntity.status(HttpStatus.OK).body("No course found");
        }
        return ResponseEntity.of(Optional.of(filteredCourseList));
    }

    @GetMapping("/attendedCourse")
    @PreAuthorize("hasRole('admin') or hasRole('manager')")
    public ResponseEntity<?> attendedCourses(Authentication authentication,@RequestParam String empId, @RequestParam int page, @RequestParam int limit)
    {
        Map<Integer,List<AttendedCourse>> attendedNonAttendedCourses;
        try
        {
            attendedNonAttendedCourses = commonService.attendedCourse(authentication.getName(), empId, page, limit);
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        if (attendedNonAttendedCourses == null)
        {
            return new ResponseEntity<>("Employee has not attended any course", HttpStatus.OK);
        }
        return ResponseEntity.of(Optional.of(attendedNonAttendedCourses));
    }

    @GetMapping("/nonAttendedCourse")
    @PreAuthorize("hasRole('admin') or hasRole('manager')")
    public ResponseEntity<?> NonAttendedCourses(Authentication authentication,@RequestParam String empId, @RequestParam int page, @RequestParam int limit)
    {
        Map<Integer,List<NonAttendedCourse>> nonAttendedNonAttendedCourses;
        try {
            nonAttendedNonAttendedCourses = commonService.nonAttendedCourse(authentication.getName(), empId, page, limit);
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        if (nonAttendedNonAttendedCourses == null)
        {
            return new ResponseEntity<>("Employee do not have any courses", HttpStatus.OK);
        }
        return ResponseEntity.of(Optional.of(nonAttendedNonAttendedCourses));
    }
}

