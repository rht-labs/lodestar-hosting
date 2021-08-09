package com.redhat.labs.lodestar.hosting.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Tuple;

import org.javers.core.metamodel.annotation.DiffIgnore;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(indexes = { @Index(columnList = "projectId"), @Index(columnList = "engagementUuid") })
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostingEnvironment extends PanacheEntityBase {
    private static final String ENGAGEMENT_UUID = "engagementUuid";
    public static final String COLUMN_OCP_VERSION = "ocpVersion";

    @Id @GeneratedValue @DiffIgnore @JsonIgnore
    private Long id;
    
    private String uuid;

    @Column(nullable = false)
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime created;
    
    @Column(nullable = false)
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime updated;
    
    @Column(nullable = false)
    private String engagementUuid;
    
    @Column(nullable = false)
    Long projectId;
    
    @Column(nullable = false)
    @JsonProperty("environment_name")
    private String name;

    @JsonProperty("additional_details")
    private String additionalDetails;
    @JsonProperty("ocp_cloud_provider_name")
    private String ocpCloudProviderName;
    @JsonProperty("ocp_cloud_provider_region")
    private String ocpCloudProviderRegion;
    @JsonProperty("ocp_persistent_storage_size")
    private String ocpPersistentStorageSize;
    @JsonProperty("ocp_sub_domain")
    private String ocpSubDomain;
    @JsonProperty("ocp_version")
    private String ocpVersion;
    @JsonProperty("ocp_cluster_size")
    private String ocpClusterSize;
    @JsonIgnore
    private String ocpMajorVersion;
    @JsonIgnore
    private String ocpMinorVersion;
    
    private String region;

    public void generateId() {
        if (null == uuid) {
            uuid = UUID.randomUUID().toString();
        }
    }

    public static long countByEnagementUuid(String engagementUuid) {
        return HostingEnvironment.count(ENGAGEMENT_UUID, engagementUuid);
    }

    public static List<HostingEnvironment> getByEnagementUuid(String engagementUuid) {
        return HostingEnvironment.list(ENGAGEMENT_UUID, Sort.by("name").and("uuid"), engagementUuid);
    }

    public static List<HostingEnvironment> getHostingEnvironments(int page, int pageSize) {
        return findAll(Sort.by(ENGAGEMENT_UUID).and("name").and("uuid")).page(Page.of(page, pageSize)).list();
    }

    public static long countByEngagementSubset(List<String> engagementUuids) {
        return HostingEnvironment.count(ENGAGEMENT_UUID + " IN (?1)", engagementUuids);
    }

    public static List<HostingEnvironment> getByEngagementSubset(int page, int pageSize, List<String> engagementUuids) {
        return find(ENGAGEMENT_UUID + " IN (?1)", Sort.by(ENGAGEMENT_UUID).and("name").and("uuid"), engagementUuids)
                .page(Page.of(page, pageSize)).list();
    }
    
    public static long countSubDomainOmitEngagementUuid(String engagementUuid, String subdomain) {
        String query = "ocpSubDomain = ?2 AND " + ENGAGEMENT_UUID + " != ?1";
        return count(query, engagementUuid, subdomain);
    }

    public static Map<String, Long> getColumnRollup(Rollup rollup) {

        String query = String.format(
                "SELECT %s as %s, count(distinct %s) as total FROM HostingEnvironment GROUP BY ROLLUP(%s)", rollup.getColumn(),
                rollup.getColumn(), ENGAGEMENT_UUID, rollup.getColumn());

        return HostingEnvironment.getEntityManager().createQuery(query, Tuple.class).getResultStream()
                .collect(Collectors.toMap(
                        tuple -> ((String) tuple.get(rollup.getColumn())) == null ? "All"
                                : ((String) tuple.get(rollup.getColumn())),
                        tuple -> ((Number) tuple.get("total")).longValue()));

    }
    
    public static Map<String, Long> getColumnRollup(Rollup rollup, List<String> region) {

        String query = String.format(
                "SELECT %s as %s, count(distinct %s) as total FROM HostingEnvironment WHERE region in :region GROUP BY ROLLUP(%s)", rollup.getColumn(),
                rollup.getColumn(), ENGAGEMENT_UUID, rollup.getColumn());

        return HostingEnvironment.getEntityManager().createQuery(query, Tuple.class).setParameter("region", region).getResultStream()
                .collect(Collectors.toMap(
                        tuple -> ((String) tuple.get(rollup.getColumn())) == null ? "All"
                                : ((String) tuple.get(rollup.getColumn())),
                        tuple -> ((Number) tuple.get("total")).longValue()));

    }
    
    public enum Rollup {
        OCP_VERSION(COLUMN_OCP_VERSION), OCP_VERSION_MAJOR("ocpMajorVersion"), OCP_VERSION_MINOR("ocpMinorVersion");
        
        String column;
        
        Rollup(String column) {
            this.column = column;
        }
        
        public final String getColumn() {
            return column;
        }
        
    }

}
