package com.redhat.labs.lodestar.hosting.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.ListCompareAlgorithm;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.metamodel.clazz.EntityDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.hosting.model.Engagement;
import com.redhat.labs.lodestar.hosting.model.GitlabFile;
import com.redhat.labs.lodestar.hosting.model.HostingEnvironment;
import com.redhat.labs.lodestar.hosting.model.HostingEnvironment.Rollup;
import com.redhat.labs.lodestar.hosting.rest.client.EngagementApiRestClient;
import com.redhat.labs.lodestar.hosting.rest.client.GitlabRestClient;
import com.redhat.labs.lodestar.hosting.utils.JsonMarshaller;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class HostingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostingService.class);

    private static final String ENGAGEMENT_UUID = "engagementUuid";
    public static final String UPDATE_EVENT = "updateEvent";
    public static final String NO_UPDATE = "noUpdate";
    
    private Javers javers;
    
    @PostConstruct
    void setUpJavers() {
        List<String> ignoredProps = Arrays.asList(new String[] { "id", "created", "updated", "projectId", "ocpMajorVersion", "ocpMinorVersion", "region" });
        javers = JaversBuilder.javers().withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE)
                .registerEntity(new EntityDefinition(HostingEnvironment.class, "uuid", ignoredProps)).build();
    }

    @Inject
    @RestClient
    GitlabRestClient gitlabRestClient;

    @Inject
    @RestClient
    EngagementApiRestClient engagementRestClient;

    @Inject
    JsonMarshaller json;

    @ConfigProperty(name = "git.branch")
    String branch;

    @ConfigProperty(name = "hosting.file")
    String hostingEnvFile;

    @ConfigProperty(name = "commit.message.prefix")
    String commitMessagePrefix;

    void onStart(@Observes StartupEvent ev) {
        long count = HostingEnvironment.count();

        LOGGER.debug("There are {} hosting environments in the hosting db.", count);

        if (count == 0) {
            refresh();
            count = HostingEnvironment.count();
            LOGGER.debug("There are now {} hosting environments in the hosting db.", count);
        }
    }

    @Transactional
    public void refresh() {

        List<Engagement> engagements = engagementRestClient.getAllEngagementProjects();

        LOGGER.debug("Engagement count {}", engagements.size());
        engagements.stream().forEach(this::reloadEngagement);

        LOGGER.debug("refresh complete");
    }

    @Transactional
    public long purge() {
        LOGGER.info("Purging hosting db");
        return HostingEnvironment.deleteAll();
    }

    @Transactional
    public void reloadEngagement(Engagement e) {
        LOGGER.trace("Reloading {}", e);

        if (e.getUuid() == null) {
            LOGGER.error("PROJECT {} DOES NOT HAVE AN ENGAGEMENT UUID ON THE DESCRIPTION", e.getProjectId());
        } else {

            List<HostingEnvironment> hostingEnvs = getHostingEnvironmentsFromGitlab(String.valueOf(e.getProjectId()));

            for (HostingEnvironment hostingEnv : hostingEnvs) {
                LOGGER.trace("u {}", hostingEnv);
                fillOutHostingEnvironment(hostingEnv, e.getUuid(), null, e.getProjectId()); //region should be in the gitlab file already

                if (hostingEnv.getUuid() != null && hostingEnv.getCreated() == null) {
                    LOGGER.error("Hosting env {} for enagement {} did not have a create date set this is INCORRECT. Setting to now",
                            hostingEnv.getUuid(), hostingEnv.getEngagementUuid());
                    hostingEnv.setCreated(LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC));
                }
            }
            LOGGER.trace("reloaded engagement (about to persist) {} {}", e, hostingEnvs);
            deleteHostingEnvs(e.getUuid());
            HostingEnvironment.persist(hostingEnvs);
        }
    }

    public long countHostingEnvironments() {
        return HostingEnvironment.count();
    }

    public List<HostingEnvironment> getAllHostingEnvironments(int page, int pageSize) {
        return HostingEnvironment.getHostingEnvironments(page, pageSize);
    }

    public long countHostingForEnagementUuid(String engagementUuid) {
        return HostingEnvironment.countByEnagementUuid(engagementUuid);
    }

    public List<HostingEnvironment> getHostingForEnagementUuid(String engagementUuid) {
        return HostingEnvironment.getByEnagementUuid(engagementUuid);
    }

    public long countHostingForEnagementSubset(List<String> engagementUuids) {
        return HostingEnvironment.countByEngagementSubset(engagementUuids);
    }

    public List<HostingEnvironment> getHostingForEnagementSubset(int page, int pageSize, List<String> engagementUuids) {
        return HostingEnvironment.getByEngagementSubset(page, pageSize, engagementUuids);
    }

    public Map<String, Long> getOcpVersionRollup(Rollup rollup, List<String> region) {
        if(region.isEmpty()) {
            return HostingEnvironment.getColumnRollup(rollup);
        }

        return HostingEnvironment.getColumnRollup(rollup, region);
    }
    
    public boolean isValidSubdomain(String engagementUuid, String subdomain) {
        long taken = HostingEnvironment.countSubDomainOmitEngagementUuid(engagementUuid, subdomain);
        return taken == 0; //not taken by another engagement
    }

    @Transactional
    public String updateHosting(String engagementUuid, List<HostingEnvironment> hostings, String authorEmail,
            String authorName) {

        Engagement engagement = engagementRestClient.getEngagement(engagementUuid);

        for (HostingEnvironment hostingEnv : hostings) {
            if (hostingEnv.getUuid() == null) {
                hostingEnv.generateId();  // new - although legacy service (backend) will set this until deprecated
                hostingEnv.setCreated(LocalDateTime.now());
            }
            if(hostingEnv.getOcpSubDomain() != null && !isValidSubdomain(engagementUuid, hostingEnv.getOcpSubDomain())) {
                throw new WebApplicationException(409);            }
            fillOutHostingEnvironment(hostingEnv, engagement.getUuid(), engagement.getRegion(), engagement.getProjectId());
        }

        List<HostingEnvironment> existing = HostingEnvironment.getByEnagementUuid(engagementUuid);

        Diff diff = javers.compareCollections(existing, hostings, HostingEnvironment.class);

        if (diff.hasChanges()) {
            deleteHostingEnvs(engagementUuid);
            HostingEnvironment.persist(hostings);

            String commitMessage = createCommitMessasge(diff);

            return String.format("%d||%s", engagement.getProjectId(), commitMessage);
        }

        return NO_UPDATE;
    }

    private long deleteHostingEnvs(String engagementUuid) {
        long deletedRows = HostingEnvironment.delete(ENGAGEMENT_UUID, engagementUuid);
        LOGGER.debug("Deleted {} rows for engagement {}", deletedRows, engagementUuid);

        return deletedRows;
    }

    private String createCommitMessasge(Diff diff) {
        LOGGER.trace("count by type {}", diff.countByType());
        StringBuilder commit = new StringBuilder(commitMessagePrefix);

        if (diff.countByType().containsKey(NewObject.class)) {
            commit.append(" added ");
            commit.append(diff.countByType().get(NewObject.class));
        }

        if (diff.countByType().containsKey(ValueChange.class)) {
            commit.append(" changed ");
            commit.append(diff.countByType().get(ValueChange.class));
        }

        if (diff.countByType().containsKey(ObjectRemoved.class)) {
            commit.append(" removed ");
            commit.append(diff.countByType().get(ObjectRemoved.class));
        }

        commit.append("\n");
        commit.append(diff.prettyPrint());

        return commit.toString();
    }

    private void fillOutHostingEnvironment(HostingEnvironment hostingEnv, String engagementUuid, String region, long projectId) {
        hostingEnv.setProjectId(projectId);
        hostingEnv.setEngagementUuid(engagementUuid);
        hostingEnv.setUpdated(LocalDateTime.now()); // While v1 + v2 run in parallel there will a small difference in
                                                    // the updated value

        if (hostingEnv.getOcpVersion() != null) {
            String[] version = hostingEnv.getOcpVersion().split("\\.");
            if (version.length > 0) {
                hostingEnv.setOcpMajorVersion(version[0]);
            }

            if (version.length > 1) {
                hostingEnv.setOcpMinorVersion(version[0] + "." + version[1]);
            }
        }
        
        if(hostingEnv.getRegion() == null) {
            hostingEnv.setRegion(region);
        }

        LOGGER.trace("filled out {}", hostingEnv);

    }

    private List<HostingEnvironment> getHostingEnvironmentsFromGitlab(String projectIdOrPath) {

        try {
            GitlabFile file = gitlabRestClient.getFile(projectIdOrPath, hostingEnvFile, branch);
            file.decodeFileAttributes();
            LOGGER.trace("hosting file json for project {} {}", projectIdOrPath, file.getContent());
            return json.fromJson(file.getContent());
        } catch (WebApplicationException ex) {
            if (ex.getResponse().getStatus() != 404) {
                throw ex;
            }
            LOGGER.error("No hosting file found for {} {}", projectIdOrPath, ex.getMessage());
            return Collections.emptyList();
        } catch (RuntimeException ex) {
            LOGGER.error("Failure retrieving file {} {}", projectIdOrPath, ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    /**
     * 
     * @param message 5 part method -
     *                uuid,projectId,commitMessage,authorEmail,authorName
     */
    @ConsumeEvent(value = UPDATE_EVENT, blocking = true)
    @Transactional
    public void updateHostingInGitlab(String message) {
        LOGGER.trace("Gitlabbing hosting - {}", message);

        String[] uuidProjectMessageEmailName = message.split("\\|\\|");

        List<HostingEnvironment> hostingEnvs = getHostingForEnagementUuid(uuidProjectMessageEmailName[0]);

        String content = json.toJson(hostingEnvs);
        GitlabFile file = GitlabFile.builder().filePath(hostingEnvFile).content(content)
                .commitMessage(uuidProjectMessageEmailName[2]).branch(branch)
                .authorEmail(uuidProjectMessageEmailName[3]).authorName(uuidProjectMessageEmailName[4]).build();
        file.encodeFileAttributes();

        gitlabRestClient.updateFile(uuidProjectMessageEmailName[1], hostingEnvFile, file);
        LOGGER.debug("Gitlab hosting updated - {}", uuidProjectMessageEmailName[0]);

    }
}
