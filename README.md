# LodeStar Hosting

This project manages hosting environment data for LodeStar.

The API is document via swagger and is available at `/q/swagger-ui`

----

## Configuration

The following environment variables are available:

### Logging
| Name | Default | Description|
|------|---------|------------|
| ENGAGEMENT_API_URL | http://git-api:8080 | The url to get engagement data |
| GITLAB_API_URL | https://acmegit.com | The url to Gitlab |
| GITLAB_TOKEN | t | The Access Token for Gitlab |
| LODESTAR_LOGGING | DEBUG | Logging to the base source package | 
| HOSTING_POSTGRESQL_USER | | The db user | 
| HOSTING_POSTGRESQL_PASSWORD | | The db password |
| HOSTING_POSTGRESQL_URL | | The jdbc url to the db |

## Deployment

See the deployment [readme](./deployment) for information on deploying to a OpenShift environment

## Running the application locally

### Postgresql 

A postgres database that is needed for development Is provided via [Testcontainers](https://www.testcontainers.org/). Testcontainers will also be initiated during tests. For deployment to a non-dev environment see the application.properties file.

### Local Dev

You can run your application in dev mode that enables live coding using:

```
export GITLAB_API_URL=https://gitlab.com/ 
export GITLAB_TOKEN=token
export ENGAGEMENT_API_URL=https://git-api.test.com 
mvn quarkus:dev
```

In dev mode the application uses [Testcontainers](https://www.testcontainers.org/) that automatically spins up a postgresql container so there is no need to configure a database. Docker is needed.


> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

### Testing

Tests also leverage [Testcontainers](https://www.testcontainers.org/) and will automatically spin up a posgresql container.

```
mvn test
```