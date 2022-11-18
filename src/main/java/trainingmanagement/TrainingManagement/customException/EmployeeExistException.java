package trainingmanagement.TrainingManagement.customException;

public class EmployeeExistException  extends Exception
{
   public EmployeeExistException(String msg)
   {
       super(msg);
   }
}
