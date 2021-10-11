package com.redhat.labs.lodestar.hosting.model;

import com.redhat.labs.lodestar.hosting.utils.EncodingUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitlabAction {


    @Builder.Default
    private String action = "update";
    private String filePath;
    private String content;
    @Builder.Default
    private String encoding = "base64";

    public static class GitlabActionBuilder {

        public GitlabActionBuilder content(String content) {
            byte[] encodedContents = EncodingUtils.base64Encode(content.getBytes());
            this.content = new String(encodedContents, StandardCharsets.UTF_8);
            return this;
        }
    }
}
