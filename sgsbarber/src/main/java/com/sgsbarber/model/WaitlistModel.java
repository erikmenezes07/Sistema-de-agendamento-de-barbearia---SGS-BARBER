package com.sgsbarber.model;
import jakarta.persistence.*; import java.time.*;
@Entity @Table(name="waitlist") public class WaitlistModel {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="client_id") private Long clientId; @Column(name="barber_id") private Long barberId;
 private LocalDate date; @Column(name="preferred_period") private String preferredPeriod; private String status;
 @Column(name="created_at",insertable=false,updatable=false) private OffsetDateTime createdAt;
 public Long getId(){return id;} public void setId(Long v){id=v;} public Long getClientId(){return clientId;} public void setClientId(Long v){clientId=v;}
 public Long getBarberId(){return barberId;} public void setBarberId(Long v){barberId=v;} public LocalDate getDate(){return date;} public void setDate(LocalDate v){date=v;}
 public String getPreferredPeriod(){return preferredPeriod;} public void setPreferredPeriod(String v){preferredPeriod=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
 public OffsetDateTime getCreatedAt(){return createdAt;}
}
