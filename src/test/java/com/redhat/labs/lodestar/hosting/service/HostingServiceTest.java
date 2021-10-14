package com.redhat.labs.lodestar.hosting.service;

import com.google.gson.*;
import com.redhat.labs.lodestar.hosting.mock.ResourceLoader;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@QuarkusTest
public class HostingServiceTest {

    @Inject
    HostingService hostingService;
    static String expected;
    
    @BeforeAll
    static void setUp() {
        expected = ResourceLoader.load("legacy-engagement-expected.json");
    }

    @Test
    void convertLegacyJsonHostingUpdate() {
        String legacyEngagement = ResourceLoader.load("legacy-engagement.json");
        String hostingEnvs = ResourceLoader.load("hosting-env.json");

        String result = hostingService.createLegacyJson(legacyEngagement, hostingEnvs);
        Assertions.assertEquals(expected, result);
    }

    @Test
    void convertLegacyJsonHostingNew() {
        String legacyEngagement = ResourceLoader.load("legacy-engagement-no-host.json");
        String hostingEnvs = ResourceLoader.load("hosting-env.json");

        String result = hostingService.createLegacyJson(legacyEngagement, hostingEnvs);
        Assertions.assertEquals(expected, result);
    }

}
