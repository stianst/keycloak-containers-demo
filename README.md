# A deep dive into Keycloak

This project contains the bits and pieces needed to run the DevNation session 
A deep dive into Keycloak on your own.

To run this demo you need to have Docker installed. Alternatively, you can 
reproduce most steps without Docker, but you need to adapt the steps accordingly.

## Setup

### Create a user defined network

To make it easy to connect Keycloak to LDAP create a user defined network:

    docker network create devnation-network

### Start Keycloak

There's an extended Keycloak image in this project to be able to remotely deploy
themes and providers.

First build the custom providers and themes with:

    mvn clean install

Then build the image with:
    
    docker build -t devnation-keycloak -f keycloak/Dockerfile .

Then run it with:

    docker run --name devnation-keycloak -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin \
        -p 8080:8080 --net devnation-network devnation-keycloak

### Start LDAP server

For the LDAP part of the demo we need an LDAP server running.

First build the image with:

    docker build -t devnation-ldap ldap
    
Then run it with:

    docker run --name devnation-ldap --net devnation-network devnation-ldap
    
### Start JS Console application

The JS Console application provides a playground to play with tokens.

First build the image with:

    docker build -t devnation-js-console js-console
    
Then run it with:

    docker run --name devnation-js-console -p 8000:80 devnation-js-console

## Creating the realm

Open [Keycloak Admin Console](http://localhost:8080/auth/admin/) and login with
username `admin` and password `admin`.

Create a new realm called `demo` (find the `add realm` button in the drop-down
in the top-left corner). Once created set a friendly Display name for the realm, for 
example `Demo SSO`.

Now create a client for the JS console by clicking on `clients` then `create`.

Fill in the following values:

* Client ID: `js-console`
* Root URL: `http://localhost:8000`

## Configuring SMTP server

In order to allow Keycloak to send emails we need to configure an SMTP server.
If you have a Google account you can use the Gmail SMTP server. 

First lets set a email address on the admin user so we can test email delivery.
From the drop-down in the top-left corner select `Master`. Go to `Users`, click
on `View all users` and select the `admin` user. 
Set the `Email` field to `<username>@gmail.com` (replace `<username>` with your
Google username). 

Now switch back to the `demo` realm, then click on `Realm Settings` then `Email`. 

Fill in the following values:

* Host: `smtp.gmail.com`
* From: `<username>@gmail.com`
* Enable SSL: `ON`
* Enable StartTLS : `ON`
* Enable Authentication: `ON`
* Username: `<username>@gmail.com`
* Password: `<password>`

Click `Save` and `Test connection`. Open your Gmail and check that you have
received an email from Keycloak.

## Enable user registration

Let's enable user self-registration and at the same time require users to verify
their email address.

Open the [Keycloak Admin Console](http://localhost:8080/auth/admin/) again. Click 
on `Realm settings` then `Login`.

Fill in the following values:

* User registration: `ON`
* Verify email: `ON`

To try this out open the [JS Console](http://localhost:8000). You will be
automatically redirected to the login screen. Click on `Register` 
and fill in the form. After registering you will be prompted to verify your email
by clicking on a link in an email sent to your email address.

## Getting users from LDAP

Now let's try to load users from LDAP into Keycloak.

Open the admin console again. Click on `User Federation`, select `ldap` from the
drop-down. 

Fill in the following values:

* Edit Mode: `WRITABLE`
* Vendor: `other`
* Connection URL: `ldap://devnation-ldap:389`
* Users DN: `ou=People,dc=example,dc=org`
* Bind DN: `cn=admin,dc=example,dc=org`
* Bind Credential: `admin`

Click on `Save` then click on `Synchronize all users`.

Now go to `Users` and click `View all users`. You will see two new users `bwilson` and
`jbrown`. Both these users have the password `password`.

Neither of these users have verified their email and since you don't have access
to their email you need to manually set the email verified by clicking on each
user and turning on `Email Verified`.

Try opening the [JS Console](http://localhost:8000) again and login with one of
these users.