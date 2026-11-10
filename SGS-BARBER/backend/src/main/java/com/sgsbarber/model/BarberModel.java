package com.sgsbarber.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "barbers")
public class BarberModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String specialty;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> specialties;
    private String photoUrl;
    private String email;
    private String phone;
    @Column(name = "commission_percent")
    private BigDecimal commission = BigDecimal.ZERO;
    private Boolean active = true;

    @Transient
    private List<Long> serviceIds;

    public BarberModel() {}

    public BarberModel(String name, String email, String phone, Boolean active) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public List<String> getSpecialties() { return specialties; }
    public void setSpecialties(List<String> specialties) { this.specialties = specialties; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public BigDecimal getCommission() { return commission; }
    public void setCommission(BigDecimal commission) { this.commission = commission; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public List<Long> getServiceIds() { return serviceIds; }
    public void setServiceIds(List<Long> serviceIds) { this.serviceIds = serviceIds; }
}
