package trainingmanagement.TrainingManagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import trainingmanagement.TrainingManagement.customException.EmployeeNotExistException;
import trainingmanagement.TrainingManagement.entity.Employee;
import trainingmanagement.TrainingManagement.entity.EmployeeRole;
import trainingmanagement.TrainingManagement.request.MultipleEmployeeRequest;
import trainingmanagement.TrainingManagement.response.RegistrationResponse;
import trainingmanagement.TrainingManagement.service.SuperAdminService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT, RequestMethod.PATCH},
        origins = {"http://localhost:4200","https://thriving-croissant-5df179.netlify.app"})
@RequestMapping("/superAdmin")
public class SuperAdminController
{
    @Autowired
    SuperAdminService superAdminService;

    @PostMapping("/registerEmployees")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<?> registerEmployees(@RequestBody List<Employee> employee)
    {
        List<RegistrationResponse> invalidEmployeeList = superAdminService.registerNewEmployee(employee);
        if (invalidEmployeeList.size() == 0)
        {
            List<RegistrationResponse> validResponse = new ArrayList<>();
            RegistrationResponse response = new RegistrationResponse();
            response.setReason("Registration successful");
            validResponse.add(response);
            return ResponseEntity.of(Optional.of(validResponse));
        }
        return ResponseEntity.status(HttpStatus.OK).body(invalidEmployeeList);
    }

    @PutMapping("/changeRole")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<String> changingRole(@RequestBody EmployeeRole employeeRole) throws EmployeeNotExistException {
        String roleStatus = null;
        try
        {
            roleStatus = superAdminService.changeRole(employeeRole);
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
        return ResponseEntity.of(Optional.of( roleStatus));
    }

    @PutMapping("/delete/employees")
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Refresh the page and try again");
        }
    }
}
