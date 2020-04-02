# A deep dive into Keycloak

This project contains a DIY deep dive into Keycloak.

The steps included here requires Docker (or Podman). It should also be possible to replicate the steps without Docker by
adapting the steps accordingly.


## Start containers

### Create a user defined network

To make it easy to connect Keycloak to LDAP and the mail server create a user defined network:

    docker network create demo-network

### Start Keycloak

We're going to use an extended Keycloak image that includes a custom theme and some custom providers.

First, build the custom providers and themes with:

    mvn clean install

Then build the image with:
    
    docker build -t demo-keycloak -f keycloak/Dockerfile .

Finally run it with:

    docker run --name demo-keycloak -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin \
        -p 8080:8080 --net demo-network demo-keycloak

### Start LDAP server

For the LDAP part of the demo we need an LDAP server running.

First build the image with:

    docker build -t demo-ldap ldap
    
Then run it with:

    docker run --name demo-ldap --net demo-network demo-ldap
    
### Start mail server

In order to allow Keycloak to send emails we need to configure an SMTP server. MailHog provides an excellent email
server that can used for testing.

Start the mail server with:

    docker run -d -p 8025:8025 --name demo-mail --net demo-network mailhog/mailhog
    
### Start JS Console application

The JS Console application provides a playground to play with tokens issued by Keycloak.

First build the image with:

    docker build -t demo-js-console js-console
    
Then run it with:

    docker run --name demo-js-console -p 8000:80 demo-js-console



## Creating the realm

