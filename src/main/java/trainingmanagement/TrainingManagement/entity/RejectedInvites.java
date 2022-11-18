package trainingmanagement.TrainingManagement.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RejectedInvites
{
    private String inviteId;
    private int courseId;
    private String empId;
    private String reason;
}
