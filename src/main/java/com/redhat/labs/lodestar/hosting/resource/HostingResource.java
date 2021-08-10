package com.redhat.labs.lodestar.hosting.resource;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.redhat.labs.lodestar.hosting.model.HostingEnvironment;
import com.redhat.labs.lodestar.hosting.model.HostingEnvironment.Rollup;
import com.redhat.labs.lodestar.hosting.service.HostingService;

import io.vertx.mutiny.core.eventbus.EventBus;

@RequestScoped
@Path("/api/hosting")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Hosting", description = "Hosting environments for an Engagement")
public class HostingResource {
    private static final String COUNT_HEADER = "x-total-hosting";
    
    @Inject
    HostingService hostingService;
    
    @Inject
    EventBus bus;

    @GET
    public Response getHostingEnvironments(@QueryParam("engagementUuids") List<String> engagementUuids,
            @DefaultValue("0") @QueryParam(value = "page") int page, @DefaultValue("20") @QueryParam(value = "pageSize") int pageSize) {

        List<HostingEnvironment> hostingEnvs;
        long hostingEnvCount;

        if(engagementUuids.isEmpty()) {
            hostingEnvCount = hostingService.countHostingEnvironments();
            hostingEnvs = hostingService.getAllHostingEnvironments(page, pageSize);  
        } else {   
            hostingEnvCount = hostingService.countHostingForEnagementSubset(engagementUuids);
            hostingEnvs = hostingService.getHostingForEnagementSubset(page, pageSize, engagementUuids);
        }
        
        return Response.ok(hostingEnvs).header("x-page", page).header("x-per-page", pageSize)
                .header(COUNT_HEADER, hostingEnvCount).header("x-total-pages", (hostingEnvCount / pageSize) + 1).build();
    }
    
    @GET
    @Path("/engagements/{engagementUuid}")
    public Response getHostingEnvironmentsByEngagementUuid(@PathParam("engagementUuid") String engagementUuid) {
        List<HostingEnvironment> hostingEnvs = hostingService.getHostingForEnagementUuid(engagementUuid);
        long hostingEnvCount = hostingService.countHostingForEnagementUuid(engagementUuid);
        
        return Response.ok(hostingEnvs).header(COUNT_HEADER, hostingEnvCount).build();
    }
    
    @PUT
    @Path("/engagements/{engagementUuid}")
    @APIResponses(value = { @APIResponse(responseCode = "409", description = "This subdomain is already taken"),
            @APIResponse(responseCode = "200", description = "The hosting env was was saved to the db") })
    public Response updateHostingEnvironments(@PathParam(value = "engagementUuid") String uuid, List<HostingEnvironment> hostings,
            @DefaultValue("bot@bot.com") @QueryParam(value = "authorEmail") String authorEmail,
            @DefaultValue("Hosting System") @QueryParam(value = "authorName") String authorName) {

        String projectIdAndCommitMessage = hostingService.updateHosting(uuid, hostings, authorEmail, authorName);
        
        if(!HostingService.NO_UPDATE.equals(projectIdAndCommitMessage)) {
            String message = String.format("%s||%s||%s||%s", uuid, projectIdAndCommitMessage, authorEmail, authorName);
            bus.publish(HostingService.UPDATE_EVENT, message);
        }
        
        return getHostingEnvironmentsByEngagementUuid(uuid);
    }
    
    @HEAD
    @Path("/subdomain/valid/{engagementUuid}/{subdomain}")
    @APIResponses(value = { @APIResponse(responseCode = "409", description = "This subdomain is already taken"),
            @APIResponse(responseCode = "200", description = "The subdomain is available or already assigned to this engagement") })
    public Response isSubddomaiinValid(@PathParam("engagementUuid") String engagementUuid, @PathParam("subdomain") String subdomain) {
        boolean isValid = hostingService.isValidSubdomain(engagementUuid, subdomain);

        if (isValid) {
            return Response.ok().build();
        }

        return Response.status(409).build();

    }
    
    @GET
    @Path("/openshift/versions")
    public Response getOpenShiftVersions(@QueryParam("depth") final Rollup rollup, @QueryParam("region") List<String> region) {
        Map<String, Long> versions = hostingService.getOcpVersionRollup(rollup, region);
        
        return Response.ok(versions).build();
    }
    
    @PUT
    @Path("/refresh")
    @APIResponses(value = { @APIResponse(responseCode = "202", description = "The request was accepted and will be processed.") })
    @Operation(summary = "Refreshes database with data in git, purging first")
    public Response refreshData() {

        hostingService.purge();
        hostingService.refresh();
        
        long hostingCount = hostingService.countHostingEnvironments();
        
        return Response.ok().header(COUNT_HEADER, hostingCount).build();
    }
    
    
    
}
