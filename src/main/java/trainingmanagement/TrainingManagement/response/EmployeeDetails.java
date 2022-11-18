package trainingmanagement.TrainingManagement.response;

import lombok.Data;

@Data
public class EmployeeDetails
{
    private String empId;
    private String empName;
    private String designation;
    private Integer attendedCount;
    private Integer upcomingCount;
    private Integer activeCount;
}
