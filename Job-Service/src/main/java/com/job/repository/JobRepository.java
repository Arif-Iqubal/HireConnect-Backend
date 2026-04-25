package com.job.repository;


import com.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findByTitle(String title);

    List<Job> findByCategory(String category);

    List<Job> findByLocation(String location);

    List<Job> findByPostedBy(Long postedBy);

    List<Job> findByStatus(String status);

    List<Job> findByTitleContainingIgnoreCase(String keyword);
}