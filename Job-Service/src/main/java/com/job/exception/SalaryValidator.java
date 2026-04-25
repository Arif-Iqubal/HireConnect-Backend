package com.job.exception;

import com.job.dto.request.JobRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SalaryValidator implements ConstraintValidator<ValidSalary, JobRequest> {

    @Override
    public boolean isValid(JobRequest job, ConstraintValidatorContext context) {
        return job.getSalaryMax() >= job.getSalaryMin();
    }
}