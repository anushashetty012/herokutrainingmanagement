package trainingmanagement.TrainingManagement.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class EmployeeDetails
{
    private String empId;
    private String empName;
    private String designation;
    private Integer attendedCount;
    private Integer upcomingCount;
    private Integer activeCount;
    private String role;

    public EmployeeDetails(String empId, String empName, String designation, String role) {
        this.empId = empId;
        this.empName = empName;
        this.designation = designation;
        this.role = role;
    }
}
