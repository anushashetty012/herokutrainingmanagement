package trainingmanagement.TrainingManagement.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendeesNonAttendeesCount
{
    private int attendees;
    private int nonAttendees;
}
