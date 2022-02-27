/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.experimental.magic;
import java.util.HashMap;
import org.keycloak.email.freemarker.beans.ProfileBean;

import java.util.Map;
import java.util.Collections;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.theme.FreeMarkerUtil;

import javax.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MagicLinkFormAuthenticator extends AbstractUsernameFormAuthenticator implements Authenticator {

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String email = formData.getFirst("email").trim();
        System.out.println(email);
        System.out.println(context.getRealm().getName());
        String realmName = context.getRealm().getName() ;

        // Uncomment this block if you want to filter       //
        // the keycloack access to a specific email domaine //

        /*if (
          (!email.endsWith("@gruion.com") && realmName.equals("gruion.com"))
        ){
          context.challenge(context.form().createForm("view-error-domain.ftl"));
          return;
        }else{*/
          UserModel user = context.getSession().users().getUserByEmail(email, context.getRealm());
          if (user == null) {
              // Register user
              user = context.getSession().users().addUser(context.getRealm(), email);
              user.setEnabled(true);
              user.setEmail(email);

              // Uncomment the following line to require user to update profile on first login
              // user.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
          }

          String key = KeycloakModelUtils.generateId();
          context.getAuthenticationSession().setAuthNote("email-key", key);

          String link = KeycloakUriBuilder.fromUri(context.getRefreshExecutionUrl()).queryParam("key", key).build().toString();
          try {

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("realmName", realmName);
            attributes.put("link", link);
            FreeMarkerEmailTemplateProvider emailTemplateProvider = new FreeMarkerEmailTemplateProvider(context.getSession(), new FreeMarkerUtil());
            emailTemplateProvider.setRealm(context.getRealm());
            emailTemplateProvider.setUser(user);
            String subject = "Login to " + realmName;
            emailTemplateProvider.send(subject,Collections.emptyList(), "email-magic-link.ftl", attributes);

          } catch (Exception e) {
              context.challenge(context.form().createForm("view-error.ftl"));
              e.printStackTrace();
              return;
          }

          context.setUser(user);
          context.challenge(context.form().createForm("view-email.ftl"));

        //}
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String sessionKey = context.getAuthenticationSession().getAuthNote("email-key");
        if (sessionKey != null) {
            String requestKey = context.getUriInfo().getQueryParameters().getFirst("key");
            if (requestKey != null) {
                if (requestKey.equals(sessionKey)) {
                    context.success();
                } else {
                    context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
                }
            } else {
                context.challenge(context.form().createForm("view-email.ftl"));
            }
        } else {
            context.challenge(context.form().createForm("login-email-only.ftl"));
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }

}
