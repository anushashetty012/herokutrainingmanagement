package trainingmanagement.TrainingManagement.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import trainingmanagement.TrainingManagement.customException.EmployeeNotExistException;
import trainingmanagement.TrainingManagement.entity.JwtRequest;
import trainingmanagement.TrainingManagement.entity.JwtResponse;
import trainingmanagement.TrainingManagement.service.JwtService;

import java.util.Optional;

@RestController
@CrossOrigin(
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT, RequestMethod.PATCH},
        origins = {"http://localhost:4200","https://thriving-croissant-5df179.netlify.app"})
public class JwtController {

    @Autowired
    private JwtService jwtService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping({"/authenticate"})
    public ResponseEntity<?> createJwtToken(@RequestBody JwtRequest jwtRequest) throws Exception
    {
        JwtResponse response;
        try
        {
            checkEmployeeExist(jwtRequest.getEmpId());
            response  = jwtService.createJwtToken(jwtRequest);
        }
        catch (EmployeeNotExistException e)
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not allowed to use this service");
        }
        if (response == null)
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
        return ResponseEntity.of(Optional.of(response));
    }

    public void checkEmployeeExist(String empId) throws EmployeeNotExistException
    {
        String query="select emp_id from employee where emp_id=? and delete_status=0 ";
        try
        {
            String str=jdbcTemplate.queryForObject(query,String.class,empId);

        }
        catch (DataAccessException e)
        {
            throw new EmployeeNotExistException("Employee "+empId+" Does Not Exist");
        }
    }
}
