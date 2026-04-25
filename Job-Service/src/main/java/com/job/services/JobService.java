package com.job.services;


import com.job.dto.request.JobRequest;
import com.job.dto.response.JobResponse;

import java.util.List;

public interface JobService {

    JobResponse addJob(JobRequest request);

    List<JobResponse> getAllJobs();

    JobResponse getJobById(Long id);

    JobResponse updateJob(Long id, JobRequest request);

    void deleteJob(Long id);

    List<JobResponse> getJobsByCategory(String category);

    List<JobResponse> getJobsByLocation(String location);

    List<JobResponse> searchJobs(String keyword);
}