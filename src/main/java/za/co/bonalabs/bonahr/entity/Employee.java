package za.co.bonalabs.bonahr.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.time.LocalDate;

@Entity
@Table(
        name = "employees",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_employees_organisation_employee_number",
                        columnNames = {
                                "organisation_id",
                                "employee_number"
                        }
                ),
                @UniqueConstraint(
                        name = "uq_employees_organisation_email",
                        columnNames = {
                                "organisation_id",
                                "email"
                        }
                )
        }
)
public class Employee {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "organisation_id",
            nullable = false
    )
    private Organisation organisation;

    @Column(
            name = "employee_number",
            nullable = false,
            length = 50
    )
    private String employeeNumber;

    @Column(
            name = "first_name",
            nullable = false,
            length = 100
    )
    private String firstName;

    @Column(
            name = "last_name",
            nullable = false,
            length = 100
    )
    private String lastName;

    @Column(length = 255)
    private String email;

    @Column(name = "job_title", length = 150)
    private String jobTitle;

    @Column(length = 150)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 30)
    private EmploymentType employmentType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(length = 50)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Employee() {
        // Required by JPA
    }

    public Employee(
            Organisation organisation,
            String employeeNumber,
            String firstName,
            String lastName
    ) {
        this.organisation = organisation;
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = EmployeeStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {

        if (id == null) {
            id = UUID.randomUUID();
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Organisation getOrganisation() {
        return organisation;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public String getPhone() {
        return phone;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setStatus(EmployeeStatus status) {
        this.status = status;
    }
}