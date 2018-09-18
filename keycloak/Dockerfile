FROM jboss/keycloak

RUN /opt/jboss/keycloak/bin/add-user.sh admin admin --silent

CMD ["-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]