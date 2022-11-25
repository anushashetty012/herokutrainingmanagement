package trainingmanagement.TrainingManagement.customException;

public class InvalidEmployeeDetailsException extends Exception
{
    public InvalidEmployeeDetailsException(String msg)
    {
        super(msg);
    }
}
