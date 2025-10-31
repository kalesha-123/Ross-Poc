package com.rossPalletScanApp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.rossPalletScanApp.dto.OrganizedInferenceResponse;
import com.rossPalletScanApp.service.ImageService;

@RestController
@RequestMapping("/api")
public class ImageController {
    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/scans")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            OrganizedInferenceResponse out = imageService.uploadAndInfer(file);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of(
                            "error", "Error uploading/inferencing",
                            "message", e.getMessage()
                    ));
        }
    }
}