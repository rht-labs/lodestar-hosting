package com.redhat.labs.lodestar.hosting.mock;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redhat.labs.lodestar.hosting.model.GitlabFile;
import com.redhat.labs.lodestar.hosting.utils.JsonMarshaller;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ResourceLoader {
    static ObjectMapper om;

   static {
        om = new ObjectMapper();
        om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .enable(SerializationFeature.INDENT_OUTPUT);
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    }

    public static String load(String resourceName) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static String loadGitlabFile(String resourceName) {

        String loaded = load(resourceName);
        GitlabFile file = GitlabFile.builder().content(loaded).filePath("engagement.json").build();
        file.encodeFileAttributes();

        try {
            return om.writeValueAsString(file);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }
}
