package trainingmanagement.TrainingManagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.util.Set;

@Entity
@NoArgsConstructor
public class Employee
{

    @Id
    private String empId;
    private String empName;
    private String password;
    private String designation;
    private String email;
    private String profilePic;
    @JsonIgnore
    private Boolean deleteStatus = false;


    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "EMPLOYEE_ROLE",
        joinColumns = {
            @JoinColumn(name = "EMP_ID")

        },
            inverseJoinColumns = {
                @JoinColumn(name = "ROLE_NAME")
            }
    )
    private Set<Roles> roles;

    public String getEmpId()
    {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public Set<Roles> getRoles() {
        return roles;
    }

    public void setRoles(Set<Roles> roles) {
        this.roles = roles;
    }

    public Boolean getDeleteStatus() {
        return deleteStatus;
    }

    public void setDeleteStatus(Boolean deleteStatus) {
        this.deleteStatus = deleteStatus;
    }

    public Employee(String empId)
    {
        this.empId = empId;
    }
}
