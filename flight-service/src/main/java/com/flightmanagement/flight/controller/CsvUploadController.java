package com.flightmanagement.flight.controller;

import com.flightmanagement.flight.dto.response.FlightUploadBatchResponseDto;
import com.flightmanagement.flight.security.UserContext;
import com.flightmanagement.flight.service.CsvUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class CsvUploadController {

    private final CsvUploadService csvUploadService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadCsvFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        UserContext userContext = (UserContext) authentication.getPrincipal();

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().body("Only CSV files are allowed");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            return ResponseEntity.badRequest().body("File size exceeds 10MB limit");
        }

        // Process upload asynchronously
        CompletableFuture<FlightUploadBatchResponseDto> future =
                csvUploadService.processUpload(file, userContext);

        return ResponseEntity.accepted().body("File upload started. Processing asynchronously.");
    }
}