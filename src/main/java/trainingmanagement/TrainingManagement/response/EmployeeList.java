package trainingmanagement.TrainingManagement.response;

import lombok.Data;

@Data
public class EmployeeList
{
    private String empId;
    private int attendedCount;
    private int upcomingCount;
    private int activeCount;
}