package trainingmanagement.TrainingManagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import trainingmanagement.TrainingManagement.entity.Course;
import trainingmanagement.TrainingManagement.request.FilterByDate;
import trainingmanagement.TrainingManagement.response.*;
import trainingmanagement.TrainingManagement.service.EmployeeService;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/employee")
public class EmployeeController
{
    @Autowired
    EmployeeService employeeService;

    @GetMapping("count/acceptedInvites/{courseId}")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('employee')")
    public ResponseEntity<?> getAcceptedCount(@PathVariable int courseId,Authentication authentication)
    {
        int count = employeeService.getAcceptedCount(courseId,authentication.getName());
        if (count == 0)
        {
            return new ResponseEntity<>("There are no attendees to this course or this course is not allocated to you",HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.of(Optional.of(count));
    }

    @GetMapping("/courseDetails/{courseId}")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('employee')")
    public ResponseEntity<?> viewCourseDetails(@PathVariable int courseId,Authentication authentication)
    {
        CourseInfo courseData = employeeService.viewCourseDetails(courseId,authentication.getName());
        if (courseData == null)
        {
            return new ResponseEntity<>("No such course is allocated to you",HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.of(Optional.of(courseData));
    }

    @PutMapping("/acceptInvite/{inviteId}")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('employee')")
    public ResponseEntity<?> acceptInvite(@PathVariable int inviteId)
    {
        String acceptanceStatus = employeeService.acceptInvite(inviteId);
        if (acceptanceStatus == null)
        {
            return new ResponseEntity<>("Invite not found", HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.of(Optional.of(acceptanceStatus));
    }

    @PutMapping("/rejectInvite/{inviteId}")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('employee')")
    public ResponseEntity<?> rejectInvite(@PathVariable int inviteId, @RequestBody RejectedResponse reason)
    {
        String rejectStatus = employeeService.rejectInvite(inviteId,reason);
        if (rejectStatus == null)
        {
            return new ResponseEntity<>("Invite not found", HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.of(Optional.of(rejectStatus));
    }

    @GetMapping("/profileInfo")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('employee')")
    public ResponseEntity<?> employeeProfile(Authentication authentication)
    {
        EmployeeProfile profileInfo = employeeService.profileInfo(authentication.getName());
        if (profileInfo == null)
        {
            return new ResponseEntity<>("Employee not found", HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.of(Optional.of(profileInfo));
    }

    @GetMapping("/attendedCourse")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('employee')")
    public ResponseEntity<?> attendedCourses(Authentication authentication, @RequestParam int page, @RequestParam int limit)
    {
        Map<Integer,List<AttendedCourse>> attendedNonAttendedCourses = employeeService.attendedCourse(authentication.getName(),page, limit);
        if (attendedNonAttendedCourses == null)
        {
            return new ResponseEntity<>("You did not attend any course or there are no more course", HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.of(Optional.of(attendedNonAttendedCourses));
    }

    @GetMapping("/nonAttendedCourse")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('employee')")
    public ResponseEntity<?> NonAttendedCourses(Authentication authentication, @RequestParam int page, @RequestParam int limit)
    {
        Map<Integer,List<NonAttendedCourse>> nonAttendedNonAttendedCourses = employeeService.nonAttendedCourse(authentication.getName(), page, limit);
        if (nonAttendedNonAttendedCourses == null)
        {
            return new ResponseEntity<>("you do not have any courses", HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.of(Optional.of(nonAttendedNonAttendedCourses));
    }

    //filtering based on employee profile
    @GetMapping("/acceptedCourses/filter")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('employee')")
    public ResponseEntity<?> filterCourses(Authentication authentication, @ModelAttribute FilterByDate filter, @RequestParam int page, @RequestParam int limit){
        String empId = authentication.getName();
        Map<Integer,List<Course>> courseList = employeeService.filterCourse(filter,empId,page,limit);
        if(courseList == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No "+filter.getCompletionStatus()+" course between the range");
        }
        return ResponseEntity.of(Optional.of(courseList));
    }

    //Get count of Course completion status
    @GetMapping("/course/count/{completionStatus}")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('employee')")
    public ResponseEntity<Integer> getCourseStatusCount(Authentication authentication, @PathVariable String completionStatus){
        String empId = authentication.getName();
        int count = employeeService.getCourseStatusCountForEmployee(empId,completionStatus);
        return ResponseEntity.of(Optional.of(count));
    }

    @GetMapping("/acceptedCourses/filter/{completionStatus}")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('employee')")
    public ResponseEntity<?> filterCoursesByStatus(Authentication authentication, @PathVariable String completionStatus,@RequestParam int page,@RequestParam int limit){
        String empId = authentication.getName();
        Map<Integer,List<Course>> courseList = employeeService.coursesForEmployeeByCompletedStatus(empId,completionStatus,page,limit);
        if(courseList == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No "+ completionStatus+" course");
        }
        return ResponseEntity.of(Optional.of(courseList));
    }

    @GetMapping("/notification/count")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('employee')")
    public ResponseEntity<Integer> NotificationCount(Authentication authentication)
    {
        String empId = authentication.getName();
        return ResponseEntity.status(HttpStatus.OK).body(employeeService.notificationCount(empId));
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('employee')")
    public ResponseEntity<?> notifications(Authentication authentication)
    {
        String empId = authentication.getName();
        return ResponseEntity.status(HttpStatus.OK).body(employeeService.notifications(empId));
    }

    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('employee')")
    @PutMapping("/uploadProfilePhoto")
    public ResponseEntity<?> uploadIProfile(@RequestParam("file") MultipartFile file, Authentication authentication)
    {
        String uploadMessage = employeeService.uploadProfilePhoto(file,authentication.getName());
        return ResponseEntity.status(HttpStatus.OK).body(uploadMessage);
    }

    @PreAuthorize("hasRole('admin') or hasRole('manager') or hasRole('employee')")
    @GetMapping("/viewProfilePhoto")
    public ResponseEntity<?> retrieveProfileLink(Authentication authentication) throws Exception
    {
        String url = employeeService.viewProfilePhoto(authentication.getName());
        return ResponseEntity.status(HttpStatus.OK).body(url);
    }
}
