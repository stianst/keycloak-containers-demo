# A deep dive into Keycloak

This project contains the bits and pieces needed to run the DevNation session 
A deep dive into Keycloak on your own.

To run this demo you need to have Docker installed. Alternatively, you can 
reproduce most steps without Docker, but you need to adapt the steps accordingly.

## Setup

### Start Keycloak

There's an extended Keycloak image in this project to be able to remotely deploy
themes and providers.

First build this image with:
    
    docker build -t devnation-keycloak keycloak

Then run it with:

    docker run --name keycloak -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin \
        -p 8080:8080 -p 9990:9990 devnation-keycloak

### Start LDAP server

For the LDAP part of the demo we need an LDAP server running.

First build this image with:

    docker build -t devnation-ldap ldap
    
Then run it with:

    docker run --name devnation-ldap -p 1389:389 devnation-ldap
    
### Start JS Console application

The JS Console application provides a playground to play with tokens.

First build this image with:

    docker build -t devnation-js-console js-console
    
Then run it with:

    docker run --name devnation-js-console -p 8000:80 devnation-js-console

## Creating the realm

Open [Keycloak Admin Console](http://localhost:8080/auth/admin/) 