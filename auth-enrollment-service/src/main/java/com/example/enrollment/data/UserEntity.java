package com.example.enrollment.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private String password;

    @Column(length = 16, nullable = false)
    private String role; // STUDENT or FACULTY

    public UserEntity() {}
    public UserEntity(String id, String role) {
        this.id = id;
        this.role = role;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
