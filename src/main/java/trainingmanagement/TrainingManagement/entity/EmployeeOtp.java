package trainingmanagement.TrainingManagement.entity;

import lombok.Data;

@Data
public class EmployeeOtp
{
    private String empId;
    private String is2faEnabled;
    private String twoFaCode;
    private String twoFaExpireTime;
    private String twoFaDefaultType;
}
