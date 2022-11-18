package trainingmanagement.TrainingManagement.customException;

public class EmployeeNotExistException extends Exception
{
    public EmployeeNotExistException(String msg)
    {
        super(msg);
    }
}
