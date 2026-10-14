package com.sgsbarber.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "clients")
public class ClientModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String phone;
    private String email;
    private String cpf;
    @Column(name = "birth_date") private LocalDate birthDate;
    @Column(name = "favorite_barber_id") private Long favoriteBarberId;
    private String notes;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences_json") private Map<String, Object> preferences;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", insertable = false) private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { this.phone = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { this.email = value; }
    public String getCpf() { return cpf; }
    public void setCpf(String value) { this.cpf = value; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate value) { this.birthDate = value; }
    public Long getFavoriteBarberId() { return favoriteBarberId; }
    public void setFavoriteBarberId(Long value) { this.favoriteBarberId = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { this.notes = value; }
    public Map<String, Object> getPreferences() { return preferences; }
    public void setPreferences(Map<String, Object> value) { this.preferences = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
