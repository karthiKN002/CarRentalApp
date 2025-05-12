package com.example.gearup.uiactivities.admin;

public class PendingApproval {
    private String id;
    private String email;
    private String phone;
    private String licenseDocument; // Changed from documentUrl

    public PendingApproval() {}

    public boolean hasDocument() {
        return licenseDocument != null && !licenseDocument.isEmpty();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDocumentUrl() { return licenseDocument; }
    public void setDocumentUrl(String documentUrl) { this.licenseDocument = documentUrl; }
}