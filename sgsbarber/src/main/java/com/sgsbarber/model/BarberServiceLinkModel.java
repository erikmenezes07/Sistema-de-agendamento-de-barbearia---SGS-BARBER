package com.sgsbarber.model;
import jakarta.persistence.*;
@Entity @Table(name="barber_services") @IdClass(BarberServiceLinkId.class) public class BarberServiceLinkModel {
 @Id @Column(name="barber_id") private Long barberId; @Id @Column(name="service_id") private Long serviceId;
 public Long getBarberId(){return barberId;} public void setBarberId(Long v){barberId=v;} public Long getServiceId(){return serviceId;} public void setServiceId(Long v){serviceId=v;}
}
