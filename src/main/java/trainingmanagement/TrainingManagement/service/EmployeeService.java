package trainingmanagement.TrainingManagement.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import trainingmanagement.TrainingManagement.entity.Course;
import trainingmanagement.TrainingManagement.entity.Invites;
import trainingmanagement.TrainingManagement.entity.ManagersCourses;
import trainingmanagement.TrainingManagement.request.FilterByDate;
import trainingmanagement.TrainingManagement.response.*;
import java.util.*;


@Service
public class EmployeeService
{
    @Autowired
    JdbcTemplate jdbcTemplate;
    private String GET_ACCEPTED_COUNT = "SELECT COUNT(empId) FROM AcceptedInvites,Course WHERE AcceptedInvites.courseId=Course.courseId and AcceptedInvites.courseId=? and AcceptedInvites.deleteStatus=false and Course.deleteStatus=false";
    //for admin
    private String VIEW_COURSE_DETAILS = "SELECT courseId,courseName,trainer,trainingMode,startDate,endDate,duration,startTime,endTime,completionStatus,meetingInfo FROM Course WHERE courseId=? and deleteStatus=false";
    //for manager
    private String CHECK_COURSE_ALLOCATION = "SELECT courseId FROM ManagersCourses WHERE managerId=? AND courseId=?";
    private String CHECK_IF_ACCEPTED = "SELECT courseId FROM Invites where empId=? AND courseId=? and (acceptanceStatus = true or acceptanceStatus is null);";
    private String GET_ROLE = "SELECT role_name FROM employee_role WHERE emp_id=?";
    int offset=0;

    //to get role
    public String getRole(String empId)
    {
        return jdbcTemplate.queryForObject(GET_ROLE,new Object[]{empId},String.class);
    }

    public int getAcceptedCount(int courseId,String empId)
    {
        if(getRole((empId)).equalsIgnoreCase("admin")|| getRole((empId)).equalsIgnoreCase("super_admin"))
        {
            return jdbcTemplate.queryForObject(GET_ACCEPTED_COUNT, new Object[]{courseId},Integer.class);
        }
        if (getRole((empId)).equalsIgnoreCase("manager"))
        {

            List<Invites> isInvited = jdbcTemplate.query(CHECK_IF_ACCEPTED,(rs, rowNum) -> {
                return new Invites(rs.getInt("courseId"));
            },empId,courseId);
            List<ManagersCourses> isCourseAssigned = jdbcTemplate.query(CHECK_COURSE_ALLOCATION,(rs, rowNum) -> {
                return new ManagersCourses(rs.getInt("courseId"));
            },empId,courseId);
            if (isCourseAssigned.size()!=0 || isInvited.size()!=0)
            {
                return jdbcTemplate.queryForObject(GET_ACCEPTED_COUNT, new Object[]{courseId},Integer.class);
            }
        }
        if (getRole((empId)).equalsIgnoreCase("employee"))
        {
            List<Invites> isInvited = jdbcTemplate.query(CHECK_IF_ACCEPTED,(rs, rowNum) -> {
                return new Invites(rs.getInt("courseId"));
            },empId,courseId);
            if (isInvited.size()!=0)
            {
                return jdbcTemplate.queryForObject(GET_ACCEPTED_COUNT, new Object[]{courseId},Integer.class);
            }
        }
        return -1;
    }

    public CourseInfo viewCourseDetails(int courseId, String empId)
    {
        if(getRole((empId)).equalsIgnoreCase("admin") || getRole((empId)).equalsIgnoreCase("super_admin"))
        {
            try
            {
                return jdbcTemplate.queryForObject(VIEW_COURSE_DETAILS,(rs, rowNum) -> {
                    return new CourseInfo(rs.getInt("courseId"),rs.getString("courseName"),rs.getString("trainer"),rs.getString("trainingMode"),rs.getDate("startDate"),rs.getDate("endDate"),rs.getTime("duration"),rs.getTime("startTime"),rs.getTime("endTime"),rs.getString("completionStatus"),rs.getString("meetingInfo"));
                },courseId);
            }
            catch (DataAccessException e)
            {
                return null;
            }
        }
        if (getRole((empId)).equalsIgnoreCase("manager"))
        {
            List<Invites> isInvited = jdbcTemplate.query(CHECK_IF_ACCEPTED,(rs, rowNum) -> {
                return new Invites(rs.getInt("courseId"));
            },empId,courseId);

            List<ManagersCourses> isCourseAssigned = jdbcTemplate.query(CHECK_COURSE_ALLOCATION,(rs, rowNum) -> {
                return new ManagersCourses(rs.getInt("courseId"));
            },empId,courseId);
            if (isCourseAssigned.size()!=0 || isInvited.size()!=0)
            {
                return jdbcTemplate.queryForObject(VIEW_COURSE_DETAILS,(rs, rowNum) -> {
                    return new CourseInfo(rs.getInt("courseId"),rs.getString("courseName"),rs.getString("trainer"),rs.getString("trainingMode"),rs.getDate("startDate"),rs.getDate("endDate"),rs.getTime("duration"),rs.getTime("startTime"),rs.getTime("endTime"),rs.getString("completionStatus"),rs.getString("meetingInfo"));
                },courseId);
            }
        }
        if (getRole((empId)).equalsIgnoreCase("employee"))
        {
            try
            {
                List<Invites> isInvited = jdbcTemplate.query(CHECK_IF_ACCEPTED,(rs, rowNum) -> {
                    return new Invites(rs.getInt("courseId"));
                },empId,courseId);
                if (isInvited.size()!=0)
                {
                    return jdbcTemplate.queryForObject(VIEW_COURSE_DETAILS,(rs, rowNum) -> {
                        return new CourseInfo(rs.getInt("courseId"),rs.getString("courseName"),rs.getString("trainer"),rs.getString("trainingMode"),rs.getDate("startDate"),rs.getDate("endDate"),rs.getTime("duration"),rs.getTime("startTime"),rs.getTime("endTime"),rs.getString("completionStatus"),rs.getString("meetingInfo"));
                    },courseId);
                }
            }
            catch (DataAccessException e)
            {
                return null;
            }
        }
        return null;
    }

