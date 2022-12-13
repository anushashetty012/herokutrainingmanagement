package trainingmanagement.TrainingManagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import trainingmanagement.TrainingManagement.customException.CourseInfoIntegrityException;
import trainingmanagement.TrainingManagement.entity.Course;
import trainingmanagement.TrainingManagement.request.ManagerEmployees;
import trainingmanagement.TrainingManagement.request.MultipleEmployeeRequest;
import trainingmanagement.TrainingManagement.response.*;
import trainingmanagement.TrainingManagement.service.AdminService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin")
public class AdminController
{
    @Autowired
    AdminService adminRepository;

    //create course
    @PostMapping("/createCourse")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> createCourse(@RequestBody Course course)
    {
        String course1=null;
        try
        {
            course1 = adminRepository.createCourse(course);
        }
        catch (CourseInfoIntegrityException e)
        {
            return new ResponseEntity<>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.status(HttpStatus.OK).body(course1);
    }

    //to allocate course to manager
    //displays list of upcoming courses
    @GetMapping("/courseToAssignManager")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> getCourseToAssignManager(@RequestParam int page, @RequestParam int limit)
    {
        Map<Integer,List<CourseList>> courseList = adminRepository.getCourseToAssignManager(page,limit);
        if (courseList == null)
        {
            return new ResponseEntity<>("No more upcoming course in the company, create a course to assign to manager",HttpStatus.OK);
        }
        return ResponseEntity.of(Optional.of(courseList));
    }

    //gets the list of managers to allocate reportees
    @GetMapping("/managers")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> getManagers(@RequestParam int page, @RequestParam int limit)
    {
        Map<Integer,List<EmployeeInfo>> managers = adminRepository.getManagers(page,limit);
        if (managers == null)
        {
            return new ResponseEntity<>("No more managers in the company",HttpStatus.OK);
        }
        return ResponseEntity.of(Optional.of(managers));
    }

    //search managers by search key- to allocate reportees
    @GetMapping("/managersBySearchKey")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> getManagersBySearchKey(@RequestParam String searchKey)
    {
        List<EmployeeInfo> managers = adminRepository.getManagersBySearchkey(searchKey);
        if (managers == null)
        {
            return new ResponseEntity<>("No match found",HttpStatus.OK);
        }
        return ResponseEntity.of(Optional.of(managers));
    }

    @GetMapping("/managersToAssignCourse/{courseId}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> getManagersToAssignCourse(@PathVariable int courseId, @RequestParam int page, @RequestParam int limit)
    {
        Map<Integer,List<ManagerInfo>> managers;
        try
        {
            managers = adminRepository.getManagersToAssignCourse(courseId,page,limit);
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Refresh the page and try again");
        }
        if (managers == null)
        {
            return ResponseEntity.status(HttpStatus.OK).body("No managers found");
        }
        return ResponseEntity.of(Optional.of(managers));
    }

    @GetMapping("/managersToAssignCourse/searchKey/{courseId}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> getManagersToAssignCourseBySearchKey(@PathVariable int courseId,@RequestParam String searchKey)
    {
        List<ManagerInfo> managers = adminRepository.getManagersToAssignCourseBySearchkey(courseId,searchKey);
        if (managers == null)
        {
            return new ResponseEntity<>("No match found",HttpStatus.OK);
        }
        return ResponseEntity.of(Optional.of(managers));
    }

    //assigns course to managers
    @PostMapping("/assignCourseToManager/{courseId}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> assignCourseToManager(@PathVariable int courseId, @RequestBody List<MultipleEmployeeRequest> courseToManager)
    {
        String assignStatus;
        try
        {
            assignStatus = adminRepository.assignCourseToManager(courseId,courseToManager);
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Refresh the page and try again");
        }
        return ResponseEntity.of(Optional.of(assignStatus));
    }

    @PutMapping("/unassignCourseFromManager/{courseId}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> unassignCourseFromManager(@PathVariable int courseId, @RequestBody List<MultipleEmployeeRequest> courseToManager)
    {
        String unassignStatus;
        try
        {
            unassignStatus = adminRepository.unassignCourseFromManager(courseId,courseToManager);
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.of(Optional.of(unassignStatus));
    }

    @PutMapping("/update/course")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> updateCourse(@RequestBody Course course){
        int updatedCourse = 0;
        try
        {
            updatedCourse = adminRepository.updateCourse(course);
        }
        catch (CourseInfoIntegrityException e)
        {
            return new ResponseEntity<>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if(updatedCourse == 0)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update the course");
        }
        return ResponseEntity.status(HttpStatus.OK).body("Updated successfully");
    }

    @GetMapping("/employeesToAssignManager/{managerId}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> employeesToAssignManager(@PathVariable String managerId,@RequestParam int page, @RequestParam int limit)
    {
        Map<Integer,List<EmployeesToManager>> employees;
        try
        {
            employees = adminRepository.employeesToAssignManager(managerId,page,limit);
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Refresh the page and try again");
        }
        if (employees == null)
        {
            return new ResponseEntity<>("No employees found",HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.OK).body(employees);
    }

    @GetMapping("/employeesToAssignManager/search/{managerId}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> employeesToAssignManagerBySearchKey(@PathVariable String managerId,@RequestParam String searchKey)
    {
        List<EmployeesToManager> employees;
        try
        {
            employees = adminRepository.employeesToAssignManagerBySearchKey(managerId,searchKey);
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Refresh the page and try again");
        }
        if (employees == null)
        {
            return new ResponseEntity<>("No employees found",HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.OK).body(employees);
    }

    @PutMapping("/assignManager/employees")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> assignManager(@RequestBody ManagerEmployees managerEmployees)
    {
        try
        {
            adminRepository.assignEmployeesToManager(managerEmployees);
            return ResponseEntity.status(HttpStatus.OK).body("Manager assigned successfully");
        } catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Refresh the page and try again");
        }
    }
    @DeleteMapping("/delete/course/{courseId}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> deleteCourse(@PathVariable Integer courseId)
    {
        try
        {
            adminRepository.deleteCourse(courseId);
            return ResponseEntity.status(HttpStatus.OK).body("Deleted course successfully");
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/delete/reportees")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> deleteReportees(@RequestBody ManagerEmployees managerEmployees)
    {
        try
        {
            adminRepository.removeReportee(managerEmployees);
            return ResponseEntity.status(HttpStatus.OK).body("Deleted reportees successfully");
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
