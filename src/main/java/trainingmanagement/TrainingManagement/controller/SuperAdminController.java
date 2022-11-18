package trainingmanagement.TrainingManagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import trainingmanagement.TrainingManagement.customException.EmployeeExistException;
import trainingmanagement.TrainingManagement.customException.EmployeeNotExistException;
import trainingmanagement.TrainingManagement.customException.SuperAdminIdException;
import trainingmanagement.TrainingManagement.entity.Employee;
import trainingmanagement.TrainingManagement.entity.EmployeeRole;
import trainingmanagement.TrainingManagement.request.MultipleEmployeeRequest;
import trainingmanagement.TrainingManagement.service.SuperAdminService;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/superAdmin")
public class SuperAdminController
{
    @Autowired
    SuperAdminService superAdminService;

    @PostMapping("/registerEmployees")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<String> registerEmployees(@RequestBody Employee employee)
    {
        try
        {
            superAdminService.registerNewEmployee(employee);
            return ResponseEntity.of(Optional.of( " Registration successful"));
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/changeRole")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<String> changingRole(@RequestBody EmployeeRole employeeRole) throws EmployeeNotExistException {
        String roleStatus = null;
        try{
            roleStatus = superAdminService.changeRole(employeeRole);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body(e.getMessage());
        }
        return ResponseEntity.of(Optional.of( roleStatus));
    }

    @PutMapping("/delete/Employees")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<String> deleteEmployees(@RequestBody List<MultipleEmployeeRequest> employees)
    {
        try
        {
            superAdminService.deleteEmployees(employees);
            return ResponseEntity.status(HttpStatus.OK).body("Deleted successfully");

        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body(e.getMessage());
        }
    }

    @GetMapping("/employees")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<?> employees()
    {
       return ResponseEntity.status(HttpStatus.OK).body(superAdminService.employeeDetailsListForSuperAdmin());
    }
}
