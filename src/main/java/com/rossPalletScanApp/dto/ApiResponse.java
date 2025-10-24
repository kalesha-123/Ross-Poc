package com.rossPalletScanApp.dto;

import java.util.List;

public class ApiResponse<T> {
 public String status;
 public String message;
 public Integer totalPallets;
 public Integer validPalletCount;

 public Object requestedCombination;

 public List<T> data;

 public ApiResponse() {}

 public static <T> ApiResponse<T> of(String status, String message, List<T> data) {
     ApiResponse<T> r = new ApiResponse<>();
     r.status = status;
     r.message = message;
     r.data = data;
     return r;
 }
}