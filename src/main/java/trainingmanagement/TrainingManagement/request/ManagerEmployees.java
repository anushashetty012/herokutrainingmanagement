package trainingmanagement.TrainingManagement.request;

import lombok.Data;

import java.util.List;

@Data
public class ManagerEmployees
{
    private String managerId;
    private List<String> empId;
}
