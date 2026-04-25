package com.job.controller;


import com.job.dto.request.JobRequest;
import com.job.dto.response.JobResponse;
import com.job.services.JobService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    // 🔹 Add Job
    @PostMapping
    public ResponseEntity<JobResponse> addJob(
            @Valid @RequestBody JobRequest request) {

        return ResponseEntity.ok(jobService.addJob(request));
    }

    // 🔹 Get All Jobs
    @GetMapping
    public ResponseEntity<List<JobResponse>> getAllJobs() {

        return ResponseEntity.ok(jobService.getAllJobs());
    }

    // 🔹 Get Job by ID
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable Long id) {

        return ResponseEntity.ok(jobService.getJobById(id));
    }

    // 🔹 Update Job
    @PutMapping("/{id}")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobRequest request) {

        return ResponseEntity.ok(jobService.updateJob(id, request));
    }

    // 🔹 Delete Job
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteJob(@PathVariable Long id) {

        jobService.deleteJob(id);
        return ResponseEntity.ok("Job deleted successfully");
    }

    // 🔹 Filter by Category
    @GetMapping("/category")
    public ResponseEntity<List<JobResponse>> getJobsByCategory(
            @RequestParam String category) {

        return ResponseEntity.ok(jobService.getJobsByCategory(category));
    }

    // 🔹 Filter by Location
    @GetMapping("/location")
    public ResponseEntity<List<JobResponse>> getJobsByLocation(
            @RequestParam String location) {

        return ResponseEntity.ok(jobService.getJobsByLocation(location));
    }

    // 🔹 Search Jobs (by title keyword)
    @GetMapping("/search")
    public ResponseEntity<List<JobResponse>> searchJobs(
            @RequestParam String keyword) {

        return ResponseEntity.ok(jobService.searchJobs(keyword));
    }
}
