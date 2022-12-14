package trainingmanagement.TrainingManagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
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
import trainingmanagement.TrainingManagement.response.RegistrationResponse;

import java.sql.Timestamp;
import java.util.*;

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

    public void registerEmployee(Employee employee) throws MailException {
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
        employee.setCreationTimestamp(new Timestamp(System.currentTimeMillis()));
        employeeDao.save(employee);
        jdbcTemplate.update(query,employee.getEmpId());
    }
    public void checkEmailExist( String email) throws InvalidEmployeeDetailsException
    {
        String query="select count(email) from employee where email=? and delete_status=false";
        int i = jdbcTemplate.queryForObject(query, Integer.class,email);
        if(i >= 1)
        {
            throw new InvalidEmployeeDetailsException("Email already exists, register with different email");
        }
    }
    public void checkValidityOfEmployees(Employee employee) throws InvalidEmployeeDetailsException, EmployeeExistException, SuperAdminIdException {
        isEmployeeDetailValid(employee);
        checkEmployeeExist(employee.getEmpId());
        checkEmailExist(employee.getEmail());
        isSuperAdminId(employee.getEmpId());
    }
    public void isEmployeeDetailValid(Employee employee) throws InvalidEmployeeDetailsException
    {
        if ((employee.getEmpId().trim()).isEmpty())
        {
            throw new InvalidEmployeeDetailsException("Employee id cannot be null");
        }
        if ((employee.getEmpId().trim()).length()!=6)
        {
            throw new InvalidEmployeeDetailsException("Employee id should be 6 characters");
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
        if (!((employee.getEmail().trim()).contains("@") && (employee.getEmail().trim()).contains(".")))
        {
            throw new InvalidEmployeeDetailsException("Email format is incorrect (include @ and .)");
        }
    }

    public List<RegistrationResponse> registerNewEmployee(List<Employee> employee)
    {
        List<RegistrationResponse> invalidEmployeeList = new ArrayList<>();
        for (Employee emp:employee)
        {
            try
            {
                checkValidityOfEmployees(emp);
                registerEmployee(emp);
            }
            catch (Exception e) {
                RegistrationResponse response = new RegistrationResponse();
                response.setEmpId(emp.getEmpId());
                response.setReason(emp.getEmpId()+" Not registered because "+e.getMessage());
                invalidEmployeeList.add(response);
            }
        }
        return invalidEmployeeList;
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
        if (employeeRole.getRoleName().equalsIgnoreCase("employee")||employeeRole.getRoleName().equalsIgnoreCase("admin"))
        {
            updateManagerTable(employeeRole.getEmpId());
            updateManagersCourseTable(employeeRole.getEmpId());
        }
        return "Role of "+employeeRole.getEmpId()+" changed to "+employeeRole.getRoleName();
    }

    public void updateManagerTable(String managerId)
    {
        String query = "update Manager set managerId = null where managerId=?";
        jdbcTemplate.update(query,managerId);
    }
    public void updateManagersCourseTable(String managerId)
    {
        String query = "delete from ManagersCourses where managerId=?";
        jdbcTemplate.update(query,managerId);
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

    public void deleteListValid(List<MultipleEmployeeRequest> empId) throws EmployeeNotExistException, EmployeeExistException {
        for (MultipleEmployeeRequest emp:empId)
        {
            isEmployeeValidToDelete(emp);
        }
    }
    public void isEmployeeValidToDelete(MultipleEmployeeRequest emp) throws EmployeeExistException, EmployeeNotExistException
    {
        employeeExist(emp.getEmpId());
        checkEmployeeDeleted(emp.getEmpId());
    }
    public void deleteEmployees(List<MultipleEmployeeRequest> empId) throws EmployeeNotExistException, EmployeeExistException, SuperAdminIdException
    {
        deleteListValid(empId);
        for (MultipleEmployeeRequest emp:empId)
        {
            deleteEmployee(emp.getEmpId());
            deleteFromEmployeeRoleOnEmpDelete(emp.getEmpId());
            deleteFromInvitesOnEmpDelete(emp.getEmpId());
            deleteFromAcceptedInvitesOnEmpDelete(emp.getEmpId());
            setNullInManagerTable(emp.getEmpId());
        }
    }
    public void deleteFromEmployeeRoleOnEmpDelete(String empId)
    {
        String query = "delete from employee_role where emp_id=?";
        jdbcTemplate.update(query,empId);
    }

    public void deleteFromInvitesOnEmpDelete(String empId)
    {
        String query = "delete from Invites where empId=? and courseId in(select courseId from Course where completionStatus='upcoming')";
        jdbcTemplate.update(query,empId);
    }

    public void deleteFromAcceptedInvitesOnEmpDelete(String empId)
    {
        String query = "delete from AcceptedInvites where empId=? and courseId in(select courseId from Course where completionStatus='upcoming')";
        jdbcTemplate.update(query,empId);
    }

    public void setNullInManagerTable(String empId)
    {
        String query =  "update Manager set managerId=null where managerId=?";
        jdbcTemplate.update(query,empId);
    }

    public void deleteEmployee(String empId) throws SuperAdminIdException
    {
        isSuperAdminId(empId);
        String query="update employee set delete_status=1 where emp_id=?";
        jdbcTemplate.update(query,empId);
    }

    public void isSuperAdminId(String empId) throws SuperAdminIdException
    {
        if(empId.equalsIgnoreCase("RT001"))
        {
            throw new SuperAdminIdException("can't give super admin as employee");
        }
    }
}
