package com.ztna.model;

import jakarta.persistence.*;

/**
 * Individual Protected Resource catalog item for least-privilege access control.
 */
@Entity
@Table(name = "resources")
public class ResourceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String resourceName;

    private String displayName;

    private String description;

    /**
     * Sensitivity level: LOW | MEDIUM | HIGH | CRITICAL
     */
    private String sensitivityLevel = "MEDIUM";

    private String apiEndpoint;

    public ResourceItem() {}

    public ResourceItem(String resourceName, String displayName, String description,
                        String sensitivityLevel, String apiEndpoint) {
        this.resourceName = resourceName;
        this.displayName = displayName;
        this.description = description;
        this.sensitivityLevel = sensitivityLevel;
        this.apiEndpoint = apiEndpoint;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSensitivityLevel() { return sensitivityLevel; }
    public void setSensitivityLevel(String sensitivityLevel) { this.sensitivityLevel = sensitivityLevel; }

    public String getApiEndpoint() { return apiEndpoint; }
    public void setApiEndpoint(String apiEndpoint) { this.apiEndpoint = apiEndpoint; }
}
