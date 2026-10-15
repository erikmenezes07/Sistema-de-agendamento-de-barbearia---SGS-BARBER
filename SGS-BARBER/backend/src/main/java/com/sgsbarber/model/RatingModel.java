package com.sgsbarber.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ratings")
public class RatingModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "appointment_id", nullable = false) private Long appointmentId;
    @Column(name = "client_id", nullable = false) private Long clientId;
    @Column(name = "barber_id", nullable = false) private Long barberId;
    @Column(name = "shop_rating") private Short shopRating;
    @Column(name = "barber_rating") private Short barberRating;
    private String comment;

    // Compatibility fields for the original demo frontend; they are not persisted.
    @Transient private String clientName;
    @Transient private Integer stars;

    public RatingModel() {}
    public RatingModel(String clientName, Integer stars, String comment) {
        this.clientName = clientName; this.stars = stars; this.comment = comment;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long value) { this.appointmentId = value; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long value) { this.clientId = value; }
    public Long getBarberId() { return barberId; }
    public void setBarberId(Long value) { this.barberId = value; }
    public Short getShopRating() { return shopRating; }
    public void setShopRating(Short value) { this.shopRating = value; }
    public Short getBarberRating() { return barberRating; }
    public void setBarberRating(Short value) { this.barberRating = value; }
    public String getComment() { return comment; }
    public void setComment(String value) { this.comment = value; }
    public String getClientName() { return clientName; }
    public void setClientName(String value) { this.clientName = value; }
    public Integer getStars() { return stars; }
    public void setStars(Integer value) { this.stars = value; }
}
