package trainingmanagement.TrainingManagement.request;

import lombok.Data;

import java.sql.Date;


@Data
public class FilterByDate
{
    String completionStatus;
    Date downDate;
    Date topDate;
}