    public void isInviteValid(int inviteId) throws Exception
    {
        String query1 = "select courseId from Invites where inviteId=? and acceptanceStatus is null";
        String query2 = "select courseId from Course where courseId=? and (completionStatus='upcoming' or (completionStatus='active' and trainingMode='Online sessions'))";
        try {
           Integer id = jdbcTemplate.queryForObject(query1,Integer.class,inviteId);
           jdbcTemplate.queryForObject(query2, Integer.class,id);
        } catch (DataAccessException e) {
            throw new Exception("Invite is not found or Course is not valid anymore");
        }
    }

    public void removeFromRejectedAndInvites(int inviteId)
    {
        InviteDetail inviteDetail = getInviteDetail(inviteId);
        String query = "delete from RejectedInvites where courseId=? and empId=?";
        String query1 = "delete from Invites where courseId=? and empId=? and acceptanceStatus=0";
        jdbcTemplate.update(query,inviteDetail.getCourseId(),inviteDetail.getEmpId());
        jdbcTemplate.update(query1,inviteDetail.getCourseId(),inviteDetail.getEmpId());
    }
    public InviteDetail getInviteDetail(int inviteId)
    {
        String query = "select courseId,empId from Invites where inviteId=?";
        return jdbcTemplate.query(query,new BeanPropertyRowMapper<>(InviteDetail.class),inviteId).get(0);
    }

    public String acceptInvite(int inviteId)
    {
        try
        {
            isInviteValid(inviteId);
            jdbcTemplate.update("update Invites set acceptanceStatus=true where inviteId=?",inviteId);
            AcceptedOrRejectedResponse invite = jdbcTemplate.queryForObject("select inviteId,empId,courseId from Invites where inviteId=?",(rs, rowNum) -> {
                return new AcceptedOrRejectedResponse(rs.getInt("inviteId"),rs.getString("empId"),rs.getInt("courseId"));
            },inviteId);
            jdbcTemplate.update("insert into AcceptedInvites(inviteId,courseId,empId) values(?,?,?)",invite.getInviteId(),invite.getCourseId(),invite.getEmpId());
        }
        catch (Exception e)
        {
            return null;
        }
        removeFromRejectedAndInvites(inviteId);
        return "Accepted invite successfully";
    }

    public String rejectInvite(int inviteId, RejectedResponse reason)
    {
        try
        {
            isInviteValid(inviteId);
            jdbcTemplate.update("update Invites set acceptanceStatus=false where inviteId=?",inviteId);
            AcceptedOrRejectedResponse invite = jdbcTemplate.queryForObject("select inviteId,empId,courseId from Invites where inviteId=?",(rs, rowNum) -> {
                return new AcceptedOrRejectedResponse(rs.getInt("inviteId"),rs.getString("empId"),rs.getInt("courseId"));
            },inviteId);
            jdbcTemplate.update("insert into RejectedInvites(inviteId,courseId,empId,reason) values(?,?,?,?)",invite.getInviteId(),invite.getCourseId(),invite.getEmpId(),reason.getReason());
        }
        catch (Exception e)
        {
            return null;
        }
        return "Rejected invite successfully";
    }

