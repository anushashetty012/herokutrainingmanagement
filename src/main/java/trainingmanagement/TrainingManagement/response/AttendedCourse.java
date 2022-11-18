package trainingmanagement.TrainingManagement.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Date;

@Data
@AllArgsConstructor
public class AttendedCourse
{
    private int courseId;
    private String courseName;
    private String trainer;
    private String trainingMode;
    private Date startDate;
    private Date endDate;
}
