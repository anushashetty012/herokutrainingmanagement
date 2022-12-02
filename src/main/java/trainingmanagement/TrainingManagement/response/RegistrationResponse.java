package trainingmanagement.TrainingManagement.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistrationResponse
{
    private String reason;
    private String empId;
}
