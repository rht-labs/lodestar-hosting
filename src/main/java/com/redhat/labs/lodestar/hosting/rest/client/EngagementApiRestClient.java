package com.redhat.labs.lodestar.hosting.rest.client;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.apache.http.NoHttpResponseException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.hosting.model.Engagement;

@Retry(maxRetries = 5, delay = 1200, retryOn = NoHttpResponseException.class, abortOn = WebApplicationException.class)
@RegisterRestClient(configKey = "engagement.api")
@RegisterProvider(value = RestClientResponseMapper.class, priority = 50)
@Produces("application/json")
@Consumes("application/json")
@Path("/api/v1/engagements")
public interface EngagementApiRestClient {

    @GET
    @Path("/projects")
    List<Engagement> getAllEngagementProjects();
    
    @GET
    @Path("/projects/{engagementUuid}")
    Engagement getProject(@PathParam("engagementUuid") String engagementUuid, @QueryParam("mini") boolean mini);
    
    @GET
    @Path("/uuid/{engagementUuid}")
    Engagement getEngagement(@PathParam("engagementUuid") String engagementUuid);
}
