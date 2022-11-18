package trainingmanagement.TrainingManagement.dao;


import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import trainingmanagement.TrainingManagement.entity.Employee;

@Repository
public interface EmployeeDao extends CrudRepository<Employee, String> {
}
