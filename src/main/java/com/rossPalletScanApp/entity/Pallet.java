package com.rossPalletScanApp.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pallets")
public class Pallet {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 // e.g., "Pallet 1"
 @Column(nullable = false, unique = true)
 private String code;

 @Column(nullable = false, unique = true)
 private String masterContainerId;

 @Column(nullable = false)
 private int capacity = 3; // fixed

 @OneToMany(mappedBy = "pallet", cascade = CascadeType.ALL, orphanRemoval = true)
 private List<Box> boxes = new ArrayList<>();

 public Pallet() {}

 public Pallet(String code, String masterContainerId, int capacity) {
     this.code = code;
     this.masterContainerId = masterContainerId;
     this.capacity = capacity;
 }

 // getters/setters

 public Long getId() { return id; }
 public String getCode() { return code; }
 public void setCode(String code) { this.code = code; }

 public String getMasterContainerId() { return masterContainerId; }
 public void setMasterContainerId(String masterContainerId) { this.masterContainerId = masterContainerId; }

 public int getCapacity() { return capacity; }
 public void setCapacity(int capacity) { this.capacity = capacity; }

 public List<Box> getBoxes() { return boxes; }
 public void setBoxes(List<Box> boxes) { this.boxes = boxes; }
}