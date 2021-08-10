DROP TABLE IF EXISTS HostingEnvironment;

create table HostingEnvironment (
    id int8 not null,
    additionalDetails varchar(255),
    created timestamp not null,
    engagementUuid varchar(255) not null,
    name varchar(255) not null,
    ocpCloudProviderName varchar(255),
    ocpCloudProviderRegion varchar(255),
    ocpClusterSize varchar(255),
    ocpMajorVersion varchar(255),
    ocpMinorVersion varchar(255),
    ocpPersistentStorageSize varchar(255),
    ocpSubDomain varchar(255),
    ocpVersion varchar(255),
    projectId int8 not null,
    region varchar(255),
    updated timestamp not null,
    uuid varchar(255),
    primary key (id)
);

DROP INDEX IF EXISTS project_index;
DROP INDEX IF EXISTS subdomain_index;
DROP INDEX IF EXISTS engagement_uuid_index;
DROP INDEX IF EXISTS region_index;
DROP SEQUENCE IF EXISTS hibernate_sequence;

create index project_index on HostingEnvironment (projectId);
create index subdomain_index on HostingEnvironment (ocpSubDomain);
create index engagement_uuid_index on HostingEnvironment (engagementUuid);
create index region_index on HostingEnvironment (region, ocpVersion, ocpMajorVersion, ocpMinorVersion);
create sequence hibernate_sequence start 1 increment 1;