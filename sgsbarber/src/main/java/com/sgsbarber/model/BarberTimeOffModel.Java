package com.sgsbarber.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "barber_time_off")
public class BarberTimeOffModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "barber_id", nullable = false)
    private Long barberId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    private String reason;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getBarberId() { return barberId; }
    public void setBarberId(Long value) { barberId = value; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate value) { startDate = value; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate value) { endDate = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
}
