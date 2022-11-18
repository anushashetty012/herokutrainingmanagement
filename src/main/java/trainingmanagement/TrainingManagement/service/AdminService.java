package trainingmanagement.TrainingManagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trainingmanagement.TrainingManagement.customException.*;
import trainingmanagement.TrainingManagement.entity.Course;
import trainingmanagement.TrainingManagement.entity.ManagersCourses;
import trainingmanagement.TrainingManagement.request.ManagerEmployees;
import trainingmanagement.TrainingManagement.request.MultipleEmployeeRequest;
import trainingmanagement.TrainingManagement.response.CourseList;
import trainingmanagement.TrainingManagement.response.EmployeeInfo;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AdminService
{
    private String TRAINING_COUNT = "SELECT COUNT(courseId) FROM Course WHERE completionStatus=? and deleteStatus=false";
    private String GET_COURSE = "SELECT courseId,courseName,trainer,trainingMode,startDate,endDate,duration,startTime,endTime,completionStatus FROM Course WHERE completionStatus=? and deleteStatus=false limit ?,?";
    private String CREATE_COURSE = "INSERT INTO Course(courseName,trainer,trainingMode,startDate,endDate,duration,startTime,endTime,meetingInfo,startTimestamp,endTimestamp) values(?,?,?,?,?,?,?,?,?,?,?)";

    //allocate project manager
    private String COURSES_TO_MANAGER = "SELECT courseId,courseName,trainer,trainingMode,startDate,endDate,duration,startTime,endTime,completionStatus FROM Course WHERE completionStatus='upcoming' and deleteStatus=false LIMIT ?,?";
    private String GET_MANAGERS = "SELECT Employee.emp_Id,emp_Name,designation FROM Employee, employee_role WHERE Employee.emp_id = employee_role.emp_id and employee_role.role_name='manager' AND Employee.delete_status=false LIMIT ?,?";
    private String ASSIGN_MANAGER = "INSERT INTO ManagersCourses(managerId,courseId) VALUES(?,?)";
    int offset=0;

    @Autowired
    JdbcTemplate jdbcTemplate;

    public int activeCourse()
    {
        return jdbcTemplate.queryForObject(TRAINING_COUNT, new Object[]{"active"}, Integer.class);
    }

    public int upcomingCourse()
    {
        return jdbcTemplate.queryForObject(TRAINING_COUNT, new Object[]{"upcoming"}, Integer.class);
    }
    public int completedCourse()
    {
        return jdbcTemplate.queryForObject(TRAINING_COUNT, new Object[]{"completed"}, Integer.class);
    }
    public Map<Integer,List<CourseList>> getCourse(String completionStatus, int page, int limit)
    {
        Map map = new HashMap<Integer,List>();
        offset = limit *(page-1);
        List<CourseList> courses = jdbcTemplate.query(GET_COURSE,(rs, rowNum) -> {
            return new CourseList(rs.getInt("courseId"),rs.getString("courseName"),rs.getString("trainer"),rs.getString("trainingMode"),rs.getDate("startDate"),rs.getDate("endDate"),rs.getTime("duration"),rs.getTime("startTime"),rs.getTime("endTime"),rs.getString("completionStatus"));
        },completionStatus,page,limit);
        if (courses.size()!=0)
        {
            map.put(courses.size(),courses);
            return map;
        }
        return null;
    }
    public Timestamp createTimestamp(Date date, Time time)
    {

        Timestamp t= new Timestamp(date.getYear(), date.getMonth(), date.getDate(), time.getHours(), time.getMinutes(), time.getSeconds(),0);
        return t;
    }

    public String createCourse(Course course) throws CourseInfoIntegrityException {
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

    public Map<Integer,List<EmployeeInfo>> getManagersBySearchkey(int page, int limit, String searchKey)
    {
        String GET_MANAGERS = "SELECT Employee.emp_Id,emp_Name,designation FROM Employee, employee_role WHERE Employee.emp_id = employee_role.emp_id and employee_role.role_name='manager' AND Employee.delete_status=false and (Employee.emp_id=? or Employee.emp_name like ? or Employee.designation like ?) LIMIT ?,?";
        Map map = new HashMap<Integer,List>();
        offset = limit *(page-1);
        List<EmployeeInfo> employeeDetails =  jdbcTemplate.query(GET_MANAGERS,(rs, rowNum) -> {
            return new EmployeeInfo(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"));
        },searchKey,"%"+searchKey+"%","%"+searchKey+"%",offset,limit);
        if (employeeDetails.size() != 0)
        {
            map.put(employeeDetails.size(),employeeDetails);
            return map;
        }
        return null;
    }

    public String assignCourseToManager(int courseId, List<MultipleEmployeeRequest> courseToManager)
    {
        int count=0;
        int noOfManagers = courseToManager.size();
        for (int i = 0; i < noOfManagers; i++)
        {
            String query = "select managerId from ManagersCourses where managerId=? and courseId=?";
            List<ManagersCourses> managerId = jdbcTemplate.query(query, (rs, rowNum) -> {
                return new ManagersCourses(rs.getString("managerId"));
            }, courseToManager.get(i).getEmpId(), courseId);
            if (managerId.size() == 0)
            {
                jdbcTemplate.update(ASSIGN_MANAGER, new Object[]{courseToManager.get(i).getEmpId(), courseId});
            }
            else
            {
                count++;
            }
            if (count==noOfManagers)
            {
                return "This course is already allocated to this manager";
            }
        }
        return "Course allocated successfully";
    }

    //Update Existing Course
    public int updateCourse(Course course) throws CourseInfoIntegrityException
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
        String query = "update course set courseName =?, trainer=?, trainingMode=?, startDate=?, endDate =?, duration=?, startTime =?, endTime =?, meetingInfo=?,startTimestamp=?,endTimestamp=? where courseId = ? and deleteStatus=0";
        return jdbcTemplate.update(query, course.getCourseName(),course.getTrainer(),course.getTrainingMode(),course.getStartDate(),course.getEndDate(),course.getDuration(),course.getStartTime(),course.getEndTime(),course.getMeetingInfo(),startTimestamp,endTimestamp,course.getCourseId());
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
                throw new CourseInfoIntegrityException("start time should be smaller end time");
            }
        }
    }
    public void courseInfoIntegrity(Course course) throws CourseInfoIntegrityException
    {
        if (course.getCourseName().isEmpty())
        {
            throw new CourseInfoIntegrityException("CourseName can't be empty");
        }
        courseModeIntegrity(course.getTrainingMode());
        long millis=System.currentTimeMillis();
        java.sql.Date date=new java.sql.Date(millis);
        String str= date.toString();

        if(0>course.getStartDate().compareTo(Date.valueOf(str)))
        {
            throw new CourseInfoIntegrityException("start date can't be before  current date");
        }
        try {
            int i=course.getStartDate().compareTo(course.getEndDate());
            if(i>0)
            {
                throw new CourseInfoIntegrityException("end date cant be before start date");
            }
            checkStartTimeExist(course);
            checkEndTimeExist(course);
            checkTime(course);
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
    //can't do anything if emplist contain super admin empId
    public void assignEmployeesToManager(ManagerEmployees managerEmployees) throws ManagerNotExistException, EmployeeNotExistException, ManagerEmployeeSameException, SuperAdminIdException
    {

        String managerId=managerEmployees.getManagerId();
        checkManagerExist(managerId);
        isSuperAdminId(managerId);
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

        isSuperAdminId(empId);
        checkEmployeeExist(empId);
        checkManagerIdAndEmployeeIdSame(empId,managerId);
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

            throw new ManagerNotExistException("ManagerId Does Not Exist");
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
        String query="update course set deleteStatus=1 where courseId=?";
        jdbcTemplate.update(query,courseId);
    }

    public void isCourseExist(int courseId,boolean deleteStatus) throws CourseDeletionException
    {
        String query="select courseId from course where courseId=? and deleteStatus=? and completionStatus='upcoming'";
        try
        {
            jdbcTemplate.queryForObject(query, Integer.class,courseId,deleteStatus);
        } catch (Exception e) {
            throw new CourseDeletionException("Course does not exist or course is active/completed");
        }
    }
}