    //attended and non attended course
    //profile info
    public EmployeeProfile profileInfo(String empId)
    {
        try
        {
            return jdbcTemplate.queryForObject("select emp_id,emp_name,designation,profile_pic from employee where emp_id=? and delete_status=false",(rs, rowNum) -> {
                return new EmployeeProfile(rs.getString("emp_id"),rs.getString("emp_name"),rs.getString("designation"),rs.getString("profile_pic"));
            },empId);
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }

    //attended course
    public Map<Integer,List<AttendedCourse>> attendedCourse(String empId,int page, int limit)
    {
        try
        {
            Map map = new HashMap<Integer,List>();
            offset = limit *(page-1);
            List<AttendedCourse> attendedCourseList = jdbcTemplate.query("select Course.courseId,courseName,trainer,trainingMode,startDate,endDate from Course,AcceptedInvites where Course.courseId = AcceptedInvites.courseId and Course.completionStatus='completed' and AcceptedInvites.deleteStatus=false and Course.deleteStatus=false and AcceptedInvites.empId=? limit ?,?",(rs, rowNum) -> {
                return new AttendedCourse(rs.getInt("courseId"),rs.getString("courseName"),rs.getString("trainer"),rs.getString("trainingMode"),rs.getDate("startDate"),rs.getDate("endDate"));
            },empId,offset,limit);
            if (attendedCourseList.size()!=0)
            {
                map.put(attendedCourseList.size(),attendedCourseList);
                return map;
            }
        }
        catch (DataAccessException e)
        {
            return null;
        }
        return null;
    }

    public Map<Integer,List<NonAttendedCourse>> nonAttendedCourse(String empId, int page, int limit)
    {
        try
        {
            Map map = new HashMap<Integer,List>();
            offset = limit *(page-1);
            List<NonAttendedCourse> nonAttendedCourseList = jdbcTemplate.query("select Course.courseId,courseName,trainer,trainingMode,startDate,endDate,reason from Course,RejectedInvites where Course.courseId = RejectedInvites.courseId and Course.deleteStatus=false and Course.completionStatus='completed' and RejectedInvites.empId=? limit ?,?",(rs, rowNum) -> {
                return new NonAttendedCourse(rs.getInt("courseId"),rs.getString("courseName"),rs.getString("trainer"),rs.getString("trainingMode"),rs.getDate("startDate"),rs.getDate("endDate"),rs.getString("reason"));
            },empId,offset,limit);
            if (nonAttendedCourseList.size()!=0)
            {
                map.put(nonAttendedCourseList.size(),nonAttendedCourseList);
                return map;
            }
        }
        catch (DataAccessException e)
        {
            return null;
        }
        return null;
    }

    //Filter Accepted invites by Completed status based on date
    public Map<Integer,List<Course>> filterCourse(FilterByDate filter, String empId,int page, int limit)
    {
        Map map = new HashMap<Integer,List>();
        offset = limit *(page-1);
        if(filter.getCompletionStatus().matches("active|upcoming")){
            List<Course> filteredCourses = filterCoursesForEmployeeByActiveOrUpcomingStatus(filter,empId,offset,limit);
            if (filteredCourses.size()!=0)
            {
                map.put(filteredCourses.size(),filteredCourses);
                return map;
            }
        }
        List<Course> filteredCourses = filterCoursesForEmployeeByCompletedStatus(filter,empId,offset,limit);
        if (filteredCourses.size()!=0)
        {
            map.put(filteredCourses.size(),filteredCourses);
            return map;
        }
        return null;
    }

    //Filter for Active and Upcoming Courses from Accepted Invites by Employee by date and status
    public List<Course> filterCoursesForEmployeeByActiveOrUpcomingStatus(FilterByDate filter, String empId,int offset,int limit)
    {
        String query = "SELECT Course.courseId,courseName,trainer,trainingMode,startDate,endDate,duration,startTime,endTime,completionStatus FROM Course, AcceptedInvites WHERE Course.courseId = AcceptedInvites.courseId AND empId = ? AND AcceptedInvites.deleteStatus = 0 AND Course.completionStatus = ? AND (startDate >= ? and startDate <= ? ) limit ?,?";
        return jdbcTemplate.query(query,new BeanPropertyRowMapper<Course>(Course.class),empId,filter.getCompletionStatus(),filter.getDownDate(),filter.getTopDate(),offset,limit);
    }

    //Filter for Completed Courses from Accepted Invites by Employee by date and status
    public List<Course> filterCoursesForEmployeeByCompletedStatus(FilterByDate filter, String empId,int offset,int limit)
    {
        String query = "SELECT Course.courseId,courseName,trainer,trainingMode,startDate,endDate,duration,startTime,endTime,completionStatus FROM Course, AcceptedInvites WHERE Course.courseId = AcceptedInvites.courseId AND empId = ? AND AcceptedInvites.deleteStatus = 0 AND Course.completionStatus = ? AND (endDate >= ? and endDate <= ? ) limit ?,?";
        return jdbcTemplate.query(query,new BeanPropertyRowMapper<Course>(Course.class),empId,filter.getCompletionStatus(),filter.getDownDate(),filter.getTopDate(),offset,limit);
    }

    //Get Count of Courses that employee has accepted the request based on completion status
    public int getCourseStatusCountForEmployee(String empId, String completionStatus)
    {
        String query = "select count(AcceptedInvites.courseId) from AcceptedInvites, Course where AcceptedInvites.courseId = Course.courseId and Course.completionStatus = ? and empId = ? and Course.deleteStatus=false and AcceptedInvites.deleteStatus = false";
        return jdbcTemplate.queryForObject(query, Integer.class,completionStatus,empId);
    }

    //to retrieve invited courses based on completion status
    public Map<Integer,List<Course>> coursesForEmployeeByCompletedStatus(String empId, String status, int page, int limit)
    {
        Map map = new HashMap<Integer,List>();
        offset = limit *(page-1);
        String query = "SELECT Course.courseId,courseName,trainer,trainingMode,startDate,endDate,duration,startTime,endTime,completionStatus FROM Course, AcceptedInvites WHERE Course.courseId = AcceptedInvites.courseId AND empId = ? AND AcceptedInvites.deleteStatus = 0 AND Course.completionStatus = ? limit ?,?";
        List<Course> courseList = jdbcTemplate.query(query,new BeanPropertyRowMapper<Course>(Course.class),empId,status,offset,limit);
        if (courseList.size()!=0)
        {
            map.put(courseList.size(),courseList);
            return map;
        }
        return null;
    }

    public Integer notificationCount(String empId)
    {
        String query = "select count(empId) from Invites where empId=? and notificationSentStatus=0 and acceptanceStatus is null";
        try {
            return jdbcTemplate.queryForObject(query, Integer.class, empId);
        }
        catch (DataAccessException e) {
            return 0;
        }
    }

    public Map<Integer,List<Notification>> invites(String empId)
    {
        String query1 = "select inviteId,courseId,empId from Invites where empId=? and acceptanceStatus is null order by inviteId desc";
        List<Notification> invites=jdbcTemplate.query(query1,new BeanPropertyRowMapper<>(Notification.class),empId);
        Map<Integer,List<Notification>> map=new HashMap<Integer,List<Notification>>();
        map.put(invites.size(),invites);
        return map;
    }

    public void clearNotification(String empId)
    {
        String query = "update Invites set clearNotificationStatus = true,notificationSentStatus=true where empId=?";
        jdbcTemplate.update(query,empId);
    }

    public Map<Integer,List<Notification>> notification(String empId)
    {
        String query1 = "select inviteId,Invites.courseId,empId,courseName from Invites,Course where Invites.courseId = Course.courseId and empId=? and\n" +
                "acceptanceStatus is null and clearNotificationStatus = false order by inviteId desc";
        List<Notification> notification=jdbcTemplate.query(query1,new BeanPropertyRowMapper<>(Notification.class),empId);
        Map<Integer,List<Notification>> map=new HashMap<Integer,List<Notification>>();
        map.put(notification.size(),notification);
        return map;
    }

    public void reduceNotificationCount(int inviteId)
    {
        String query = "update Invites set notificationSentStatus=true where inviteId = ?";
        jdbcTemplate.update(query,inviteId);
    }

    //profile photo upload
    public PhotoUploadResponse uploadProfilePhoto(MultipartFile profilePhoto,String empId)
    {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "do52xyv54",
                "api_key", "815687279696268",
                "api_secret", "EBPsxcDsTwxLZxJ6jjtUaAKv1EU",
                "secure", "true"));
        cloudinary.config.secure = true;
        try
        {
            // Upload the image
            Map params1 = ObjectUtils.asMap(
                    "use_filename", true,
                    "filename",empId,
                    "unique_filename", true,
                    "overwrite", true,
                    "folder","trainingmanagement"
            );
            Map uploadResult = cloudinary.uploader().upload(profilePhoto.getBytes(), params1);
            String url = uploadResult.get("secure_url").toString();
            int uploadStatus = jdbcTemplate.update("update employee set profile_pic=? where emp_id=?",url,empId);
            PhotoUploadResponse photoUploadResponse = new PhotoUploadResponse();
            if (uploadStatus == 1)
            {
                photoUploadResponse.setUrl(url);
                photoUploadResponse.setMessage("Profile photo uploaded successfully");
            }
            return photoUploadResponse;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public String viewProfilePhoto(String empId)
    {
        try
        {
            String profileUrl = jdbcTemplate.queryForObject("select profile_pic from employee where emp_id=?", String.class, empId);
            return profileUrl;
        }
        catch (DataAccessException e)
        {
            return "Employee id not found";
        }
    }
}
