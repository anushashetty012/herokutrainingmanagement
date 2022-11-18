package trainingmanagement.TrainingManagement.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeInvite
{
    private String empId;
    private String empName;
    private String designation;
    private boolean invited=false;
}
