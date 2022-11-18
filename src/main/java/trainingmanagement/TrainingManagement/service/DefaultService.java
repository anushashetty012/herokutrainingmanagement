package trainingmanagement.TrainingManagement.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import trainingmanagement.TrainingManagement.dao.EmployeeDao;
import trainingmanagement.TrainingManagement.dao.RoleDao;
import trainingmanagement.TrainingManagement.entity.Employee;
import trainingmanagement.TrainingManagement.entity.Roles;

import java.util.HashSet;
import java.util.Set;

@Service
public class DefaultService
{
    @Autowired
    private EmployeeDao employeeDao;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void initRoleAndEmployee()
    {

        //Adding Default super admin role
        Roles superAdminRole = new Roles();
        superAdminRole.setRoleName("super_admin");
        roleDao.save(superAdminRole);

        //Adding Employee role
        Roles employeeRole = new Roles();
        employeeRole.setRoleName("employee");
        roleDao.save(employeeRole);

        //Add manager role
        Roles managerRole = new Roles();
        managerRole.setRoleName("manager");
        roleDao.save(managerRole);

        //Add admin role
        Roles adminRole = new Roles();
        adminRole.setRoleName("admin");
        roleDao.save(adminRole);

        //Default Super Admin
        Employee superAdmin = new Employee();
        superAdmin.setEmpId("RT001");
        superAdmin.setEmpName("Super Admin");
        superAdmin.setPassword(getEncodedPassword("super123"));
        superAdmin.setDesignation("Super Admin");
        superAdmin.setEmail("super@gmail.com");

        Set<Roles> superAdminRoles = new HashSet<>();
        superAdminRoles.add(superAdminRole);

        superAdmin.setRoles(superAdminRoles);
        employeeDao.save(superAdmin);
    }
    public String getEncodedPassword(String password)
    {
        return passwordEncoder.encode(password);
    }
}
