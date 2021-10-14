package com.redhat.labs.lodestar.hosting.rest.client;

import javax.ws.rs.*;

import com.redhat.labs.lodestar.hosting.model.GitlabAction;
import com.redhat.labs.lodestar.hosting.model.GitlabCommit;
import org.apache.http.NoHttpResponseException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.hosting.model.GitlabFile;


@Retry(maxRetries = 5, delay = 1200, retryOn = NoHttpResponseException.class, abortOn = WebApplicationException.class)
@Path("/api/v4")
@RegisterRestClient(configKey = "gitlab.api")
@RegisterProvider(value = RestClientResponseMapper.class, priority = 50)
@RegisterClientHeaders(GitlabTokenFactory.class)
@Produces("application/json")
@Consumes("application/json")
public interface GitlabRestClient {
    
    @PUT
    @Path("/projects/{id}/repository/files/{file_path}")
    @Produces("application/json")
    GitlabFile updateFile(@PathParam("id") @Encoded String projectId, @PathParam("file_path") @Encoded String filePath,
            GitlabFile file);

    @POST
    @Path("/projects/{id}/repository/commits")
    @Produces("application/json")
    void createCommit(@PathParam("id") @Encoded String projectId, GitlabCommit commit);
    
    @GET
    @Path("/projects/{id}/repository/files/{file_path}")
    @Produces("application/json")
    GitlabFile getFile(@PathParam("id") @Encoded String projectId, @PathParam("file_path") @Encoded String filePath,
            @QueryParam("ref") @Encoded String ref);
}