Open [Keycloak Admin Console](http://localhost:8080/auth/admin/). Login with
username `admin` and password `admin`.

Create a new realm called `demo` (find the `add realm` button in the drop-down
in the top-left corner). 

Once created set a friendly Display name for the realm, for 
example `Demo SSO`.

## Create a client

Now create a client for the JS console by clicking on `clients` then `create`.

Fill in the following values:

* Client ID: `js-console`
* Click `Save`

On the next form fill in the following values:

* Valid Redirect URIs: `http://localhost:8000/*`
* Web Origins: `http://localhost:8000`

## Configuring SMTP server

First lets set a email address on the admin user so we can test email delivery.

From the drop-down in the top-left corner select `Master`. Go to `Users`, click
on `View all users` and select the `admin` user. Set the `Email` field to `admin@localhost`.

Now switch back to the `demo` realm, then click on `Realm Settings` then `Email`. 

Fill in the following values:

* Host: `demo-mail`
* From: `keycloak@localhost`

Click `Save` and `Test connection`. Open your http://localhost:8025 and check that you have
received an email from Keycloak.

## Enable user registration

Let's enable user self-registration and at the same time require users to verify
their email address.

Open the [Keycloak Admin Console](http://localhost:8080/auth/admin/). 

Click on `Realm settings` then `Login`.

Fill in the following values:

* User registration: `ON`
* Verify email: `ON`

To try this out open the [JS Console](http://localhost:8000). 

You will be automatically redirected to the login screen. Click on `Register` 
and fill in the form. After registering you will be prompted to verify your email
by clicking on a link in an email sent to your email address.

## Adding claims to the tokens

What if your applications want to know something else about users? Say you want to have
an avatar available for your users.

Keycloak makes it possible to add custom attributes to users as well as adding custom
claims to tokens.

First open `Users` and select the user you registered earlier. Click on attributes and the following attribute:

* Key: `avatar_url`
* Value: `https://www.keycloak.org/resources/images/keycloak_logo_480x108.png`

Click `Add` followed by `Save`.

Now Keycloak knows the users avatar, but the application also needs access to this. We're
going to add this through Client Scopes.

Click on `Client Scopes` then `Create`.

Fill in the following values:

* Name: `avatar`
* Consent Screen Text: `Avatar`

Click on `Save`. 

Click on `Mappers` then `Create`. Fill in the following values:

* Name: `avatar`
* Mapper Type: `User Attribute`
* User Attribute `avatar_url`
* Token Claim Name: `avatar_url`
* Claim JSON Type: `String`

Click `Save`.

Now we've created a client scope, but we also need to asign it to the client. Go to
`Clients` and select `js-console`. Select `Client Scopes`.

We're going to add it as a `Default Client Scope`. So select the `avatar` here and click
`Add selected`. As it's a default scope it is added to the token by default, if it's
set as an optional client scope the client has to explicitly request it with the `scope` 
parameter.

Now go back to the JS Console and click `Refresh`.

## Require consent for the application

So far we've assumed the JS Console is an internal trusted application, but what if it's
a third party application? In that case we probably want the user to grant access to what the application wants to have 
access to.

Open the [Keycloak Admin Console](http://localhost:8080/auth/admin/). 

Go to `Clients`, select `JS Console` and turn on `Consent Required`. 

Go back to the [JS Console](http://localhost:8000) and click `Login` again. Now Keycloak will prompt the user to grant 
access to the application.

You may want to turn this off again before continuing.

# Roles and groups

Keycloak has supports for both roles and groups.

Roles can be added realm-wide or to specific applications. There is also support for 
composite roles where a role can be a composite of other roles. This allows for instance
creating a `default` role that can be added to all users, but in turn easily managing what
roles all the users with the `default` role will have.

Groups have a parent/child relationship where a child inherits from its parent. Groups can
be mapped to roles, have attributes or just added directly to the token for your application
to resolve its meaning.

Let's start by creating a role and see it in the token.  

Open the [Keycloak Admin Console](http://localhost:8080/auth/admin/). 

Click on `Roles` and `Add Role`. Set the Role Name to `user` and click `Save`.

Now click on `Users` and find the user you want to login with. Click on `Role Mappings`. 
Select `user` from Available roles and click `Add selected`.

Go back to the [JS Console](http://localhost:8000) and click `Refresh`, then `Access Token JSON`.
Notice that there is a `realm_access` claim in the token that now contains the user role.

Next let's create a Group. Go back to the [Keycloak Admin Console](http://localhost:8080/auth/admin/).

Click on `New` and use `mygroup` as the Name. Click on `Attributes` and add key `user_type` with value `consumers`.

Now let's make sure this group and the claim is added to the token. Go to `Client Scopes` and click `Create`.
For the name use `myscope`. 

Click on `Mappers`. Click on `Create`.

Fill in the following values:

* Name: `groups`
* Mapper Type: `Group Membership`
* Token Claim Name: `groups`

Click `Save` then go back to `Mappers` and click `Create` again.

Fill in the following values:

* Name: `type`
* Mapper Type: `User Attribute`
* User Attribute: `user_type`
* Token Claim Name: `user_type`
* Claim JSON Type: `String`

Find the `js-console` client again and add the `myclaim` as a default client scope.

Go back to the [JS Console](http://localhost:8000) and click `Refresh`, then `Access Token JSON`.
Notice that there is a `groups` claim in the token as well as a `user_type` claim.

## Users from LDAP

Now let's try to load users from LDAP into Keycloak.

Open the admin console again. Click on `User Federation`, select `ldap` from the
drop-down. 

Fill in the following values:

* Edit Mode: `WRITABLE`
* Vendor: `other`
* Connection URL: `ldap://demo-ldap:389`
* Users DN: `ou=People,dc=example,dc=org`
* Bind DN: `cn=admin,dc=example,dc=org`
* Bind Credential: `admin`
* Trust Email: `ON`

Click on `Save` then click on `Synchronize all users`.

Now go to `Users` and click `View all users`. You will see two new users `bwilson` and
`jbrown`. Both these users have the password `password`.

Try opening the [JS Console](http://localhost:8000) again and login with one of
these users.

## Users from GitHub

Now that we have users in Keycloak as well as loading users from LDAP let's get users
from an external Identity Provider. For simplicity we'll use GitHub, but the same 
approach works for a corporate identity provider and other social networks.

Open the [Keycloak Admin Console](http://localhost:8080/auth/admin/). 

Click on `Identity Providers`. From the drop-down select `GitHub`. Copy the value 
in `Redirect URI`. You'll need this in a second when you create a new OAuth 
application in GitHub.

In a new tab open [Register a new OAuth application](https://github.com/settings/applications/new).

Fill in the following values:

* Application name: Keycloak
* Homepage URL: <Redirect URI from Keycloak Admin Console>
* Authorization callback URL: <Redirect URI from Keycloak Admin Console>

Now copy the value for `Client ID` and `Client Secret` from GitHub to the Keycloak
Admin Console.

We also want to add a mapper to retrieve the avatar from GitHub. Click on
`Mappers` and `Create`.

Fill in the following values:

* Name: `avatar`
* Mapper Type: `Attribute Importer`
* Social Profile JSON Field Path: `avatar_url`
* User Attribute Name: `avatar_url`

Try opening the [JS Console](http://localhost:8000) again and instead of providing
a username or password click on `GitHub`.

Notice how it automatically knows your name and also has your avatar.

## Style that login

Perhaps you don't want the login screen to look like a Keycloak login screen, but rather 
add a splash of your own styling to it?

The Keycloak Docker image we built earlier actually contains a custom theme, so we 
have one ready to go.

Open the [Keycloak Admin Console](http://localhost:8080/auth/admin/). 

Click on `Realm Settings` then `Themes`. In the drop-down under `Login Theme` select
`sunrise`.

Try opening the [JS Console](http://localhost:8000) to login a take in the beauty of the
new login screen!

You may want to change it back before you continue ;).

## Keys and Signing Algorithms

By default Keycloak signs tokens with RS256, but we have support for other signing
algorithms. We also have support for a single realm to have multiple keys.
 
It's even possible to change signing algorithms and signing keys transparently without
any impact to users.

Let's first try to change the signing algorithm for the JS console client.

First let's see what algorithm is currently in use. Open the [JS Console](http://localhost:8000), 
login, then click on `ID Token`. This will display a rather cryptic string, which is
the encoded token. Copy this value, making sure you select everything.

In a different tab open the [JWT validation extension](http://localhost:8080/auth/realms/demo/jwt). 
This is a custom extension to Keycloak that allows decoding a token as well as verifying the
signature of the token.

What we're interested is in the header. The field `alg` will show what signing algorithm is
used to sign the token. It should show `RS256`.

Now let's change this to the new cool kid on the block, `ES256`.

Open the [Keycloak Admin Console](http://localhost:8080/auth/admin/) in a new tab. Make sure you
keep the JS Console open as we want to show how it gets new tokens without having to re-login. 

Click on `Clients` and select the `js-console` client. Under `Fine Grained OpenID Connect Configuration`
switch `Access Token Signature Algorithm` and `ID Token Signature Algorithm` to `ES256`.

Now go back to the JS Console and click on `Refresh`. This will use the Refresh Token to 
obtain new updated ID and Access tokens.

Click on `ID Token`, copy it and open the [JWT validation extension](http://localhost:8080/auth/realms/demo/jwt)
again. Notice that now the tokens are signed with `ES256` instead of `RS256`.

While you're looking at the `ID Token` take a note of the kid, try to remember the first few characters.
The `kid` refers to the keypair used to sign the token.

Go back to the [Keycloak Admin Console](http://localhost:8080/auth/admin/). Go to
`Realm Settings` then `Keys`. What we're going to do now is introduce a new set of active keys and
mark the previous keys as no longer the active keys. 

Click on `Providers`. From the drop-down select `ecdsa-generated`. Set the priority to `200` and click
Save. As the priority is higher than the current active keys the new keys will be used next time
tokens are signed.

Now go back to the JS Console and clik on `Refresh`. Copy the token to the [JWT validation extension](http://localhost:8080/auth/realms/demo/jwt).
Notice that the `kid` has now changed.

What this does is provide a seamless way of changing signatures and keys. Currently logged-in users
will receive new tokens and cookies over time and after a while you can safely remove the old keys
without affecting any logged-in users.

## Sessions

Make sure you have the [JS Console](http://localhost:8000) open in a tab and you're logged-in.
Open the [Keycloak Admin Console](http://localhost:8080/auth/admin/) in another tab.

Find the user you are logged-in as and click on `Sessions`. Click on `Logout all session`.

Go back to the [JS Console](http://localhost:8000) and click `Refresh`. Notice how you are 
now longer authenticated.

Not only can admins log out users, but users themselves can logout other sessions from the
account management console.

## Events

Open the [Keycloak Admin Console](http://localhost:8080/auth/admin/). Click on `Events`
and `Config`. Turn on `Save Events` and click `Save`.

Go back to the [JS Console](http://localhost:8000) and click `Refresh`. Logout. Then
when you log in use the wrong password, then login with the correct password.

Go back to the `Events` in the [Keycloak Admin Console](http://localhost:8080/auth/admin/)
and notice how there are now a list of events.

Not only can Keycloak save these events to be able to display them in the admin console
and account management console, but you can develop your own event listener that can
do what you want with the events.

# Custom stuff

Keycloak has a huge number of SPIs that allow you to develop your own custom providers.
You can develop custom user stores, protocol mappers, authenticators, event listeners
and a large number of other things. We have about 100 SPIs so you can customize a lot!

When we previously deployed Keycloak we also included a custom authenticator that enables
users to login through `email`.

To enable this open the [Keycloak Admin Console](http://localhost:8080/auth/admin/). 
Click on `Authentication`.

Click on `Copy` to create a new flow based on the `browser` flow. Use the name `My Browser Flow`.

Click on `Actions` and `Delete` for `Username Password Form` and `OTP Form`.

Click on `Actions` next to `Browser-email forms`. Then click on `Add execution`.
Select `Magic Link` from the list. Once it's saved select `Required` for the `Magic Link`.

Now to use this new flow when users login select `Bindings` and select `My Browser Flow` for the
`Browser flow`.

Open the [JS Console](http://localhost:8000) and click Logout. For the email enter your email
address and click `Log In`. Open your email and you should have a mail with a link which will
authenticate you and bring you to the JS Console.

Now let's add WebAuthn to the mix. Open the [Keycloak Admin Console](http://localhost:8080/auth/admin/).
Go back to the `Browser-email` flow. Click `Actions` and `Add execution`. Select `WebAuthn Authenticator`. Then
mark it as `Required`. You also need to register the WebAuthn required action. 

Select `Required Actions`, `Register`, then select `WebAuthn Register` and click `Ok`. 

Open the [JS Console](http://localhost:8000) and click Logout. Login again. After you've done the
email based login you will be prompted to configure WebAuthn. You'll need a WebAuthn security key to try this out.

## Cool stuff we didn't cover!

### Clustering and Caching

Keycloak leverages Infinispan to cache everything loaded from the database and user stores
like LDAP. This removes the need to go to the database for every request.

Infinispan also enables us to provide a clustered mode where user sessions are distributed
throughout the cluster. Enabling load balancing as well as redundancy.

Not only does Infinispan allow us to cluster within a single data center, but we also have
support for multiple data centers. This works, but there are a few improvements that can be made
here.

### Authorization Services

Keycloak provides a way to centrally manage permissions for resources as well as support for
UMA 2.0 that enables users themselves to manage permissions to their own resources.

Hopefully, soon we will have a DevNation Live session focusing only on this feature.

### Dynamic Client Registration

Keycloak has a very powerful dynamic client registration service that allows users and applications
themselves to register clients through a REST API. This supports clients defined in Keycloak's
own client representation, standard OpenID Connect client format as well as SAML 2.0 client format.

It is also possible to define policies on what is permitted. For example you can restrict this
service to specific IP ranges, automatically require consent and a lot of other policies. You can
also develop your own custom policies. 

### OpenID Connect and SAML 2.0

Keycloak provides support for both OpenID Connect and SAML 2.0. There's even community 
maintained extensions for WS-Fed and CAS.

### Adapters and the Keycloak Gatekeeper

If you missed the last DevNation Live session on Keycloak make sure to watch the recording.
We covered how you can easily secure your applications with Keycloak.

### Loads more...

For more information about Keycloak check out our [homepage](https://www.keycloak.org) and 
our [documentation](https://www.keycloak.org/documentation.html).