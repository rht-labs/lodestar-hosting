package com.redhat.labs.lodestar.hosting.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Engagement {

    private String uuid;
    private long projectId;
    @JsonProperty("engagement_region")
    private String region;
    
}
