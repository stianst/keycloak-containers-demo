package org.keycloak.experimental.token;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class TokenValidatorFactory implements RealmResourceProviderFactory, RealmResourceProvider {

    private KeycloakSession session;

    public RealmResourceProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    public void init(Config.Scope config) {
    }

    public void postInit(KeycloakSessionFactory factory) {
    }

    public void close() {
    }

    public String getId() {
        return "jwt";
    }

    public Object getResource() {
        return new TokenResource(session);
    }

}
