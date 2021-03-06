package com.redhat.labs.lodestar.hosting.mock;

import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class ExternalApiWireMock implements QuarkusTestResourceLifecycleManager {

    private WireMockServer wireMockServer; 
    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        
        String body = ResourceLoader.load("seed-engagement.json");
        
        stubFor(get(urlEqualTo("/api/v2/engagements")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
        body = ResourceLoader.load("gitlab-project-13065.json");
        

        stubFor(get(urlEqualTo("/api/v2/engagements/cb570945-a209-40ba-9e42-63a7993baf4d")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
        body = ResourceLoader.load("gitlab-project-20962.json");

        stubFor(get(urlEqualTo("/api/v2/engagements/second")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));

        body = ResourceLoader.load("engagement-second.json");
        
        stubFor(get(urlEqualTo("/api/v2/engagements/second")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
        body = ResourceLoader.load("gitlab-file-hosting-13065.json");
        
        stubFor(get(urlEqualTo("/api/v4/projects/13065/repository/files/engagement%2Fhosting.json?ref=master")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
        body = ResourceLoader.load("gitlab-file-hosting-20962.json");
        
        stubFor(get(urlEqualTo("/api/v4/projects/20962/repository/files/engagement%2Fhosting.json?ref=master")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));

        body = ResourceLoader.loadGitlabFile("legacy-engagement-no-host.json");

        stubFor(get(urlEqualTo("/api/v4/projects/20962/repository/files/engagement.json?ref=master")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
        ));

        stubFor(post(urlEqualTo("/api/v4/projects/20962/repository/commits")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
        stubFor(get(urlEqualTo("/api/v4/projects/99/repository/files/engagement%2Fhosting.json?ref=master")).willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type",  "application/json")
                .withBody("{\"msg\": \" 500 Something bad happened\"}")
                ));
        
        stubFor(get(urlEqualTo("/api/v4/projects/30/repository/files/engagement%2Fhosting.json?ref=master")).willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type",  "application/json")
                .withBody("{\"msg\": \" 404 No file found \"}")
                ));
        
        Map<String, String> config = new HashMap<>();
        config.put("gitlab.api/mp-rest/url", wireMockServer.baseUrl());
        config.put("engagement.api/mp-rest/url", wireMockServer.baseUrl());
        
        return config;
    }

    @Override
    public void stop() {
        if(null != wireMockServer) {
           wireMockServer.stop();
        }
        
    }


}
