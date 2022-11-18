package trainingmanagement.TrainingManagement.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Invites
{
    private int inviteId;
    private String empId;
    private int courseId;
    private boolean acceptanceStatus;
    private boolean notificationSentStatus;

    public Invites(int courseId)
    {
        this.courseId = courseId;
    }
}
