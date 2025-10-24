package com.rossPalletScanApp.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "box_container_pool")
public class BoxContainerPool {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @Column(nullable = false, unique = true)
 private String containerId;

 @Column(nullable = false)
 private boolean assigned = false;

 public BoxContainerPool() {}

 public BoxContainerPool(String containerId) {
     this.containerId = containerId;
     this.assigned = false;
 }

 // getters/setters

 public Long getId() { return id; }
 public String getContainerId() { return containerId; }
 public void setContainerId(String containerId) { this.containerId = containerId; }

 public boolean isAssigned() { return assigned; }
 public void setAssigned(boolean assigned) { this.assigned = assigned; }
}