package trainingmanagement.TrainingManagement.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeInfo
{
    private String empId;
    private String empName;
    private String designation;
}
