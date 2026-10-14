package com.sgsbarber.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "services")
public class ServiceModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private BigDecimal price;
    @Column(name = "duration_minutes")
    private Integer duration;
    private Boolean active = true;

    public ServiceModel() {}

    public ServiceModel(String name, BigDecimal price, Integer duration, Boolean active) {
        this.name = name;
        this.price = price;
        this.duration = duration;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
