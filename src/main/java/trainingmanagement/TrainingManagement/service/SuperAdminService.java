package trainingmanagement.TrainingManagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import trainingmanagement.TrainingManagement.customException.EmployeeExistException;
import trainingmanagement.TrainingManagement.customException.EmployeeNotExistException;
import trainingmanagement.TrainingManagement.customException.InvalidEmployeeDetailsException;
import trainingmanagement.TrainingManagement.customException.SuperAdminIdException;
import trainingmanagement.TrainingManagement.dao.EmployeeDao;
import trainingmanagement.TrainingManagement.dao.RoleDao;
import trainingmanagement.TrainingManagement.entity.Employee;
import trainingmanagement.TrainingManagement.entity.EmployeeRole;
import trainingmanagement.TrainingManagement.entity.Roles;
import trainingmanagement.TrainingManagement.request.MultipleEmployeeRequest;
import trainingmanagement.TrainingManagement.response.EmployeeDetails;
import trainingmanagement.TrainingManagement.response.EmployeeProfile;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class SuperAdminService
{
    @Autowired
    private EmployeeDao employeeDao;
    @Autowired
    private AdminService adminService;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private String CHANGING_ROLES = "UPDATE employee_role SET role_name=? WHERE emp_id=?";
    int offset=0;

    public void registerEmployee(Employee employee) throws EmployeeExistException, EmployeeNotExistException, SuperAdminIdException, InvalidEmployeeDetailsException {
        isEmployeeDetailValid(employee);
        checkEmployeeExist(employee.getEmpId());
        isSuperAdminId(employee.getEmpId());
        Roles roles = roleDao.findById("employee").get();
        Set<Roles> employeeRoles = new HashSet<>();

        employeeRoles.add(roles);
        employee.setRoles(employeeRoles);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("spring.email.from@gmail.com");
        message.setTo(employee.getEmail());
        String emailText ="Employee id: " + employee.getEmpId()+"\nPassword: "+employee.getPassword();
        message.setText(emailText);
        message.setSubject("Login credentials for Training management website");
        mailSender.send(message);
        employee.setPassword(getEncodedPassword(employee.getPassword()));
        String query =  "insert into Manager(empId) values(?)";
        employeeDao.save(employee);
        jdbcTemplate.update(query,employee.getEmpId());
    }
    public void isEmployeeDetailValid(Employee employee) throws InvalidEmployeeDetailsException
    {
        if ((employee.getEmpId().trim()).isEmpty())
        {
            System.out.println((employee.getEmpId().trim()));
            throw new InvalidEmployeeDetailsException("Employee id cannot be null");
        }
        if ((employee.getEmpName().trim()).isEmpty())
        {
            throw new InvalidEmployeeDetailsException("Employee name cannot be null");
        }
        if ((employee.getPassword().trim()).isEmpty())
        {
            throw new InvalidEmployeeDetailsException("Employee password cannot be null");
        }
        if ((employee.getDesignation().trim()).isEmpty())
        {
            throw new InvalidEmployeeDetailsException("Employee designation cannot be null");
        }
        if ((employee.getEmail().trim()).isEmpty())
        {
            throw new InvalidEmployeeDetailsException("Employee email id cannot be null");
        }
    }

    public void registerNewEmployee(List<Employee> employee) throws EmployeeExistException, EmployeeNotExistException, SuperAdminIdException, InvalidEmployeeDetailsException {
        for (Employee emp:employee)
        {
            registerEmployee(emp);
        }
    }
    public void checkEmployeeExist(String empId) throws EmployeeExistException
    {
        String query="select count(emp_id) from employee where emp_id=? ";
        int i = jdbcTemplate.queryForObject(query, Integer.class,empId);
        if(i == 1)
        {
            throw new EmployeeExistException("Employee Already Exist, Create a new employee Id");
        }
    }

    public void checkEmployeeDeleted(String empId) throws EmployeeNotExistException
    {
        String query="select emp_id from employee where emp_id=? and delete_status=0";
        try
        {
            String str=jdbcTemplate.queryForObject(query,String.class,empId);
        }
        catch (DataAccessException e)
        {
            throw new EmployeeNotExistException("Employee "+empId+" already deleted");
        }
    }

    public String changeRole(EmployeeRole employeeRole) throws EmployeeNotExistException, SuperAdminIdException, EmployeeExistException
    {
        isSuperAdminId(employeeRole.getEmpId());
        employeeExist(employeeRole.getEmpId());
        checkEmployeeDeleted(employeeRole.getEmpId());
        jdbcTemplate.update(CHANGING_ROLES,employeeRole.getRoleName(),employeeRole.getEmpId());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("spring.email.from@gmail.com");
        String query = "select email from employee where emp_id=?";
        String email = jdbcTemplate.queryForObject(query, String.class,employeeRole.getEmpId());
        message.setTo(email);
        String emailText ="Your role is changed to "+employeeRole.getRoleName();
        message.setText(emailText);
        message.setSubject("Role changed");
        mailSender.send(message);
        return "Role changed to "+employeeRole.getRoleName();
    }

    public void employeeExist(String empId) throws EmployeeExistException
    {
        String query="select emp_id from employee where emp_id=? ";
        try
        {
            String str=jdbcTemplate.queryForObject(query,String.class,empId);
        }
        catch (DataAccessException e) {
            throw new EmployeeExistException("Employee "+empId+" doesn't Exist");
        }
    }

    public String getEncodedPassword(String password)
    {
        return passwordEncoder.encode(password);
    }
    public void deleteEmployees(List<MultipleEmployeeRequest> empId) throws EmployeeNotExistException, EmployeeExistException, SuperAdminIdException
    {
        for (MultipleEmployeeRequest emp:empId)
        {
            employeeExist(emp.getEmpId());
            checkEmployeeDeleted(emp.getEmpId());
            deleteEmployee(emp.getEmpId());
        }
    }

    public void deleteEmployee(String empId) throws SuperAdminIdException
    {
        isSuperAdminId(empId);
        String query="update employee set delete_status=1 where emp_id=?";
        jdbcTemplate.update(query,empId);
    }

    public Map<Integer,List<EmployeeProfile>> employeeDetailsListForSuperAdmin(int page, int limit)
    {
        Map map = new HashMap<Integer,List>();
        offset = limit *(page-1);
        String queryForEmployees = "SELECT emp_id, emp_name, designation,profile_pic FROM employee WHERE delete_status = 0 AND emp_id <> 'RT001' limit ?,?";
        List<EmployeeProfile> a = jdbcTemplate.query(queryForEmployees,new BeanPropertyRowMapper<EmployeeProfile>(EmployeeProfile.class),offset,limit);
        map.put(a.size(),a);
        return map;
    }

    public void isSuperAdminId(String empId) throws SuperAdminIdException
    {
        if(empId.equalsIgnoreCase("RT001"))
        {
            throw new SuperAdminIdException("can't give super admin as employee");
        }
    }
}
