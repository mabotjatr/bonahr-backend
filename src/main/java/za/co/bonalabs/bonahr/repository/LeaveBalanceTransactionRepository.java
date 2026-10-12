package za.co.bonalabs.bonahr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.bonalabs.bonahr.entity.LeaveBalanceTransaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveBalanceTransactionRepository extends JpaRepository<LeaveBalanceTransaction, UUID> {

    Optional<LeaveBalanceTransaction> findByIdAndOrganisationId(UUID id, UUID organisationId);

    List<LeaveBalanceTransaction> findAllByOrganisationIdAndLeaveAccountIdOrderByCreatedAtAsc(UUID organisationId, UUID leaveAccountId);
}