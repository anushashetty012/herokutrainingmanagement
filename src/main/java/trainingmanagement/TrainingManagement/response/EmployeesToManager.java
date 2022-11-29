package trainingmanagement.TrainingManagement.response;

import lombok.Data;

@Data
public class EmployeesToManager
{
    private String empId;
    private String empName;
    private String designation;
    private String managerId;
}
