FROM jboss/keycloak:9.0.2 as builder

USER root
RUN microdnf update -y && microdnf install -y maven

USER 1000

COPY pom.xml .
COPY magic-link/ ./magic-link/
COPY themes/ ./themes/
COPY token-validation/ ./token-validation/

RUN mvn clean || :
RUN mvn install

FROM jboss/keycloak:9.0.2

COPY --from=builder magic-link/target/magic-link.jar /opt/jboss/keycloak/standalone/deployments/
RUN touch /opt/jboss/keycloak/standalone/deployments/magic-link.jar.dodeploy

COPY --from=builder themes/target/themes.jar /opt/jboss/keycloak/standalone/deployments/
RUN touch /opt/jboss/keycloak/standalone/deployments/themes.jar.dodeploy

COPY --from=builder token-validation/target/token-validation.jar /opt/jboss/keycloak/standalone/deployments/
RUN touch /opt/jboss/keycloak/standalone/deployments/token-validation.jar.dodeploy
