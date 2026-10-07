package za.co.bonalabs.bonahr.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.co.bonalabs.bonahr.dto.employee.CreateEmployeeRequest;
import za.co.bonalabs.bonahr.dto.employee.EmployeeMapper;
import za.co.bonalabs.bonahr.dto.employee.EmployeeResponse;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.security.JwtAuthenticationDetails;
import za.co.bonalabs.bonahr.service.EmployeeService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        Employee employee = employeeService.createEmployee(details.organisationId(), request);

        EmployeeResponse response = EmployeeMapper.toResponse(employee);

        URI location = URI.create("/api/v1/employees/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable UUID id, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        Employee employee = employeeService.getEmployee(details.organisationId(), id);

        return ResponseEntity.ok(EmployeeMapper.toResponse(employee));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getEmployees(Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        List<EmployeeResponse> employees = employeeService.getEmployees(details.organisationId()).stream().map(EmployeeMapper::toResponse).toList();

        return ResponseEntity.ok(employees);
    }
}