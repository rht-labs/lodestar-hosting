package com.redhat.labs.lodestar.hosting.service;

import com.redhat.labs.lodestar.hosting.mock.ResourceLoader;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class HostingServiceTest {

    @Inject
    HostingService hostingService;
    static String expected;
    
    @BeforeAll
    static void setUp() {
        expected = ResourceLoader.load("legacy-engagement-expected.json");
    }

    @BeforeEach
    void init() {
        hostingService.refresh();
    }

    @Test
    void testCheckDB() {
        hostingService.purge();
        assertEquals(0, hostingService.getAllHostingEnvironments(0, 10).size());

        hostingService.checkDBPopulated();

        assertEquals(2, hostingService.getAllHostingEnvironments(0, 10).size());

        hostingService.checkDBPopulated();

        assertEquals(2, hostingService.getAllHostingEnvironments(0, 10).size());
    }

    @Test
    void convertLegacyJsonHostingUpdate() {
        String legacyEngagement = ResourceLoader.load("legacy-engagement.json");
        String hostingEnvs = ResourceLoader.load("hosting-env.json");

        String result = hostingService.createLegacyJson(legacyEngagement, hostingEnvs);
        assertEquals(expected, result);
    }

    @Test
    void convertLegacyJsonHostingNew() {
        String legacyEngagement = ResourceLoader.load("legacy-engagement-no-host.json");
        String hostingEnvs = ResourceLoader.load("hosting-env.json");

        String result = hostingService.createLegacyJson(legacyEngagement, hostingEnvs);
        assertEquals(expected, result);
    }
}
