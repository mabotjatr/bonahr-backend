package za.co.bonalabs.bonahr.dto.leave;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeLeaveAccountResponse(
        UUID id,
        UUID employeeId,
        String employeeNumber,
        String employeeName,
        UUID leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        UUID leavePolicyId,
        BigDecimal currentBalance,
        BigDecimal reservedDays,
        LocalDate lastAccrualDate
) {
}