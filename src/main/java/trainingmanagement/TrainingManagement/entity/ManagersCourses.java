package trainingmanagement.TrainingManagement.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ManagersCourses
{
    private String managerId;
    private int courseId;

    public ManagersCourses(int courseId)
    {
        this.courseId = courseId;
    }
    public ManagersCourses(String managerId)
    {
        this.managerId = managerId;
    }
}
