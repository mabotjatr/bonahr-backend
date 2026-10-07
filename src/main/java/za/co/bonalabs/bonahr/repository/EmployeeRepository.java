package za.co.bonalabs.bonahr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.bonalabs.bonahr.entity.Employee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByIdAndOrganisationId(UUID id, UUID organisationId);

    boolean existsByOrganisationIdAndEmployeeNumber(UUID organisationId, String employeeNumber);

    boolean existsByOrganisationIdAndEmailIgnoreCase(UUID organisationId, String email);

    List<Employee> findAllByOrganisationIdOrderByLastNameAscFirstNameAsc(UUID organisationId);
}