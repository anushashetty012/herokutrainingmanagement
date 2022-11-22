package trainingmanagement.TrainingManagement.customException;

public class EmployeeNotUnderManagerException extends Exception
{
    public EmployeeNotUnderManagerException(String msg)
    {
        super(msg);
    }
}
