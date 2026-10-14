package com.sgsbarber.model;
import jakarta.persistence.*; import java.time.OffsetDateTime;
@Entity @Table(name="notifications") public class NotificationModel {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="user_id") private Long userId; private String type; private String title; private String message;
 @Column(name="read_at") private OffsetDateTime readAt; @Column(name="created_at",insertable=false,updatable=false) private OffsetDateTime createdAt;
 public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getType(){return type;} public void setType(String v){type=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getMessage(){return message;} public void setMessage(String v){message=v;} public OffsetDateTime getReadAt(){return readAt;} public void setReadAt(OffsetDateTime v){readAt=v;} public OffsetDateTime getCreatedAt(){return createdAt;}
}
