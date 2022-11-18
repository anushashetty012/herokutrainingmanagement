package trainingmanagement.TrainingManagement.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import trainingmanagement.TrainingManagement.entity.Roles;

@Repository
public interface RoleDao extends CrudRepository<Roles, String> {
}
