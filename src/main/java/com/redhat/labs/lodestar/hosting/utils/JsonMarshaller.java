package com.redhat.labs.lodestar.hosting.utils;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redhat.labs.lodestar.hosting.exception.HostingException;
import com.redhat.labs.lodestar.hosting.model.HostingEnvironment;

/**
 * Used converting String to Objects (non-request, non-response)
 * 
 * @author mcanoy
 *
 */
@ApplicationScoped
public class JsonMarshaller {
    public static final Logger LOGGER = LoggerFactory.getLogger(JsonMarshaller.class);
    
    ObjectMapper om;
    
    public JsonMarshaller() {
        om = new ObjectMapper();
        om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .enable(SerializationFeature.INDENT_OUTPUT);
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        LOGGER.debug("marshaller started");
    }

    public List<HostingEnvironment> fromJson(String json) {
        try {
            return om.readValue(json, new TypeReference<List<HostingEnvironment>>() {
            });
        } catch (JsonProcessingException e) {
            throw new HostingException("Error translating hosting json data", e);
        }
    }

    public String toJson(List<HostingEnvironment> hostingEnv) {
        try {
            return om.writeValueAsString(hostingEnv);
        } catch (JsonProcessingException e) {
            throw new HostingException("Error translating hosting data to json", e);
        }
    }

}
