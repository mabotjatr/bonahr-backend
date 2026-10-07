package za.co.bonalabs.bonahr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.EmployeeStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByIdAndOrganisationId(UUID id, UUID organisationId);

    boolean existsByOrganisationIdAndEmployeeNumber(UUID organisationId, String employeeNumber);

    boolean existsByOrganisationIdAndEmailIgnoreCase(UUID organisationId, String email);

    List<Employee> findAllByOrganisationIdOrderByLastNameAscFirstNameAsc(UUID organisationId);

    List<Employee> findAllByOrganisationIdAndFirstNameContainingIgnoreCaseOrOrganisationIdAndLastNameContainingIgnoreCaseOrderByLastNameAscFirstNameAsc(
            UUID organisationIdForFirstName, String firstName, UUID organisationIdForLastName, String lastName);

    List<Employee> findAllByOrganisationIdAndStatusOrderByLastNameAscFirstNameAsc(UUID organisationId, EmployeeStatus status);
}