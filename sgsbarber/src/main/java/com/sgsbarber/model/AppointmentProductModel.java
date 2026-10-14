package com.sgsbarber.model;
import jakarta.persistence.*; import java.math.BigDecimal;
@Entity @Table(name="appointment_products") public class AppointmentProductModel {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="appointment_id") private Long appointmentId; @Column(name="product_id") private Long productId; private Integer quantity; @Column(name="unit_price") private BigDecimal unitPrice; private BigDecimal total;
 public Long getId(){return id;} public void setId(Long v){id=v;} public Long getAppointmentId(){return appointmentId;} public void setAppointmentId(Long v){appointmentId=v;} public Long getProductId(){return productId;} public void setProductId(Long v){productId=v;} public Integer getQuantity(){return quantity;} public void setQuantity(Integer v){quantity=v;} public BigDecimal getUnitPrice(){return unitPrice;} public void setUnitPrice(BigDecimal v){unitPrice=v;} public BigDecimal getTotal(){return total;} public void setTotal(BigDecimal v){total=v;}
}
