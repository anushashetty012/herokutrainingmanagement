package trainingmanagement.TrainingManagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import trainingmanagement.TrainingManagement.customException.CourseNotValidException;
import trainingmanagement.TrainingManagement.entity.Course;
import trainingmanagement.TrainingManagement.entity.Employee;
import trainingmanagement.TrainingManagement.entity.Invites;
import trainingmanagement.TrainingManagement.entity.ManagersCourses;
import trainingmanagement.TrainingManagement.request.FilterByDate;
import trainingmanagement.TrainingManagement.response.CourseInfo;
import trainingmanagement.TrainingManagement.response.EmployeeDetails;
import trainingmanagement.TrainingManagement.response.EmployeeInvite;
import trainingmanagement.TrainingManagement.request.MultipleEmployeeRequest;
import trainingmanagement.TrainingManagement.response.EmployeeProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommonService
{
    //for manager
    private String CHECK_COURSE_ALLOCATION = "SELECT courseId FROM ManagersCourses WHERE managerId=? AND courseId=?";
    private String GET_ATTENDEES_COUNT = "SELECT COUNT(empId) FROM Invites WHERE courseId=? AND Invites.inviteId NOT IN (SELECT RejectedInvites.inviteId FROM RejectedInvites)";
    private String GET_NON_ATTENDEES_COUNT = "SELECT COUNT(empId) FROM Invites WHERE courseId=? AND Invites.inviteId IN (SELECT RejectedInvites.inviteId FROM RejectedInvites)";
    private String GET_COMPLETION_STATUS = "SELECT completionStatus FROM Course WHERE courseId=? and deleteStatus=false";
    private String GET_ATTENDEES = "SELECT emp_Id,emp_Name,designation,profile_pic FROM Employee,Invites WHERE Employee.emp_id<>'RT001' and Employee.emp_id=Invites.empId AND Invites.courseId=? and Employee.delete_status=false AND Invites.inviteId NOT IN (SELECT RejectedInvites.inviteId FROM RejectedInvites) LIMIT ?,?";
    private String GET_NON_ATTENDEES = "SELECT emp_Id,emp_Name,designation,profile_pic FROM Employee,RejectedInvites WHERE Employee.emp_id<>'RT001' and Employee.emp_id=RejectedInvites.empId AND RejectedInvites.courseId=? LIMIT ?,?";
    //to get role
    private String GET_ROLE = "SELECT role_name FROM employee_role WHERE emp_id=?";

    //for inviting
    //employees for admin
    private String GET_INVITED_EMPLOYEES = "SELECT emp_id,emp_name,designation FROM Employee,Invites WHERE Employee.emp_id<>'RT001' and Employee.emp_id=Invites.empId and courseId=? and (acceptanceStatus=true or acceptanceStatus is null) and delete_status=false";
    private String GET_NON_INVITED_EMPLOYEES = "SELECT emp_id,emp_name,designation FROM Employee WHERE Employee.emp_id<>'RT001' and Employee.emp_id NOT IN (SELECT Invites.empId FROM Invites WHERE courseId=? and (acceptanceStatus=true or acceptanceStatus is null)) and delete_status=false";
    //employees for manager
    private String GET_INVITED_EMPLOYEES_FOR_MANAGER = "SELECT emp_id,emp_name,designation FROM Employee,Manager,Invites WHERE Employee.emp_id=Manager.empId and Manager.managerId=? and Employee.emp_id<>'RT001' and Employee.emp_id=Invites.empId and courseId=? and (acceptanceStatus=true or acceptanceStatus is null) and delete_status=false";
    private String GET_NON_INVITED_EMPLOYEES_FOR_MANAGER = "SELECT emp_id,emp_name,designation FROM Employee,Manager WHERE Employee.emp_id=Manager.empId and Manager.managerId=? and Employee.emp_id<>'RT001' and Employee.emp_id NOT IN (SELECT Invites.empId FROM Invites WHERE courseId=? and (acceptanceStatus=true or acceptanceStatus is null)) and delete_status=false";
    private String INVITE_EMPLOYEES = "INSERT INTO Invites(empId,courseId) VALUES(?,?)";

    //to delete invie
    private String DELETE_INVITE_FROM_INVITES = "DELETE FROM Invites WHERE courseId=? AND empId=? and (acceptanceStatus=1 or acceptanceStatus is null)";
    private String DELETE_FROM_ACCEPTED_INVITES = "Update AcceptedInvites SET deleteStatus=true WHERE courseId=? and empId=?";
    int offset=0;
    @Autowired
    JdbcTemplate jdbcTemplate;

    //to get role
    public String getRole(String empId)
    {
        return jdbcTemplate.queryForObject(GET_ROLE,new Object[]{empId},String.class);
    }

    public String getAttendeesAndNonAttendeesForAdmin(int courseId)
    {
        try
        {
            int attendeesCount = jdbcTemplate.queryForObject(GET_ATTENDEES_COUNT,new Object[]{courseId},Integer.class);
            int nonAttendeesCount = jdbcTemplate.queryForObject(GET_NON_ATTENDEES_COUNT,new Object[]{courseId},Integer.class);
            String completionStatus = jdbcTemplate.queryForObject(GET_COMPLETION_STATUS,new Object[]{courseId},String.class);
            if (completionStatus.equalsIgnoreCase("active") || completionStatus.equalsIgnoreCase("upcoming"))
            {
                return "Attendees: "+attendeesCount;
            }
            int attendeesforCompletedCourse = jdbcTemplate.queryForObject("select count(empId) from acceptedInvites where courseId=? and deleteStatus=false",new Object[]{courseId}, Integer.class);
            return "Attendees: "+attendeesforCompletedCourse+"\nNon Attendees: "+nonAttendeesCount;
        }
        catch (DataAccessException e)
        {
            return "No such courseId exist";
        }
    }

    public String getAttendeesAndNonAttendeesForManager(int courseId, String empId)
    {
        try
        {
            List<ManagersCourses> isCourseAssigned = jdbcTemplate.query(CHECK_COURSE_ALLOCATION, (rs, rowNum) -> {
                return new ManagersCourses(rs.getInt("courseId"));
            }, empId, courseId);
            if (isCourseAssigned.size() != 0)
            {
                int attendeesCount = jdbcTemplate.queryForObject(GET_ATTENDEES_COUNT,new Object[]{courseId},Integer.class);
                int nonAttendeesCount = jdbcTemplate.queryForObject(GET_NON_ATTENDEES_COUNT,new Object[]{courseId},Integer.class);
                String completionStatus = jdbcTemplate.queryForObject(GET_COMPLETION_STATUS,new Object[]{courseId},String.class);
                if (completionStatus.equalsIgnoreCase("active") || completionStatus.equalsIgnoreCase("upcoming"))
                {
                    return "Attendees: "+attendeesCount;
                }
                int attendeesforCompletedCourse = jdbcTemplate.queryForObject("select count(empId) from acceptedInvites where courseId=? and deleteStatus=false",new Object[]{courseId}, Integer.class);
                return "Attendees: "+attendeesforCompletedCourse+"\nNon Attendees: "+nonAttendeesCount;
            }
        }
        catch (DataAccessException e)
        {
            return "No such courseId exist";
        }
        return null;
    }
    public String getAttendeesAndNonAttendeesCount(int courseId,String empId)
    {
        if(getRole((empId)).equalsIgnoreCase("admin"))
        {
            String employeeCount = getAttendeesAndNonAttendeesForAdmin(courseId);
            return employeeCount;
        }
        if (getRole((empId)).equalsIgnoreCase("manager"))
        {
            String employeeCount = getAttendeesAndNonAttendeesForManager(courseId,empId);
            return employeeCount;
        }
        return null;
    }

    public Map<Integer,List<EmployeeProfile>> attendingEmployeeForAdmin(int courseId,int offset,int limit,Map<Integer,List<EmployeeProfile>> map)
    {
        String completionStatus = jdbcTemplate.queryForObject(GET_COMPLETION_STATUS,new Object[]{courseId},String.class);
        List<EmployeeProfile> employeeProfileList=null;
        if (completionStatus.equalsIgnoreCase("active") || completionStatus.equalsIgnoreCase("upcoming"))
        {
            employeeProfileList = jdbcTemplate.query(GET_ATTENDEES,(rs, rowNum) -> {
                return new EmployeeProfile(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),rs.getString("profile_pic"));
            },courseId,offset,limit);
        }
        if (completionStatus.equalsIgnoreCase("completed"))
        {
            employeeProfileList = jdbcTemplate.query("SELECT emp_Id,emp_Name,designation,profile_pic FROM Employee,AcceptedInvites where Employee.emp_id=AcceptedInvites.empId and AcceptedInvites.deleteStatus=false and AcceptedInvites.courseId=? limit ?,?",(rs, rowNum) -> {
                return new EmployeeProfile(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),rs.getString("profile_pic"));
            },courseId,offset,limit);
        }
        if (employeeProfileList.size()!=0)
        {
            map.put(employeeProfileList.size(),employeeProfileList);
            return map;
        }
        return null;
    }
    public Map<Integer,List<EmployeeProfile>> attendingEmployeeForManager(int courseId,String empId,int offset,int limit,Map<Integer,List<EmployeeProfile>> map)
    {
        try
        {
            List<ManagersCourses> isCourseAssigned = jdbcTemplate.query(CHECK_COURSE_ALLOCATION, (rs, rowNum) -> {
                return new ManagersCourses(rs.getInt("courseId"));
            }, empId, courseId);
            if (isCourseAssigned.size() != 0 )
            {
                String completionStatus = jdbcTemplate.queryForObject(GET_COMPLETION_STATUS,new Object[]{courseId},String.class);
                List<EmployeeProfile> employeeProfileList=null;
                if (completionStatus.equalsIgnoreCase("active") || completionStatus.equalsIgnoreCase("upcoming"))
                {
                    employeeProfileList = jdbcTemplate.query(GET_ATTENDEES,(rs, rowNum) -> {
                        return new EmployeeProfile(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),rs.getString("profile_pic"));
                    },courseId,offset,limit);
                }
                if (completionStatus.equalsIgnoreCase("completed"))
                {
                    employeeProfileList = jdbcTemplate.query("SELECT emp_Id,emp_Name,designation,profile_pic FROM Employee,AcceptedInvites where Employee.emp_id=AcceptedInvites.empId and AcceptedInvites.deleteStatus=false and AcceptedInvites.courseId=? limit ?,?",(rs, rowNum) -> {
                        return new EmployeeProfile(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),rs.getString("profile_pic"));
                    },courseId,offset,limit);
                }
                if (employeeProfileList.size()!=0)
                {
                    map.put(employeeProfileList.size(),employeeProfileList);
                    return map;
                }
            }
        }
        catch (DataAccessException e)
        {
            return null;
        }
        return null;
    }

    public Map<Integer,List<EmployeeProfile>> getAttendingEmployee(int courseId, String empId,int page,int limit)
    {
        Map map = new HashMap<Integer,List>();
        offset = limit *(page-1);
        if(getRole((empId)).equalsIgnoreCase("admin"))
        {
            Map<Integer,List<EmployeeProfile>> attendingEmployee = attendingEmployeeForAdmin(courseId,offset,limit,map);
            return attendingEmployee;
        }
        if (getRole((empId)).equalsIgnoreCase("manager"))
        {
            Map<Integer,List<EmployeeProfile>> attendingEmployee = attendingEmployeeForManager(courseId,empId,offset,limit,map);
            return attendingEmployee;
        }
        return null;
    }

    //non attending employee
    public Map<Integer,List<EmployeeProfile>> nonAttendingEmployeeForAdmin(int courseId,int offset,int limit,Map<Integer,List<EmployeeProfile>> map)
    {
        List<EmployeeProfile> employeeProfileList = jdbcTemplate.query(GET_NON_ATTENDEES,(rs, rowNum) -> {
            return new EmployeeProfile(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),rs.getString("profile_pic"));
        },courseId,offset,limit);
        if (employeeProfileList.size()!=0)
        {
            map.put(employeeProfileList.size(),employeeProfileList);
            return map;
        }
        return null;
    }

    public Map<Integer,List<EmployeeProfile>> nonAttendingEmployeeForManager(int courseId,String empId,int offset,int limit,Map<Integer,List<EmployeeProfile>> map)
    {
        try
        {
            List<ManagersCourses> isCourseAssigned = jdbcTemplate.query(CHECK_COURSE_ALLOCATION, (rs, rowNum) -> {
                return new ManagersCourses(rs.getInt("courseId"));
            }, empId, courseId);
            if (isCourseAssigned.size() != 0 )
            {
                List<EmployeeProfile> employeeProfileList = jdbcTemplate.query(GET_NON_ATTENDEES,(rs, rowNum) -> {
                    return new EmployeeProfile(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),rs.getString("profile_pic"));
                },courseId,offset,limit);
                if (employeeProfileList.size()!=0)
                {
                    map.put(employeeProfileList.size(),employeeProfileList);
                    return map;
                }
            }
        }
        catch (DataAccessException e)
        {
            return null;
        }
        return null;
    }

    public Map<Integer,List<EmployeeProfile>> getNonAttendingEmployee(int courseId,String empId,int page,int limit)
    {
        Map map = new HashMap<Integer,List>();
        offset = limit *(page-1);
        if(getRole((empId)).equalsIgnoreCase("admin"))
        {
            Map<Integer,List<EmployeeProfile>> nonAttendingEmployee = nonAttendingEmployeeForAdmin(courseId,offset,limit,map);
            return nonAttendingEmployee;
        }
        if (getRole((empId)).equalsIgnoreCase("manager"))
        {
            Map<Integer,List<EmployeeProfile>> nonAttendingEmployee = nonAttendingEmployeeForManager(courseId,empId,offset,limit,map);
            return nonAttendingEmployee;
        }
        return null;
    }

    //invite employees for a course
    public List<EmployeeInvite> employeesToInviteForAdmin(int courseId,List<EmployeeInvite> employeeList, List<EmployeeInvite> employeeList1, List<EmployeeInvite> employeeList2)
    {
        employeeList = jdbcTemplate.query(GET_INVITED_EMPLOYEES,(rs, rowNum) -> {
            return new EmployeeInvite(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),true);
        },courseId);
        //Employees who rejected the invites and employees who are not invited
        employeeList1 = jdbcTemplate.query(GET_NON_INVITED_EMPLOYEES,(rs, rowNum) -> {
            return new EmployeeInvite(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),false);
        },courseId);
        employeeList2.addAll(employeeList);
        employeeList2.addAll(employeeList1);
        return employeeList2;
    }
    public List<EmployeeInvite> employeesToInviteForManager(int courseId, String empId,List<EmployeeInvite> employeeList, List<EmployeeInvite> employeeList1, List<EmployeeInvite> employeeList2)
    {
        try
        {
            List<ManagersCourses> isCourseAssigned = jdbcTemplate.query(CHECK_COURSE_ALLOCATION, (rs, rowNum) -> {
                return new ManagersCourses(rs.getInt("courseId"));
            }, empId, courseId);
            if (isCourseAssigned.size() != 0 )
            {
                employeeList = jdbcTemplate.query(GET_INVITED_EMPLOYEES_FOR_MANAGER,(rs, rowNum) -> {
                    return new EmployeeInvite(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),true);
                },empId,courseId);
                employeeList1 = jdbcTemplate.query(GET_NON_INVITED_EMPLOYEES_FOR_MANAGER,(rs, rowNum) -> {
                    return new EmployeeInvite(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),false);
                },empId,courseId);
                employeeList2.addAll(employeeList);
                employeeList2.addAll(employeeList1);
                return employeeList2;
            }
        }
        catch (DataAccessException e)
        {
            return null;
        }
        return null;
    }

    public void isCourseIdValid(int courseId) throws CourseNotValidException
    {
        String query = "select courseId from course where courseId=? and (completionStatus = 'upcoming' or (completionStatus='active' and trainingMode ='Online Sessions'));";
        try
        {
            jdbcTemplate.queryForObject(query,new Object[]{courseId},Integer.class);
        }
        catch (DataAccessException e)
        {
           throw  new CourseNotValidException("Cannot invite employees for this course");
        }
    }

    public List<EmployeeInvite> getEmployeesToInvite(int courseId,String empId) throws CourseNotValidException
    {
        isCourseIdValid(courseId);
        List<EmployeeInvite> employeeList = new ArrayList<>();
        List<EmployeeInvite> employeeList1 = new ArrayList<>();
        List<EmployeeInvite> employeeList2= new ArrayList<>();
        if(getRole((empId)).equalsIgnoreCase("admin"))
        {
            return employeesToInviteForAdmin(courseId,employeeList,employeeList1,employeeList2);
        }
        if (getRole((empId)).equalsIgnoreCase("manager"))
        {
            return employeesToInviteForManager(courseId,empId,employeeList,employeeList1,employeeList2);
        }
        return employeeList2;
    }

    public String inviteEmployees(int courseId,List<MultipleEmployeeRequest> inviteToEmployees, String empId) throws CourseNotValidException
    {
        try
        {
            isCourseIdValid(courseId);
        }
        catch (CourseNotValidException e)
        {
            return e.getMessage();
        }
        int noOfInvites = inviteToEmployees.size();
        int count = 0;
        if(getRole((empId)).equalsIgnoreCase("admin"))
        {
            for (int i=0; i<noOfInvites; i++)
            {
                //already invited
                List<Employee> isInvited = jdbcTemplate.query("SELECT emp_id FROM Employee,Invites WHERE Employee.emp_id=? and Employee.emp_id<>'RT001' and Employee.emp_id=Invites.empId and courseId=? and (acceptanceStatus=true or acceptanceStatus is null)", (rs, rowNum) -> {
                    return new Employee(rs.getString("emp_Id"));
                }, inviteToEmployees.get(i).getEmpId(), courseId);
                if (isInvited.size() == 0)
                {
                    count++;
                    jdbcTemplate.update(INVITE_EMPLOYEES, new Object[]{inviteToEmployees.get(i).getEmpId(), courseId,});
                }
            }
            if (count == 0)
            {
                return "They are already invited for this course";
            }
        }
        if (getRole((empId)).equalsIgnoreCase("manager"))
        {
            List<ManagersCourses> isCourseAssigned = jdbcTemplate.query(CHECK_COURSE_ALLOCATION, (rs, rowNum) -> {
                return new ManagersCourses(rs.getInt("courseId"));
            }, empId, courseId);
            if (isCourseAssigned.size() != 0)
            {
                for (int i=0; i<noOfInvites; i++)
                {
                    List<Employee> isInvited = jdbcTemplate.query("SELECT emp_id FROM Employee,Invites WHERE Employee.emp_id=? and Employee.emp_id<>'RT001' and Employee.emp_id=Invites.empId and courseId=? and (acceptanceStatus=true or acceptanceStatus is null)", (rs, rowNum) -> {
                        return new Employee(rs.getString("emp_Id"));
                    }, inviteToEmployees.get(i).getEmpId(), courseId);
                    if (isInvited.size() == 0)
                    {
                        count++;
                        jdbcTemplate.update(INVITE_EMPLOYEES,new Object[]{inviteToEmployees.get(i).getEmpId(),courseId});
                    }
                }
                if (count == 0)
                {
                    return "They are already invited for this course";
                }
            }
            else
            {
                return "You cannot assign employees for this course";
            }
        }
        return "Invited successfully";
    }

    public void isInviteListValid(int courseId,List<MultipleEmployeeRequest> deleteInvites) throws Exception
    {
        String query = "select empId from invites where courseId=? and (acceptanceStatus = 1 or acceptanceStatus is null) and empId=?";
        for (MultipleEmployeeRequest emp:deleteInvites)
        {
            try
            {
                jdbcTemplate.queryForObject(query, String.class,courseId,emp.getEmpId());
            }
            catch (DataAccessException e)
            {
                throw new Exception("Employee "+emp.getEmpId()+" not yet invited");
            }
        }
    }

    //to delete invite
    public String deleteInvite(int courseId, List<MultipleEmployeeRequest> deleteInvites,String empId) throws Exception
    {
        isInviteListValid(courseId, deleteInvites);
        int noOfInvites = deleteInvites.size();
        if(getRole((empId)).equalsIgnoreCase("admin"))
        {
            for(int i=0; i<noOfInvites; i++)
            {
                jdbcTemplate.update(DELETE_INVITE_FROM_INVITES, courseId, deleteInvites.get(i).getEmpId());
                jdbcTemplate.update(DELETE_FROM_ACCEPTED_INVITES, courseId, deleteInvites.get(i).getEmpId());
            }
        }
        if (getRole((empId)).equalsIgnoreCase("manager"))
        {
            List<ManagersCourses> isCourseAssigned = jdbcTemplate.query(CHECK_COURSE_ALLOCATION, (rs, rowNum) -> {
                return new ManagersCourses(rs.getInt("courseId"));
            }, empId, courseId);
            if (isCourseAssigned.size() != 0) {
                for (int i = 0; i < noOfInvites; i++) {
                    jdbcTemplate.update(DELETE_INVITE_FROM_INVITES, courseId, deleteInvites.get(i).getEmpId());
                    jdbcTemplate.update(DELETE_FROM_ACCEPTED_INVITES, courseId, deleteInvites.get(i).getEmpId());
                }
            }
            else
            {
                return "You cannot delete employees for this course";
            }
        }
        return "Deleted invite successfully";
    }

    public List<EmployeeDetails> mapEmployeeToCourseStatusCount(List<EmployeeDetails> employeeDetails)
    {
        for (EmployeeDetails e:employeeDetails)
        {
            String emp_Id=e.getEmpId();
            Integer var=getActiveCourseCountForEmployee(emp_Id);
            e.setActiveCount(var);
            var=getUpcomingCourseCountForEmployee(emp_Id);
            e.setUpcomingCount(var);
            var=getAttendedCourseCountForEmployee(emp_Id);
            e.setAttendedCount(var);
        }
        return employeeDetails;
    }

    //Checks if the role is manager or admin and calls the corresponding function
    public Map<Integer,List<EmployeeDetails>> employeeDetails(String empId,int page, int limit)
    {
        Map map = new HashMap<Integer,List<EmployeeDetails>>();
        offset = limit *(page-1);
        String role = getRole(empId);
        List<EmployeeDetails> employeeDetails;
        if(role.equals("admin")){
            employeeDetails= employeeDetailsListForAdmin(offset,limit);
        }else{
            employeeDetails= employeeDetailsListForManager(empId,offset,limit);
        }
        List<EmployeeDetails> employeeDetailsList = mapEmployeeToCourseStatusCount(employeeDetails);
        map.put(employeeDetailsList.size(),employeeDetailsList);
        return map;

    }


    //Gives List of Employees for Admin
    public List<EmployeeDetails> employeeDetailsListForAdmin(int offset,int limit)
    {
        String queryForEmployees = "SELECT emp_id, emp_name, designation FROM employee WHERE delete_status = 0 AND emp_id <> 'RT001' limit ?,?";
        List<EmployeeDetails> a = jdbcTemplate.query(queryForEmployees,new BeanPropertyRowMapper<EmployeeDetails>(EmployeeDetails.class),offset,limit);
        return a;

    }

    //Gives List of Employees for Manager
    public List<EmployeeDetails> employeeDetailsListForManager(String managerId,int offset,int limit)
    {
        String queryForEmployees = "SELECT emp_id, emp_name, designation FROM employee, manager WHERE employee.emp_id = manager.empId and manager.managerId = ? AND delete_status = 0 AND emp_id <> 'RT001' limit ?,?";

        return jdbcTemplate.query(queryForEmployees,new BeanPropertyRowMapper<EmployeeDetails>(EmployeeDetails.class),managerId,offset,limit);
    }

    //Get Employee Course Status count
    public Integer getActiveCourseCountForEmployee(String empId)
    {
        String query = "SELECT COUNT(empId) as c FROM AcceptedInvites, Course WHERE course.courseId = AcceptedInvites.courseId and empId = ? and course.completionStatus = 'active' and AcceptedInvites.deleteStatus = false and Course.deleteStatus=false";
        return jdbcTemplate.queryForObject(query,Integer.class,empId);
    }

    public Integer getUpcomingCourseCountForEmployee(String empId)
    {
        String query = "SELECT COUNT(empId) FROM AcceptedInvites, Course WHERE course.courseId = AcceptedInvites.courseId and empId = ?  and course.completionStatus = 'upcoming' and AcceptedInvites.deleteStatus = false and Course.deleteStatus=false";
        return jdbcTemplate.queryForObject(query, Integer.class,empId);
    }

    //count of completed course
    public Integer getAttendedCourseCountForEmployee(String empId)
    {
        String query = "SELECT COUNT(empId) FROM AcceptedInvites, Course WHERE course.courseId = AcceptedInvites.courseId and empId = ?  and course.completionStatus = 'completed' and AcceptedInvites.deleteStatus = false and Course.deleteStatus=false";
        return jdbcTemplate.queryForObject(query, Integer.class,empId);
    }

    //Employee Details Based on Search Key   (Method)
    public Map<Integer,List<EmployeeDetails>> employeeDetailsBySearchKey(String empId,String searchKey,int page,int limit)
    {
        Map map = new HashMap<Integer,List<EmployeeDetails>>();
        offset = limit *(page-1);
        String role = getRole(empId);
        List<EmployeeDetails> employeeDetails;
        if(role.equals("admin")){
            employeeDetails= employeeDetailsListForAdminBySearchKey(searchKey,offset,limit);
        }else{
            employeeDetails= employeeDetailsListForManagerBySearchKey(empId,searchKey,offset,limit);
        }
        List<EmployeeDetails> employeeDetailsList =  mapEmployeeToCourseStatusCount(employeeDetails);
        map.put(employeeDetailsList.size(),employeeDetailsList);
        return map;
    }

    public List<EmployeeDetails> employeeDetailsListForAdminBySearchKey(String searchKey,int offset,int limit)
    {

        String queryForEmployees = "SELECT emp_id, emp_name, designation FROM employee WHERE (emp_id = ? or emp_name like ? or designation like ?) and delete_status = 0 AND emp_id <> 'RT001' limit ?,?";
        List<EmployeeDetails> a = jdbcTemplate.query(queryForEmployees,new BeanPropertyRowMapper<EmployeeDetails>(EmployeeDetails.class),searchKey,"%"+searchKey+"%","%"+searchKey+"%",offset,limit);
        return a;
    }

    //Gives List of Employees for Manager
    public List<EmployeeDetails> employeeDetailsListForManagerBySearchKey(String managerId, String searchKey,int offset,int limit)
    {
        String queryForEmployees = "SELECT emp_id, emp_name, designation FROM employee, manager WHERE employee.emp_id = manager.empId and (emp_id = ? or emp_name like ? or designation like ?) and manager.managerId = ? AND delete_status = 0 AND emp_id <> 'RT001' limit ?,?";

        return jdbcTemplate.query(queryForEmployees,new BeanPropertyRowMapper<EmployeeDetails>(EmployeeDetails.class),searchKey,"%"+searchKey+"%","%"+searchKey+"%",managerId,offset,limit);
    }

    //Filter Course based on date and Completion status for Active and Upcoming
    public List<Course> FilterCoursesForAdminByActiveAndUpcomingStatus(FilterByDate filter, int offset, int limit)
    {
        String query = "SELECT course.courseId,courseName,trainer,trainingMode,startDate,endDate,duration,startTime,endTime,completionStatus FROM Course WHERE completionStatus=? and deleteStatus=false and (startDate >= ? and startDate <= ? ) limit ?,?";
        return jdbcTemplate.query(query,new BeanPropertyRowMapper<Course>(Course.class),filter.getCompletionStatus(),filter.getDownDate(),filter.getTopDate(),offset,limit);
    }

    public List<Course> FilterCoursesForManagerByActiveAndUpcomingStatus(FilterByDate filter, String empId,int offset, int limit)
    {
        String query = "SELECT course.courseId,courseName,trainer,trainingMode,startDate,endDate,duration,startTime,endTime,completionStatus FROM Course,managerscourses WHERE course.courseId = managerscourses.courseID and managerID=? and completionStatus=? and deleteStatus=false and (startDate >= ? and startDate <= ? limit ?,?)";
        return jdbcTemplate.query(query,new BeanPropertyRowMapper<Course>(Course.class),empId,filter.getCompletionStatus(),filter.getDownDate(),filter.getTopDate(),offset,limit);
    }

    //Filter Course based on date and Completion status for Completed Courses
    public List<Course> FilterCoursesForAdminByCompletedStatus(FilterByDate filter,int offset, int limit)
    {
        String query = "SELECT course.courseId,courseName,trainer,trainingMode,startDate,endDate,duration,startTime,endTime,completionStatus FROM Course WHERE completionStatus=? and deleteStatus=false and (endDate >= ? and endDate <= ? limit ?,?)";
        return jdbcTemplate.query(query,new BeanPropertyRowMapper<Course>(Course.class),filter.getCompletionStatus(),filter.getDownDate(),filter.getTopDate(),offset,limit);
    }

    public List<Course> FilterCoursesForManagerByCompletedStatus(FilterByDate filter, String empId,int offset, int limit)
    {
        String query = "SELECT course.courseId,courseName,trainer,trainingMode,startDate,endDate,duration,startTime,endTime,completionStatus FROM Course,managerscourses WHERE course.courseId = managerscourses.courseID and managerID=? and completionStatus=? and deleteStatus=false and (endDate >= ? and endDate <= ? limit ?,?)";
        return jdbcTemplate.query(query,new BeanPropertyRowMapper<Course>(Course.class),empId,filter.getCompletionStatus(),filter.getDownDate(),filter.getTopDate(), offset, limit);
    }

    //Method to sort admin and manager to filter the date
    public Map<Integer,List<Course>> filteredCourses(String empId, FilterByDate filter,int page, int limit)
    {
        Map map = new HashMap<Integer,List>();
        offset = limit *(page-1);
        String role = getRole(empId);
        if(role.equals("admin") )
        {
            List<Course> filteredCourse=null;
            if (filter.getCompletionStatus().matches("active|upcoming")){
                filteredCourse = FilterCoursesForAdminByActiveAndUpcomingStatus(filter,offset,limit);
                map.put(filteredCourse.size(),filteredCourse);
                return map;
            }
            else
            {
                filteredCourse = FilterCoursesForAdminByCompletedStatus(filter,offset,limit);
                map.put(filteredCourse.size(),filteredCourse);
                return map;
            }

        }
        else
        {
            List<Course> filteredCourse=null;
            if(filter.getCompletionStatus().matches("active|upcoming")){
                filteredCourse = FilterCoursesForManagerByActiveAndUpcomingStatus(filter,empId,offset,limit);
                map.put(filteredCourse.size(),filteredCourse);
                return map;
            }
            filteredCourse = FilterCoursesForManagerByCompletedStatus(filter,empId,offset,limit);
            map.put(filteredCourse.size(),filteredCourse);
            return map;
        }
    }
}
