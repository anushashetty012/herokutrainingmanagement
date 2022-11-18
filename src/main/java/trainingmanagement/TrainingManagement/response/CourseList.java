package trainingmanagement.TrainingManagement.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Date;
import java.sql.Time;

@Data
@AllArgsConstructor
public class CourseList
{
    private int courseId;
    private String courseName;
    private String trainer;
    private String trainingMode;
    private Date startDate;
    private Date endDate;
    private Time duration;
    private Time startTime;
    private Time endTime;
    private String completionStatus;
}
