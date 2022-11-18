package trainingmanagement.TrainingManagement.customException;

public class ManagerNotExistException extends Exception
{
    public ManagerNotExistException(String msg)
    {
        super(msg);
    }
}
