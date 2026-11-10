package com.sgsbarber.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="products") public class ProductModel {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private String name; private BigDecimal price; private Integer stock; private Boolean active=true;
 public Long getId(){return id;} public void setId(Long v){id=v;} public String getName(){return name;} public void setName(String v){name=v;}
 public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){price=v;} public Integer getStock(){return stock;} public void setStock(Integer v){stock=v;}
 public Boolean getActive(){return active;} public void setActive(Boolean v){active=v;}
}
