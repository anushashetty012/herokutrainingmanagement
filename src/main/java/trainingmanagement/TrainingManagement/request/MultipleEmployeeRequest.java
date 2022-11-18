package trainingmanagement.TrainingManagement.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MultipleEmployeeRequest
{
    String empId;
    public MultipleEmployeeRequest(String empId)
    {
        this.empId = empId;
    }
}
