package com.sgsbarber.dto;

public class ServiceDTO {
    private Long id;
    private String name;
    private Double price;
    private Integer duration;
    private Boolean active;

    public ServiceDTO() {}

    public ServiceDTO(Long id, String name, Double price, Integer duration, Boolean active) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.duration = duration;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}