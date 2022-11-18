package trainingmanagement.TrainingManagement.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AcceptedOrRejectedResponse
{
    private int inviteId;
    private String empId;
    private int courseId;
}
