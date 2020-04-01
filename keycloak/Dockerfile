FROM jboss/keycloak:9.0.2

COPY magic-link/target/magic-link.jar /opt/jboss/keycloak/standalone/deployments/
RUN touch /opt/jboss/keycloak/standalone/deployments/magic-link.jar.dodeploy

COPY themes/target/themes.jar /opt/jboss/keycloak/standalone/deployments/
RUN touch /opt/jboss/keycloak/standalone/deployments/themes.jar.dodeploy

COPY token-validation/target/token-validation.jar /opt/jboss/keycloak/standalone/deployments/
RUN touch /opt/jboss/keycloak/standalone/deployments/token-validation.jar.dodeploy
