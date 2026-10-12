package za.co.bonalabs.bonahr.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.LeaveAccrualMethod;
import za.co.bonalabs.bonahr.entity.LeavePolicy;
import za.co.bonalabs.bonahr.entity.LeaveType;
import za.co.bonalabs.bonahr.entity.Organisation;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class LeavePolicyRepositoryTest {

    @Autowired
    private LeavePolicyRepository leavePolicyRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Test
    void shouldPersistAndFindLeavePolicyWithinOrganisation() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Policy Repository Company " + UUID.randomUUID()));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy policy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000")));

        LeavePolicy found = leavePolicyRepository.findByIdAndOrganisationId(policy.getId(), organisation.getId()).orElseThrow();

        assertEquals(policy.getId(), found.getId());

        assertEquals(leaveType.getId(), found.getLeaveType().getId());

        assertEquals(LeaveAccrualMethod.MONTHLY, found.getAccrualMethod());
    }

    @Test
    void shouldNotFindLeavePolicyFromDifferentOrganisation() {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Leave Policy Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Leave Policy Tenant B " + UUID.randomUUID()));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy policy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisationA, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000")));

        var result = leavePolicyRepository.findByIdAndOrganisationId(policy.getId(), organisationB.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindLeavePolicyByOrganisationAndLeaveType() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Policy Lookup Company " + UUID.randomUUID()));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy policy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000")));

        LeavePolicy found = leavePolicyRepository.findByOrganisationIdAndLeaveTypeId(organisation.getId(), leaveType.getId()).orElseThrow();

        assertEquals(policy.getId(), found.getId());
    }
}