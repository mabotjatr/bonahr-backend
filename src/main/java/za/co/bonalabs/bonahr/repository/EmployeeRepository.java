package za.co.bonalabs.bonahr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<Employee> findAllByOrganisationIdAndDepartmentIgnoreCaseOrderByLastNameAscFirstNameAsc(UUID organisationId, String department);

    @Query("""
        select e
        from Employee e
        where e.organisation.id = :organisationId
          and (
                :search = ''
                or lower(e.firstName) like concat('%', :search, '%')
                or lower(e.lastName) like concat('%', :search, '%')
          )
          and (
                :status is null
                or e.status = :status
          )
          and (
                :department = ''
                or lower(e.department) = :department
          )
        order by e.lastName asc, e.firstName asc
        """)
    List<Employee> findAllByFilters(
            @Param("organisationId") UUID organisationId,
            @Param("search") String search,
            @Param("status") EmployeeStatus status,
            @Param("department") String department
    );
}