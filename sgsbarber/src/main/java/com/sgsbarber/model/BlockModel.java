package com.sgsbarber.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "blocks")
public class BlockModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "barber_id") private Long barberId;
    private LocalDate date;
    @Column(name = "start_time") private LocalTime startTime;
    @Column(name = "end_time") private LocalTime endTime;
    private String reason;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getBarberId() { return barberId; }
    public void setBarberId(Long value) { barberId = value; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate value) { date = value; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime value) { startTime = value; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime value) { endTime = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
}
