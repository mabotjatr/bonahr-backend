package za.co.bonalabs.bonahr.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "employee_documents")
public class EmployeeDocument {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private EmployeeDocumentType documentType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "mime_type", length = 150)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uploaded_by_user_id")
    private UUID uploadedByUserId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected EmployeeDocument() {
        // Required by JPA
    }

    public EmployeeDocument(
            Organisation organisation,
            Employee employee,
            EmployeeDocumentType documentType,
            String fileName,
            String storageKey,
            String mimeType,
            Long fileSize,
            UUID uploadedByUserId) {
        this.organisation = organisation;
        this.employee = employee;
        this.documentType = documentType;
        this.fileName = fileName;
        this.storageKey = storageKey;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.uploadedByUserId = uploadedByUserId;
    }

    @PrePersist
    protected void onCreate() {

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Organisation getOrganisation() {
        return organisation;
    }

    public Employee getEmployee() {
        return employee;
    }

    public EmployeeDocumentType getDocumentType() {
        return documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public UUID getUploadedByUserId() {
        return uploadedByUserId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}