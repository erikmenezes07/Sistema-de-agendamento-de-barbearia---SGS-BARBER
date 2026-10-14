package com.sgsbarber.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "appointments")
public class AppointmentModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "client_id") private Long clientId;
    @Column(name = "barber_id", nullable = false) private Long barberId;
    @Column(name = "service_id", nullable = false) private Long serviceId;
    private LocalDate date;
    @JsonProperty("time")
    @Column(name = "start_time") private LocalTime startTime;
    @Column(name = "end_time") private LocalTime endTime;
    @Column(name = "duration_minutes") private Integer durationMinutes;
    @Column(name = "buffer_minutes") private Integer bufferMinutes = 0;
    private String status;
    private BigDecimal price;
    @JsonProperty("extras")
    @Column(name = "extras_total") private BigDecimal extrasTotal = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal finalTotal = BigDecimal.ZERO;
    private String paymentMethod;
    private String checkoutNote;
    @JsonProperty("prefs")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "client_preferences_json") private List<String> clientPreferences;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;

    @Transient private String client;
    @Transient private String barber;
    @Transient private String service;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long value) { clientId = value; }
    public Long getBarberId() { return barberId; }
    public void setBarberId(Long value) { barberId = value; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long value) { serviceId = value; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate value) { date = value; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime value) { startTime = value; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime value) { endTime = value; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer value) { durationMinutes = value; }
    public Integer getBufferMinutes() { return bufferMinutes; }
    public void setBufferMinutes(Integer value) { bufferMinutes = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { price = value; }
    public BigDecimal getExtrasTotal() { return extrasTotal; }
    public void setExtrasTotal(BigDecimal value) { extrasTotal = value; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal value) { discount = value; }
    public BigDecimal getFinalTotal() { return finalTotal; }
    public void setFinalTotal(BigDecimal value) { finalTotal = value; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String value) { paymentMethod = value; }
    public String getCheckoutNote() { return checkoutNote; }
    public void setCheckoutNote(String value) { checkoutNote = value; }
    public List<String> getClientPreferences() { return clientPreferences; }
    public void setClientPreferences(List<String> value) { clientPreferences = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getClient() { return client; }
    public void setClient(String value) { client = value; }
    public String getBarber() { return barber; }
    public void setBarber(String value) { barber = value; }
    public String getService() { return service; }
    public void setService(String value) { service = value; }
}
