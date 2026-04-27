package com.hireconnect.interview.mapper;

import com.hireconnect.interview.dto.response.InterviewResponse;
import com.hireconnect.interview.entity.Interview;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InterviewMapper {
    InterviewResponse toResponse(Interview interview);
}
