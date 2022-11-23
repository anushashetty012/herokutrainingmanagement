package trainingmanagement.TrainingManagement.controller;


import org.apache.http.protocol.HTTP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import trainingmanagement.TrainingManagement.customException.EmployeeNotExistException;
import trainingmanagement.TrainingManagement.entity.JwtRequest;
import trainingmanagement.TrainingManagement.entity.JwtResponse;
import trainingmanagement.TrainingManagement.service.JwtService;

import java.util.Optional;

@RestController
@CrossOrigin
public class JwtController {

    @Autowired
    private JwtService jwtService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping({"/authenticate"})
    public ResponseEntity<?> createJwtToken(@RequestBody JwtRequest jwtRequest) throws Exception
    {
        try
        {
            checkEmployeeExist(jwtRequest.getEmpId());
        }
        catch (EmployeeNotExistException e)
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not allowed to use this service");
        }
        return ResponseEntity.of(Optional.of(jwtService.createJwtToken(jwtRequest)));
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
