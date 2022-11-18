package trainingmanagement.TrainingManagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.sql.Date;
import java.sql.Time;
@Setter
@Getter
@Data
@NoArgsConstructor
public class Course
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
    private String meetingInfo;
    @JsonIgnore
    private boolean deleteStatus;
}
