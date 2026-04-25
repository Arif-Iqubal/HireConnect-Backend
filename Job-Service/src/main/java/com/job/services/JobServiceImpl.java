package com.job.services;


import com.job.dto.request.JobRequest;
import com.job.dto.response.JobResponse;
import com.job.entity.Job;
import com.job.exception.CustomException;
import com.job.repository.JobRepository; 
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepo;

    // 🔹 Add Job
    @Override
    public JobResponse addJob(JobRequest request) {

        Job job = Job.builder()
                .title(request.getTitle())
                .category(request.getCategory())
                .type(request.getType())
                .location(request.getLocation())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .description(request.getDescription())
                .skills(request.getSkills())
                .experienceRequired(request.getExperienceRequired())
                .postedBy(request.getPostedBy())
                .status("OPEN")
                .postedAt(LocalDate.now())
                .build();

        Job saved = jobRepo.save(job);

        return mapToResponse(saved);
    }

    // 🔹 Get All Jobs
    @Override
    public List<JobResponse> getAllJobs() {
        return jobRepo.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    // 🔹 Get By ID
    @Override
    public JobResponse getJobById(Long id) {

        Job job = jobRepo.findById(id)
                .orElseThrow(() -> new CustomException("Job not found"));

        return mapToResponse(job);
    }

    // 🔹 Update Job
    @Override
    public JobResponse updateJob(Long id, JobRequest request) {

        Job existing = jobRepo.findById(id)
                .orElseThrow(() -> new CustomException("Job not found"));

        existing.setTitle(request.getTitle());
        existing.setCategory(request.getCategory());
        existing.setType(request.getType());
        existing.setLocation(request.getLocation());
        existing.setSalaryMin(request.getSalaryMin());
        existing.setSalaryMax(request.getSalaryMax());
        existing.setDescription(request.getDescription());
        existing.setSkills(request.getSkills());
        existing.setExperienceRequired(request.getExperienceRequired());

        Job updated = jobRepo.save(existing);

        return mapToResponse(updated);
    }

    // 🔹 Delete Job
    @Override
    public void deleteJob(Long id) {

        if (!jobRepo.existsById(id)) {
            throw new CustomException("Job not found");
        }

        jobRepo.deleteById(id);
    }

    // 🔹 Filter by Category
    @Override
    public List<JobResponse> getJobsByCategory(String category) {
        return jobRepo.findByCategory(category)
                .stream().map(this::mapToResponse).toList();
    }

    // 🔹 Filter by Location
    @Override
    public List<JobResponse> getJobsByLocation(String location) {
        return jobRepo.findByLocation(location)
                .stream().map(this::mapToResponse).toList();
    }

    // 🔹 Search Jobs
    @Override
    public List<JobResponse> searchJobs(String keyword) {
        return jobRepo.findByTitleContainingIgnoreCase(keyword)
                .stream().map(this::mapToResponse).toList();
    }

    // 🔹 Mapper
    private JobResponse mapToResponse(Job job) {
        return JobResponse.builder()
                .jobId(job.getJobId())
                .title(job.getTitle())
                .category(job.getCategory())
                .location(job.getLocation())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .skills(job.getSkills())
                .experienceRequired(job.getExperienceRequired())
                .status(job.getStatus())
                .postedAt(job.getPostedAt())
                .build();
    }
}