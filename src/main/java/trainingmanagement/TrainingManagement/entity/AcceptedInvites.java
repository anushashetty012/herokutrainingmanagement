package trainingmanagement.TrainingManagement.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class AcceptedInvites
{
    private String inviteId;
    private int courseId;
    private String empId;
    private boolean deleteStatus;
}
