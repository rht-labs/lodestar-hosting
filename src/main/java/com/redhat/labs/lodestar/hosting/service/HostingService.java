package com.redhat.labs.lodestar.hosting.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.google.gson.*;
import com.redhat.labs.lodestar.hosting.model.*;
import io.quarkus.scheduler.Scheduled;
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

import com.redhat.labs.lodestar.hosting.model.HostingEnvironment.Rollup;
import com.redhat.labs.lodestar.hosting.rest.client.EngagementApiRestClient;
import com.redhat.labs.lodestar.hosting.rest.client.GitlabRestClient;
import com.redhat.labs.lodestar.hosting.utils.JsonMarshaller;

import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class HostingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostingService.class);

    private static final String ENGAGEMENT_UUID = "engagementUuid";
    public static final String UPDATE_EVENT = "updateEvent";
    public static final String NO_UPDATE = "noUpdate";
    
    private Javers javers;

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    @PostConstruct
    void setUpJavers() {
        List<String> ignoredProps = Arrays.asList("id", "created", "updated", "projectId", "ocpMajorVersion", "ocpMinorVersion", "region");
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

    @Scheduled(every = "5m",  delayed = "15s")
    void checkDBPopulated() {
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

        List<Engagement> engagements = engagementRestClient.getAllEngagements();

        LOGGER.debug("Engagement count {}", engagements.size());
        engagements.forEach(this::reloadEngagement);

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
                    LOGGER.error("Hosting env {} for engagement {} did not have a create date set this is INCORRECT. Setting to now",
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

    public long countHostingForEngagementUuid(String engagementUuid) {
        return HostingEnvironment.countByEnagementUuid(engagementUuid);
    }

    public List<HostingEnvironment> getHostingForEngagementUuid(String engagementUuid) {
        return HostingEnvironment.getByEnagementUuid(engagementUuid);
    }

    public long countHostingForEngagementSubset(List<String> engagementUuids) {
        return HostingEnvironment.countByEngagementSubset(engagementUuids);
    }

    public List<HostingEnvironment> getHostingForEngagementSubset(int page, int pageSize, List<String> engagementUuids) {
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
    public String updateHosting(String engagementUuid, List<HostingEnvironment> hostings) {

        Engagement engagement = engagementRestClient.getEngagement(engagementUuid);

        for (HostingEnvironment hostingEnv : hostings) {
            if (hostingEnv.getUuid() == null) {
                hostingEnv.generateId();  // new - although legacy service (backend) will set this until deprecated
                hostingEnv.setCreated(LocalDateTime.now());
            } else if(hostingEnv.getCreated() == null) {
                HostingEnvironment env = HostingEnvironment.find("uuid = ?1", hostingEnv.getUuid()).singleResult();
                //Check db for existing and set unmodifiable values (Created)
                if(env == null) {
                    hostingEnv.setCreated(LocalDateTime.now());
                } else {
                    hostingEnv.setCreated(env.getCreated());
                }
            }

            if(hostingEnv.getOcpSubDomain() != null && !isValidSubdomain(engagementUuid, hostingEnv.getOcpSubDomain())) {
                String message = String.format("Subdomain name %s is invalid", hostingEnv.getOcpSubDomain());
                throw new WebApplicationException(Response.status(409).entity(Map.of("lodestarMessage", message)).build());
            }
            fillOutHostingEnvironment(hostingEnv, engagement.getUuid(), engagement.getRegion(), engagement.getProjectId());
        }

        List<HostingEnvironment> existing = HostingEnvironment.getByEnagementUuid(engagementUuid);

        Diff diff = javers.compareCollections(existing, hostings, HostingEnvironment.class);

        if (diff.hasChanges()) {
            deleteHostingEnvs(engagementUuid);
            HostingEnvironment.persist(hostings);

            String commitMessage = createCommitMessage(diff);

            return String.format("%d||%s", engagement.getProjectId(), commitMessage);
        }

        return NO_UPDATE;
    }

    private long deleteHostingEnvs(String engagementUuid) {
        long deletedRows = HostingEnvironment.delete(ENGAGEMENT_UUID, engagementUuid);
        LOGGER.debug("Deleted {} rows for engagement {}", deletedRows, engagementUuid);

        return deletedRows;
    }

    private String createCommitMessage(Diff diff) {
        LOGGER.trace("count by type {}", diff.countByType());
        StringBuilder commit = new StringBuilder(commitMessagePrefix);
        commit.append(String.format("%s %s", getEmoji(), getEmoji()));

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
        String file = getFile(projectIdOrPath, hostingEnvFile);

        return file == null ? Collections.emptyList() : json.fromJson(file);
    }

    private String getFile(String projectIdOrPath, String filePath) {

        try {
            GitlabFile file = gitlabRestClient.getFile(projectIdOrPath, filePath, branch);
            file.decodeFileAttributes();
            LOGGER.trace("hosting file json for project {} {}", projectIdOrPath, file.getContent());
            return file.getContent();
        } catch (WebApplicationException ex) {
            if (ex.getResponse().getStatus() != 404) {
                throw ex;
            }
            LOGGER.error("No file {} found for {} {}", filePath, projectIdOrPath, ex.getMessage());
        } catch (RuntimeException ex) {
            LOGGER.error("Failure file {} {} {}", projectIdOrPath, filePath, ex.getMessage(), ex);
        }

        return null;
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

        List<HostingEnvironment> hostingEnvs = getHostingForEngagementUuid(uuidProjectMessageEmailName[0]);

        String content = json.toJson(hostingEnvs);
        GitlabFile file = GitlabFile.builder().filePath(hostingEnvFile).content(content)
                .commitMessage(uuidProjectMessageEmailName[2]).branch(branch)
                .authorEmail(uuidProjectMessageEmailName[3]).authorName(uuidProjectMessageEmailName[4]).build();
        file.encodeFileAttributes();

        String legacyFile = getFile(uuidProjectMessageEmailName[1], "engagement.json");
        String engagementFile = createLegacyJson(legacyFile, content);

        List<GitlabAction> actions = List.of(
                GitlabAction.builder().filePath(hostingEnvFile).content(content).build(),
                GitlabAction.builder().filePath("engagement.json").content(engagementFile).build()
        );

        GitlabCommit commit = GitlabCommit.builder().commitMessage(uuidProjectMessageEmailName[2]).branch(branch)
                .authorEmail(uuidProjectMessageEmailName[3]).authorName(uuidProjectMessageEmailName[4])
                .actions(actions).build();

        gitlabRestClient.createCommit(uuidProjectMessageEmailName[1], commit);

        LOGGER.debug("Gitlab hosting updated - {}", uuidProjectMessageEmailName[0]);

    }

    String createLegacyJson(String engagementJson, String hostingEnvsJson) {

        JsonElement element = gson.fromJson(engagementJson, JsonElement.class);
        JsonObject engagement = element.getAsJsonObject();

        element = gson.fromJson(hostingEnvsJson, JsonElement.class);

        engagement.add("hosting_environments", element);
        JsonObject sorted = new JsonObject();
        engagement.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(es -> sorted.add(es.getKey(), es.getValue()));

        return gson.toJson(sorted);
    }

    private String getEmoji() {
        String bear = "\ud83d\udc3b";

        int bearCodePoint = bear.codePointAt(bear.offsetByCodePoints(0, 0));
        int mysteryAnimalCodePoint = bearCodePoint + new java.security.SecureRandom().nextInt(144);
        char[] mysteryEmoji = { Character.highSurrogate(mysteryAnimalCodePoint),
                Character.lowSurrogate(mysteryAnimalCodePoint) };

        return String.valueOf(mysteryEmoji);
    }
}
