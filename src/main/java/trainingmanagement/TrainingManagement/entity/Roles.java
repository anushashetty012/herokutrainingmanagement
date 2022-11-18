package trainingmanagement.TrainingManagement.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Roles
{
    @Id
    private String roleName;

    public String getRoleName()
    {
        return roleName;
    }

    public void setRoleName(String roleName)
    {
        this.roleName = roleName;
    }
}
