package com.redhat.labs.lodestar.hosting.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitlabCommit {

    private String branch;
    private String commitMessage;
    private List<GitlabAction> actions;
    private String authorName;
    private String authorEmail;

}
