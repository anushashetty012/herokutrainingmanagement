package trainingmanagement.TrainingManagement.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Notification
{
    private Integer inviteId;
    private String empId;
    private Integer courseId;
    private String courseName;
}
