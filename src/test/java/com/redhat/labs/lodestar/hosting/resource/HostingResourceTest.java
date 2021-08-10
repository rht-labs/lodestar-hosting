package com.redhat.labs.lodestar.hosting.resource;

import static io.restassured.RestAssured.when;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.redhat.labs.lodestar.hosting.mock.ExternalApiWireMock;
import com.redhat.labs.lodestar.hosting.model.HostingEnvironment;
import com.redhat.labs.lodestar.hosting.model.HostingEnvironment.Rollup;
import com.redhat.labs.lodestar.hosting.service.HostingService;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(HostingResource.class)
@QuarkusTestResource(ExternalApiWireMock.class)
class HostingResourceTest {

    @Inject
    HostingService hostingService;

    @BeforeEach
    void init() {
        hostingService.refresh();
    }

    @Test
    void testRefresh() {
        when().put("refresh").then().statusCode(200);
        assertEquals(2, hostingService.countHostingEnvironments());
    }

    @Test
    void testNoUpdateByUuid() {
        List<HostingEnvironment> he = hostingService.getHostingForEnagementUuid("second");

        given().contentType(ContentType.JSON).pathParam("engagementUuid", "second").body(he).put("/engagements/{engagementUuid}").then()
                .statusCode(200).body("size()", is(1)).header("x-total-hosting", equalTo("1"));
    }

    @Test
    void testUsedSubdomainUpdateByUuid() {
        List<HostingEnvironment> he = hostingService.getHostingForEnagementUuid("second");

        assertEquals(1, he.size());
        he.get(0).setOcpSubDomain("red-1");

        given().contentType(ContentType.JSON).pathParam("engagementUuid", "second").body(he).put("/engagements/{engagementUuid}").then()
                .statusCode(409);
    }

    @Test
    void testUpdateByUuid() {
        List<HostingEnvironment> he = hostingService.getHostingForEnagementUuid("second");

        assertEquals(1, he.size());
        he.get(0).setName("update");

        HostingEnvironment he2 = HostingEnvironment.builder().additionalDetails("meh").name("dos").ocpCloudProviderName("gcp")
                .ocpCloudProviderRegion("gcp-east-1").ocpClusterSize("medium").ocpVersion("5.0").ocpPersistentStorageSize("1MB").ocpSubDomain("meh")
                .build();

        he.add(he2);

        given().contentType(ContentType.JSON).pathParam("engagementUuid", "second").body(he).put("/engagements/{engagementUuid}").then()
                .statusCode(200).body("size()", is(2)).header("x-total-hosting", equalTo("2")).body("[1].uuid", equalTo("uuid2"))
                .body("[1].engagement_uuid", equalTo("second")).body("[1].environment_name", equalTo("update"))
                .body("[0].environment_name", equalTo("dos"));

        he.clear();

        given().contentType(ContentType.JSON).pathParam("engagementUuid", "second").body(he).put("/engagements/{engagementUuid}").then()
                .statusCode(200).body("size()", is(0)).header("x-total-hosting", equalTo("0"));

    }

    @Test
    void testGet() {
        when().get().then().statusCode(200).body("size()", is(2)).header("x-total-hosting", equalTo("2")).body("[0].uuid", equalTo("uuid"))
                .body("[1].uuid", equalTo("uuid2"));
    }

    @Test
    void testGetByUuid() {
        given().pathParam("engagementUuid", "second").get("/engagements/{engagementUuid}").then().statusCode(200).body("size()", is(1))
                .header("x-total-hosting", equalTo("1")).body("[0].uuid", equalTo("uuid2")).body("[0].engagement_uuid", equalTo("second"));
    }

    @Test
    void testGetUuidList() {
        given().queryParam("engagementUuids", "cb570945-a209-40ba-9e42-63a7993baf4d").queryParam("engagementUuids", "second").when().get().then()
                .statusCode(200).body("size()", is(2)).header("x-total-hosting", equalTo("2"))
                .body("[0].engagement_uuid", equalTo("cb570945-a209-40ba-9e42-63a7993baf4d")).body("[1].engagement_uuid", equalTo("second"));
    }

    @Test
    void testGetVersions() {
        given().queryParam("depth", Rollup.OCP_VERSION).when().get("/openshift/versions").then().statusCode(200).body("All", equalTo(2))
                .body("'4.9.16'", equalTo(1)).body("'4.7.16'", equalTo(1));

        given().queryParam("depth", Rollup.OCP_VERSION_MAJOR).when().get("/openshift/versions").then().statusCode(200).body("All", equalTo(2))
                .body("4", equalTo(2));

        given().queryParam("depth", Rollup.OCP_VERSION_MINOR).when().get("/openshift/versions").then().statusCode(200).body("All", equalTo(2))
                .body("'4.9'", equalTo(1)).body("'4.7'", equalTo(1));
    }

    @ParameterizedTest
    @CsvSource(value = { "na,1", "latam,1" })
    void testVersionsForRegion(String region, int expectedEnvSize) {
        List<String> regionList = Collections.singletonList(region);
        given().queryParam("depth", Rollup.OCP_VERSION_MAJOR).queryParam("region", regionList).when().get("/openshift/versions").then()
                .statusCode(200).body("All", equalTo(expectedEnvSize)).body("4", equalTo(expectedEnvSize));
    }

    @ParameterizedTest
    @CsvSource(value = { "cb570945-a209-40ba-9e42-63a7993baf4d,red-1,200", "cb570945-a209-40ba-9e42-63a7993baf4d,red-99,200", "second,red-1,409" })
    void testValidSubdomain(String engagementUuid, String subdomain, int statusCode) {

        given().pathParam("engagementUuid", engagementUuid).pathParam("subdomain", subdomain).when()
                .head("/subdomain/valid/{engagementUuid}/{subdomain}").then().statusCode(statusCode);
    }
}
