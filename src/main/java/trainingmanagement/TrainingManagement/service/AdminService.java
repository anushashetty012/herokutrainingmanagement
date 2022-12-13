package trainingmanagement.TrainingManagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Repository;
import trainingmanagement.TrainingManagement.customException.*;
import trainingmanagement.TrainingManagement.entity.Course;
import trainingmanagement.TrainingManagement.request.ManagerEmployees;
import trainingmanagement.TrainingManagement.request.MultipleEmployeeRequest;
import trainingmanagement.TrainingManagement.response.*;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AdminService
{
    private String CREATE_COURSE = "INSERT INTO Course(courseName,trainer,trainingMode,startDate,endDate,duration,startTime,endTime,meetingInfo,startTimestamp,endTimestamp) values(?,?,?,?,?,?,?,?,?,?,?)";

    //allocate project manager
    private String COURSES_TO_MANAGER = "SELECT courseId,courseName,trainer,trainingMode,startDate,endDate,duration,startTime,endTime,completionStatus FROM Course WHERE completionStatus='upcoming' and deleteStatus=false LIMIT ?,?";
    private String GET_MANAGERS = "SELECT employee.emp_Id,emp_Name,designation FROM employee, employee_role WHERE employee.emp_id = employee_role.emp_id and employee_role.role_name='manager' AND employee.delete_status=false LIMIT ?,?";
    private String ASSIGN_MANAGER = "INSERT INTO ManagersCourses(managerId,courseId) VALUES(?,?)";
    int offset=0;

    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    private JavaMailSender mailSender;

    public Timestamp createTimestamp(Date date, Time time)
    {
        Timestamp t= new Timestamp(date.getYear(), date.getMonth(), date.getDate(), time.getHours(), time.getMinutes(), time.getSeconds(),0);
        return t;
    }

    public String createCourse(Course course) throws CourseInfoIntegrityException
    {
        courseInfoIntegrity(course);
        Timestamp startTimestamp=createTimestamp(course.getStartDate(),course.getStartTime());
        Timestamp endTimestamp=null;
        try
        {
            checkEndTimeExist(course);
            endTimestamp=createTimestamp(course.getEndDate(),course.getEndTime());
        } catch (CourseInfoIntegrityException e) {

        }
        jdbcTemplate.update(CREATE_COURSE,course.getCourseName(),course.getTrainer(),course.getTrainingMode(),course.getStartDate(),course.getEndDate(),course.getDuration(),course.getStartTime(),course.getEndTime(),course.getMeetingInfo(),startTimestamp,endTimestamp);
        return "Course created successfully";
    }

    //to allocate managers to course
    public Map<Integer,List<CourseList>> getCourseToAssignManager(int page, int limit)
    {
        Map map = new HashMap<Integer,List>();
        offset = limit *(page-1);
        List<CourseList> courseList =  jdbcTemplate.query(COURSES_TO_MANAGER,(rs, rowNum) -> {
            return new CourseList(rs.getInt("courseId"),rs.getString("courseName"),rs.getString("trainer"),rs.getString("trainingMode"),rs.getDate("startDate"),rs.getDate("endDate"),rs.getTime("duration"),rs.getTime("startTime"),rs.getTime("endTime"),rs.getString("completionStatus"));
        },offset,limit);
        if (courseList.size()!=0)
        {
            map.put(courseList.size(),courseList);
            return map;
        }
        return null;
    }

    public List<ManagerInfo> getManagersAssignedToCourse(int courseId, int offset, int limit) throws CourseDeletionException
    {
        String query = "SELECT employee.emp_Id,emp_Name,designation FROM employee, employee_role WHERE employee.emp_id = employee_role.emp_id and employee_role.role_name='manager' AND employee.delete_status=false and employee_role.emp_id in (select managerId from ManagersCourses where courseId=?) LIMIT ?,?";
        List<ManagerInfo> employeeDetails =  jdbcTemplate.query(query,(rs, rowNum) -> {
            return new ManagerInfo(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),true);
        },courseId,offset,limit);
       return employeeDetails;
    }

    public Map<Integer,List<ManagerInfo>> getManagersToAssignCourse(int courseId, int page, int limit) throws CourseDeletionException
    {
        isCourseExist(courseId,false);
        Map map = new HashMap<Integer,List>();
        offset = limit *(page-1);
        String query = "SELECT employee.emp_Id,emp_Name,designation FROM employee, employee_role WHERE employee.emp_id = employee_role.emp_id and employee_role.role_name='manager' AND employee.delete_status=false and employee_role.emp_id not in (select managerId from ManagersCourses where courseId=?) LIMIT ?,?";
        List<ManagerInfo> employeeDetails =  jdbcTemplate.query(query,(rs, rowNum) -> {
            return new ManagerInfo(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),false);
        },courseId,offset,limit);
        employeeDetails.addAll(getManagersAssignedToCourse(courseId,offset,limit));
        if (employeeDetails.size() != 0)
        {
            map.put(employeeDetails.size(),employeeDetails);
            return map;
        }
        return null;
    }

    public List<ManagerInfo> getManagersAssignedToCourseBySearchkey(int courseId, String searchKey)
    {
        String GET_MANAGERS = "SELECT employee.emp_id,emp_name,designation FROM employee, employee_role WHERE employee.emp_id = employee_role.emp_id and employee_role.role_name='manager' AND employee.delete_status=false and (employee.emp_id=? or employee.emp_name like ? or employee.designation like ?) and employee_role.emp_id in (select managerId from ManagersCourses where courseId=?)";
        List<ManagerInfo> employeeDetails =  jdbcTemplate.query(GET_MANAGERS,(rs, rowNum) -> {
            return new ManagerInfo(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),true);
        },searchKey,"%"+searchKey+"%","%"+searchKey+"%",courseId);
       return  employeeDetails;
    }

    public List<ManagerInfo> getManagersToAssignCourseBySearchkey(int courseId, String searchKey)
    {
        String GET_MANAGERS = "SELECT employee.emp_id,emp_name,designation FROM employee, employee_role WHERE employee.emp_id = employee_role.emp_id and employee_role.role_name='manager' AND employee.delete_status=false and (employee.emp_id=? or employee.emp_name like ? or employee.designation like ?) and employee_role.emp_id not in (select managerId from ManagersCourses where courseId=?)";
        List<ManagerInfo> employeeDetails =  jdbcTemplate.query(GET_MANAGERS,(rs, rowNum) -> {
            return new ManagerInfo(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),false);
        },searchKey,"%"+searchKey+"%","%"+searchKey+"%",courseId);
        employeeDetails.addAll(getManagersAssignedToCourseBySearchkey(courseId,searchKey));
        if (employeeDetails.size() != 0)
        {
            return employeeDetails;
        }
        return null;
    }

    public Map<Integer,List<EmployeeInfo>> getManagers(int page, int limit)
    {
        Map map = new HashMap<Integer,List>();
        offset = limit *(page-1);
        List<EmployeeInfo> employeeDetails =  jdbcTemplate.query(GET_MANAGERS,(rs, rowNum) -> {
            return new EmployeeInfo(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"));
        },offset,limit);
        if (employeeDetails.size() != 0)
        {
            map.put(employeeDetails.size(),employeeDetails);
            return map;
        }
        return null;
    }

    public List<EmployeeInfo> getManagersBySearchkey(String searchKey)
    {
        String GET_MANAGERS = "SELECT employee.emp_id,emp_name,designation FROM employee, employee_role WHERE employee.emp_id = employee_role.emp_id and employee_role.role_name='manager' AND employee.delete_status=false and (employee.emp_id=? or employee.emp_name like ? or employee.designation like ?)";
        List<EmployeeInfo> employeeDetails =  jdbcTemplate.query(GET_MANAGERS,(rs, rowNum) -> {
            return new EmployeeInfo(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"));
        },searchKey,"%"+searchKey+"%","%"+searchKey+"%");
        if (employeeDetails.size() != 0)
        {
            return employeeDetails;
        }
        return null;
    }

    public void checkValidityOfManagerList(List<MultipleEmployeeRequest> courseToManager,int courseId) throws SuperAdminIdException, ManagerNotExistException, EmployeeNotExistException, CourseAssignedForManagerException {
        for (MultipleEmployeeRequest emp:courseToManager)
        {
            checkEmployeeExist(emp.getEmpId());
            checkManagerExist(emp.getEmpId());
            isSuperAdminId(emp.getEmpId());
            isCourseAssignedToManager(emp.getEmpId(), courseId);
        }
    }
    public void isCourseAssignedToManager(String empId,int courseId) throws CourseAssignedForManagerException
    {
        String query = "select count(managerId) from ManagersCourses where managerId=? and courseId=?";
        int count = jdbcTemplate.queryForObject(query, Integer.class,empId,courseId);
        if (count!=0)
        {
            throw new CourseAssignedForManagerException("This course is already assigned");
        }
    }

    public String assignCourseToManager(int courseId, List<MultipleEmployeeRequest> courseToManager) throws ManagerNotExistException, SuperAdminIdException, EmployeeNotExistException, CourseDeletionException, CourseAssignedForManagerException {
        isCourseExist(courseId,false);
        checkValidityOfManagerList(courseToManager,courseId);
        int noOfManagers = courseToManager.size();
        for (int i = 0; i < noOfManagers; i++)
        {
            jdbcTemplate.update(ASSIGN_MANAGER, new Object[]{courseToManager.get(i).getEmpId(), courseId});
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("spring.email.from@gmail.com");
            String query = "select email from employee where emp_id=?";
            String query1 = "select courseName from Course where courseId=?";
            String email = jdbcTemplate.queryForObject(query, String.class,courseToManager.get(i).getEmpId());
            message.setTo(email);
            String courseName = jdbcTemplate.queryForObject(query1, String.class,courseId);
            String emailText ="You have been assigned "+courseName+" course";
            message.setText(emailText);
            message.setSubject("Course assigned");
            mailSender.send(message);
        }
        return "Course allocated successfully";
    }

    public String unassignCourseFromManager(int courseId, List<MultipleEmployeeRequest> courseToManager) throws ManagerNotExistException, SuperAdminIdException, EmployeeNotExistException, CourseDeletionException, CourseAssignedForManagerException {
        isCourseExist(courseId,false);
        checkValidityOfManagerListForUnAssigning(courseToManager,courseId);
        int noOfManagers = courseToManager.size();
        for (int i = 0; i < noOfManagers; i++)
        {
            String UNASSIGN_MANAGER = "delete from ManagersCourses where courseId=? and managerId=?";
            jdbcTemplate.update(UNASSIGN_MANAGER, new Object[]{courseId,courseToManager.get(i).getEmpId()});
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("spring.email.from@gmail.com");
            String query = "select email from employee where emp_id=?";
            String query1 = "select courseName from Course where courseId=?";
            String email = jdbcTemplate.queryForObject(query, String.class,courseToManager.get(i).getEmpId());
            message.setTo(email);
            String courseName = jdbcTemplate.queryForObject(query1, String.class,courseId);
            String emailText ="You have been unassigned from "+courseName+" course";
            message.setText(emailText);
            message.setSubject("Course unassigned");
            mailSender.send(message);
        }
        return "Course unallocated successfully";
    }

    public void checkValidityOfManagerListForUnAssigning(List<MultipleEmployeeRequest> courseToManager,int courseId) throws SuperAdminIdException, ManagerNotExistException, EmployeeNotExistException, CourseAssignedForManagerException {
        for (MultipleEmployeeRequest emp:courseToManager)
        {
            checkEmployeeExist(emp.getEmpId());
            checkManagerExist(emp.getEmpId());
            isSuperAdminId(emp.getEmpId());
            checkCourseAssignedToManager(emp.getEmpId(), courseId);
        }
    }

    public void checkCourseAssignedToManager(String empId,int courseId) throws CourseAssignedForManagerException
    {
        String query = "select count(managerId) from ManagersCourses where managerId=? and courseId=?";
        int count = jdbcTemplate.queryForObject(query, Integer.class,empId,courseId);
        if (count==0)
        {
            throw new CourseAssignedForManagerException("This course is not assigned these managers");
        }
    }

    //Update Existing Course
    public Integer updateCourse(Course course) throws CourseInfoIntegrityException
    {
        String courseStatus = checkCourseStatus(course.getCourseId(),false);
        if (courseStatus.equalsIgnoreCase("completed"))
        {
            throw new CourseInfoIntegrityException("course doesn't exist or it is completed");
        }
        if (courseStatus.equalsIgnoreCase("upcoming"))
        {
            courseInfoIntegrity(course);
            return updateCourses(course);
        }
        course = getStartDateAndTime(course);
        courseInfoIntegrityForUpdatingActiveCourse(course);
        return updateCourses(course);
    }

    public void checkEndTimeExistForEndDate(Course course) throws CourseInfoIntegrityException
    {
        if (course.getEndDate() == null && course.getEndTime() != null)
        {
            throw new CourseInfoIntegrityException("End date is null, you cannot enter end time");
        }
    }
    public void courseInfoIntegrityForUpdatingActiveCourse(Course course) throws CourseInfoIntegrityException
    {
        if (course.getCourseName().trim().isEmpty())
        {
            throw new CourseInfoIntegrityException("Course Name can't be empty");
        }
        checkEndTimeExistForEndDate(course);
        courseModeIntegrity(course.getTrainingMode());
        try {
            int i=course.getStartDate().compareTo(course.getEndDate());
            if(i>0)
            {
                throw new CourseInfoIntegrityException("end date cant be before start date");
            }
            checkEndTimeExist(course);
            if(i==0)
            {
                checkTime(course);
            }
        }
        catch (NullPointerException e)
        {

        }
    }

    public int updateCourses(Course course)
    {
        Timestamp startTimestamp=createTimestamp(course.getStartDate(),course.getStartTime());
        Timestamp endTimestamp=null;
        try
        {
            checkEndTimeExist(course);
            endTimestamp=createTimestamp(course.getEndDate(),course.getEndTime());
        }
        catch (CourseInfoIntegrityException e) {

        }
        String query = "update Course set courseName =?, trainer=?, trainingMode=?, startDate=?, endDate =?, duration=?, startTime =?, endTime =?, meetingInfo=?,startTimestamp=?,endTimestamp=? where courseId = ? and deleteStatus=0";
        return jdbcTemplate.update(query, course.getCourseName(),course.getTrainer(),course.getTrainingMode(),course.getStartDate(),course.getEndDate(),course.getDuration(),course.getStartTime(),course.getEndTime(),course.getMeetingInfo(),startTimestamp,endTimestamp,course.getCourseId());
    }

    //get start date and start time from db and attach it to update request
    public Course getStartDateAndTime(Course course)
    {
        String query = "select startDate,startTime from Course where courseId=?";
        Course course1 = jdbcTemplate.queryForObject(query,(rs, rowNum) -> {
            return new Course(rs.getDate("startDate"),rs.getTime("startTime"));
        }, course.getCourseId());
        course.setStartDate(course1.getStartDate());
        course.setStartTime(course1.getStartTime());
        return course;
    }

    public Timestamp getCurrentTimestamp()
    {
        Instant instant = Instant.now();
        String d1 = instant.toString();
        Instant s = Instant.parse(d1);
        ZoneId.of("Asia/Kolkata");
        LocalDateTime l = LocalDateTime.ofInstant(s, ZoneId.of("Asia/Kolkata"));
        return Timestamp.valueOf(l);
    }

    public void checkStartTimeForCurrentTime(Course course) throws CourseInfoIntegrityException
    {
        Timestamp startTimestamp=createTimestamp(course.getStartDate(),course.getStartTime());
        if (0 > startTimestamp.compareTo(getCurrentTimestamp()))
        {
            throw new CourseInfoIntegrityException("Start time should be greater than current time");
        }
    }

    public void checkStartTimeExist(Course course) throws CourseInfoIntegrityException
    {
        if(course.getStartTime()==null)
        {
            throw new CourseInfoIntegrityException("start time should not be null");
        }
    }

    public void checkEndTimeExist(Course course) throws CourseInfoIntegrityException
    {
        if(course.getEndTime()==null)
        {
            throw new CourseInfoIntegrityException("end time should not be null");
        }
    }

    //throws exception if start time is equal or greater than end time
    //only if start time and end time is not null
    public void checkTime(Course course) throws CourseInfoIntegrityException
    {

        if (!(course.getStartTime()==null) && !(course.getEndTime()==null))
        {
            int i=course.getStartTime().compareTo(course.getEndTime());
            if(!(i<0))
            {
                throw new CourseInfoIntegrityException("start time should be smaller than end time");
            }
        }
    }
    public int compare(int a,int b)
    {
        if(a==b)
        {
            return 0;
        }
        if(a>b)
        {
            return 1;
        }
        return -1;
    }

    public int compareCurrentdateToStartdate(Timestamp startTimestamp, Timestamp currentTimestamp)
    {
        int startDate,currentDate,startMonth,currentMonth,startYear,currentYear;
        startDate = startTimestamp.getDate();
        currentDate = currentTimestamp.getDate();
        startMonth = startTimestamp.getMonth();
        currentMonth = currentTimestamp.getMonth();
        startYear = startTimestamp.getYear();
        currentYear = currentTimestamp.getYear();

        int yearStatus=compare(startYear,currentYear);
        int monthStatus=compare(startMonth,currentMonth);
        int dateStatus=compare(startDate,currentDate);

        if(yearStatus==0)
        {
            if (monthStatus==0)
            {
                if (dateStatus==0)
                {
                    return 0;
                }
                if (dateStatus>0)
                {
                    return 1;
                }
                return -1;
            }
            if (monthStatus>0)
            {
                return 1;
            }
            return -1;
        }
        if(yearStatus>0)
        {
            return 1;
        }
        return -1;
    }

    public void courseInfoIntegrity(Course course) throws CourseInfoIntegrityException
    {
        if (course.getCourseName().trim().isEmpty())
        {
            throw new CourseInfoIntegrityException("Course Name can't be empty");
        }
        checkEndTimeExistForEndDate(course);
        courseModeIntegrity(course.getTrainingMode());
        Timestamp startTimestamp = createTimestamp(course.getStartDate(),course.getStartTime());
        int startToCurrentDateStatus = compareCurrentdateToStartdate(startTimestamp,getCurrentTimestamp());
        if(0 > startToCurrentDateStatus)
        {
            throw new CourseInfoIntegrityException("start date can't be before  current date");
        }
        if (startToCurrentDateStatus == 0)
        {
            checkStartTimeExist(course);
            checkStartTimeForCurrentTime(course);
        }
        try {
            int i=course.getStartDate().compareTo(course.getEndDate());
            if(i>0)
            {
                throw new CourseInfoIntegrityException("end date cant be before start date");
            }
            checkStartTimeExist(course);
            checkEndTimeExist(course);
            if(i==0)
            {
                checkTime(course);
            }
        }
        catch (NullPointerException e)
        {
             checkStartTimeExist(course);
        }
    }

    public void courseModeIntegrity(String trainingMode) throws CourseInfoIntegrityException
    {
        if (!(trainingMode.equalsIgnoreCase("Off-site")||trainingMode.equalsIgnoreCase("At office")||trainingMode.equalsIgnoreCase("Online sessions")))
        {
            throw new CourseInfoIntegrityException("Training mode is not valid");
        }
    }
    public void assignStatusToEmployeeToManagerList(List<EmployeesToManager> employeesToManagerList,String managerId)
    {
        for (EmployeesToManager emp:employeesToManagerList)
        {
            if (emp.getManagerId() != null && emp.getManagerId().equals(managerId))
            {
                emp.setStatus(true);
            }
        }
    }

    public Map<Integer,List<EmployeesToManager>> employeesToAssignManager(String managerId, int page, int limit) throws EmployeeNotExistException, ManagerNotExistException, SuperAdminIdException {
        checkEmployeeExist(managerId);
        checkManagerExist(managerId);
        isSuperAdminId(managerId);
        Map map = new HashMap<Integer,List>();
        offset = limit *(page-1);
        String query = "select employee.emp_id, emp_name, designation, managerId from employee, Manager \n" +
                "where employee.emp_id=Manager.empId and employee.emp_id<>'RT001' and employee.emp_id<>? and employee.delete_status=false limit ?,?";
        List<EmployeesToManager> employeeDetails =  jdbcTemplate.query(query,new BeanPropertyRowMapper<EmployeesToManager>(EmployeesToManager.class),managerId,offset,limit);
        assignStatusToEmployeeToManagerList(employeeDetails,managerId);
        if (employeeDetails.size() != 0)
        {
            map.put(employeeDetails.size(),employeeDetails);
            return map;
        }
        return null;
    }

    public void assignStatusToEmployeeToManagerSearchList(List<EmployeesToManager> employeesToManagerList,String managerId)
    {
        for (EmployeesToManager emp:employeesToManagerList)
        {
            if (emp.getManagerId() != null && emp.getManagerId().equals(managerId))
            {
                emp.setStatus(true);
            }
        }
    }
    public List<EmployeesToManager> employeesToAssignManagerBySearchKey(String managerId,String searchKey) throws EmployeeNotExistException, ManagerNotExistException, SuperAdminIdException {
        checkEmployeeExist(managerId);
        checkManagerExist(managerId);
        isSuperAdminId(managerId);
        String query = "select employee.emp_id, emp_name, designation, managerId from employee, Manager \n" +
                "where employee.emp_id=Manager.empId and employee.emp_id<>'RT001' and employee.emp_id<>? and employee.delete_status=false and (employee.emp_id=? or employee.emp_name like ? or employee.designation like ?)";
        List<EmployeesToManager> employeeDetails =  jdbcTemplate.query(query,new BeanPropertyRowMapper<EmployeesToManager>(EmployeesToManager.class),managerId,searchKey,"%"+searchKey+"%","%"+searchKey+"%");
        assignStatusToEmployeeToManagerSearchList(employeeDetails,managerId);
        if (employeeDetails.size() != 0)
        {
            return employeeDetails;
        }
        return null;
    }

    public void checkManagerValid(String managerId) throws EmployeeNotExistException, ManagerNotExistException, SuperAdminIdException {
        checkEmployeeExist(managerId);
        checkManagerExist(managerId);
        isSuperAdminId(managerId);
    }
    public void checkValidityOfEmployeeList(String empId, String managerId) throws SuperAdminIdException, EmployeeNotExistException, ManagerEmployeeSameException {
        isSuperAdminId(empId);
        checkEmployeeExist(empId);
        checkManagerIdAndEmployeeIdSame(empId,managerId);
    }

    public void checkValidityOfEmployeesList(ManagerEmployees managerEmployees) throws SuperAdminIdException, EmployeeNotExistException, ManagerEmployeeSameException {
        for (String emp: managerEmployees.getEmpId())
        {
            checkValidityOfEmployeeList(emp,managerEmployees.getManagerId());
        }
    }

    //can't do anything if emplist contain super admin empId
    public void assignEmployeesToManager(ManagerEmployees managerEmployees) throws ManagerNotExistException, EmployeeNotExistException, ManagerEmployeeSameException, SuperAdminIdException
    {
        String managerId=managerEmployees.getManagerId();
        checkManagerValid(managerId);
        checkValidityOfEmployeesList(managerEmployees);
        if (managerEmployees.getEmpId()==null || managerEmployees.getEmpId().size()==0)
        {
            throw new EmployeeNotExistException("Employee Id list is empty");
        }
        for (String empId:managerEmployees.getEmpId())
        {
            updateEmployeesForManger(empId,managerId);
        }
    }

    public void updateEmployeesForManger(String empId,String managerId) throws EmployeeNotExistException, ManagerEmployeeSameException, SuperAdminIdException
    {
        String query="update Manager set managerId=? where empId=?";
        jdbcTemplate.update(query,managerId,empId);
    }

    public void checkManagerExist(String managerId) throws ManagerNotExistException
    {
        String query="select emp_id from employee_role where emp_id=? and role_name='manager'";
        try
        {
            jdbcTemplate.queryForObject(query,String.class,managerId);

        } catch (DataAccessException e) {

            throw new ManagerNotExistException("ManagerId "+managerId+" Does Not Exist");
        }
    }

    public void checkEmployeeExist(String empId) throws EmployeeNotExistException
    {
        String query="select emp_id from employee where emp_id=? and delete_status=0 ";
        try
        {
            String str=jdbcTemplate.queryForObject(query,String.class,empId);

        } catch (DataAccessException e) {

            throw new EmployeeNotExistException("Employee "+empId+" Does Not Exist");
        }
    }

    public void checkManagerIdAndEmployeeIdSame(String empId,String managerId) throws ManagerEmployeeSameException
    {
        if(empId.equalsIgnoreCase(managerId))
        {
            throw new ManagerEmployeeSameException("manager can't report to himself, remove managerId from empId List");
        }
    }

    public void isSuperAdminId(String empId) throws SuperAdminIdException
    {
        if(empId.equalsIgnoreCase("RT001"))
        {
            throw new SuperAdminIdException("can't give super admin as employee");
        }
    }

    public void  deleteCourse(int courseId) throws CourseDeletionException
    {
        isCourseExist(courseId,false);
        String query="update Course set deleteStatus=1 where courseId=?";
        jdbcTemplate.update(query,courseId);
        deleteFromInvitesOnCourseDelete(courseId);
        deleteFromAcceptedInvitesOnCourseDelete(courseId);
        deleteFromManagersCoursesOnCourseDelete(courseId);
    }

    public void deleteFromInvitesOnCourseDelete(int courseId)
    {
        String query = "delete from Invites where courseId=?";
        jdbcTemplate.update(query,courseId);
    }

    public void deleteFromAcceptedInvitesOnCourseDelete(int courseId)
    {
        String query = "delete from AcceptedInvites where courseId=?";
        jdbcTemplate.update(query,courseId);
    }

    public void deleteFromManagersCoursesOnCourseDelete(int courseId)
    {
        String query = "delete from ManagersCourses where courseId=?";
        jdbcTemplate.update(query,courseId);
    }

    public void isCourseExist(int courseId,boolean deleteStatus) throws CourseDeletionException
    {
        String query="select courseId from Course where courseId=? and deleteStatus=? and completionStatus='upcoming'";
        try
        {
            jdbcTemplate.queryForObject(query, Integer.class,courseId,deleteStatus);
        }
        catch (Exception e) {
            throw new CourseDeletionException("Course does not exist or course is active/completed");
        }
    }

    public String checkCourseStatus(int courseId,boolean deleteStatus)
    {
        String query1="select courseId from Course where courseId=? and deleteStatus=? and completionStatus='upcoming'";
        String query2="select courseId from Course where courseId=? and deleteStatus=? and completionStatus='active'";
        try
        {
            jdbcTemplate.queryForObject(query1, Integer.class,courseId,deleteStatus);
            return "upcoming";
        }
        catch (DataAccessException e) {

        }
        try
        {
            jdbcTemplate.queryForObject(query2, Integer.class,courseId,deleteStatus);
            return "active";
        }
        catch (Exception e) {
            return "completed";
        }
    }

    //to delete reportees
    public void checkManagerValidity(String managerId) throws EmployeeNotExistException, ManagerNotExistException, SuperAdminIdException {
        checkEmployeeExist(managerId);
        checkManagerExist(managerId);
        isSuperAdminId(managerId);
    }
    public void checkEmployeeUnderManager(ManagerEmployees managerEmployees) throws EmployeeNotUnderManagerException
    {
        for (String emp:managerEmployees.getEmpId())
        {
            fetchEmployeeForManager(emp,managerEmployees.getManagerId());
        }
    }
    public void fetchEmployeeForManager(String empId, String managerId) throws EmployeeNotUnderManagerException
    {
        String query = "select empId from Manager,employee where employee.emp_id = Manager.empId and Manager.managerId=? and employee.emp_id=? and employee.delete_status=0";
        try
        {
            jdbcTemplate.queryForObject(query, String.class,managerId,empId);
        }
        catch (DataAccessException e) {
            throw new EmployeeNotUnderManagerException("Employee not under manager");
        }
    }

    public void removeEmployeeFromManger(ManagerEmployees managerEmployees)
    {
        for (String emp: managerEmployees.getEmpId())
        {
            deleteReportees(emp,managerEmployees.getManagerId());
        }
    }
    public void deleteReportees(String empId,String managerId)
    {
        String query="update Manager set managerId=null where empId=? and managerId=?";
        try
        {
            jdbcTemplate.update(query,empId,managerId);
        } catch (Exception e) {

        }
    }

    public void removeReportee(ManagerEmployees managerEmployees) throws ManagerNotExistException, EmployeeNotExistException, SuperAdminIdException, ManagerEmployeeSameException, EmployeeNotUnderManagerException {
        //check managervalidity
        checkManagerValidity(managerEmployees.getManagerId());
        //check employeeListVAlid
        checkValidityOfEmployeesList(managerEmployees);
        //check employees exist under manager
        checkEmployeeUnderManager(managerEmployees);
        //then set empId =null
        removeEmployeeFromManger(managerEmployees);
    }
}
