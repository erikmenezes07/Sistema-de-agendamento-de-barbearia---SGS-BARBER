package com.sgsbarber.model;
import java.io.Serializable;
public class BarberServiceLinkId implements Serializable { public Long barberId; public Long serviceId; public BarberServiceLinkId(){} public BarberServiceLinkId(Long b,Long s){barberId=b;serviceId=s;} public boolean equals(Object o){if(!(o instanceof BarberServiceLinkId x))return false;return java.util.Objects.equals(barberId,x.barberId)&&java.util.Objects.equals(serviceId,x.serviceId);} public int hashCode(){return java.util.Objects.hash(barberId,serviceId);} }
