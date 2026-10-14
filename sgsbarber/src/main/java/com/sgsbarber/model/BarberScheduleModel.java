package com.sgsbarber.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "barber_schedules")
public class BarberScheduleModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "barber_id", nullable = false) private Long barberId;
    private Short weekday;
    private String type;
    @Column(name = "open_time") private LocalTime openTime;
    @Column(name = "close_time") private LocalTime closeTime;
    @Column(name = "break_start") private LocalTime breakStart;
    @Column(name = "break_end") private LocalTime breakEnd;
    @Column(name = "buffer_minutes") private Integer bufferMinutes = 0;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getBarberId() { return barberId; }
    public void setBarberId(Long value) { barberId = value; }
    public Short getWeekday() { return weekday; }
    public void setWeekday(Short value) { weekday = value; }
    public String getType() { return type; }
    public void setType(String value) { type = value; }
    public LocalTime getOpenTime() { return openTime; }
    public void setOpenTime(LocalTime value) { openTime = value; }
    public LocalTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalTime value) { closeTime = value; }
    public LocalTime getBreakStart() { return breakStart; }
    public void setBreakStart(LocalTime value) { breakStart = value; }
    public LocalTime getBreakEnd() { return breakEnd; }
    public void setBreakEnd(LocalTime value) { breakEnd = value; }
    public Integer getBufferMinutes() { return bufferMinutes; }
    public void setBufferMinutes(Integer value) { bufferMinutes = value; }
}
