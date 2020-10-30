Leihs Admin
============================================

Development
-----------

## Run The Application

### Backend

    lein repl

In the REPL

    (-main "run")

Inspect and set parameters

    (-main "run" "-h")


### Frontend

    lein figwheel


## Compiled Stylesheets

    sh scripts/prepare-shared-ui.sh


Building and Testing
--------------------

## Build

    lein do clean, uberjar


## Testing

1. build the application `lein do clean, uberjar`
2. start the application `java -jar target/leihs-admin.jar run`
3. run specs in a second terminal `bundle exec rspec spec/`


## CI

This is not possible yet. It seems that the openjdk 9 on ubuntu 16.04 is outdated.



Identified Problems
-------------------

* partially insane database schema,
    needs to be fixed before deploy in particular wrt. users table
* dirty data and missing constraints on user table
* "flexible" authentication_systems table
* "outdated" password handling
*  misuse of roles for is_admin
* "flexible" languages table


To be Discussed
---------------

* Refactoring/decomplect authentication systems
* Refactoring/decomplect language settings
* Refactoring/decomplect settings table
* Refactoring/e-mail system

DONE
----

## API-Tokens UI

## Users UI and resouces

* users/user API + UI inc. search/filter

changes so far:

* `is_admin` user property, needs minor roles refactoring in legacy
* `account_enabled` see auth system refactoring
* set and use user image, needs adjustments in legacy
* delete user incl. data transfer

## New Password Sign-in

* new password authentication
* sign-in via session, sign-out

## Initial Admin (preliminary)


TODOs
-----

* Add constraint: account_enabeled requires non null email
* try to fix text-warning visibility problem with css

### External and PM

* Extend the ZAPI to export the private badge image

### New Code and Refactorings

* `settings`
* audits
* delegations
* authentication system (complete rewrite): PW, Email, AAI (?)
* support sending mails: https://github.com/drewr/postal

* `user_groups`,
* `users_groups`,

Last two, can come later. The other have interdependencies in some way.


#### Some Details (noted so I don't forget them)

* routes authorization everywhere!

* authorization: api-tokes access via session only; api-tokens should not be
    modifiable via api-token authentication

* on sign-in: delete expired user_sessions; otherwise they will hang
    around for ever if concurrent sessions are allowed


##### Possible Tickets

* Refactoring and clean-up authentication systems in leihs-legacy:
  * remove tables and all/most related controllers, models
  * preserve / rewrite a very simple password sign-in for test purposes only
  * legacy & admin-API: setup integration tests and add spec for cross service pw-sign in



### Sync Service & Code

For ZHdK and also proof of concept.


# ZAPI

## Example

curl -u 'API-KEY:' resource

## Resources

https://zapi.zhdk.ch/v1/user-group
https://zapi.zhdk.ch/v1/user-group/documentation
https://zapi.zhdk.ch/v1/person
https://zapi.zhdk.ch/v1/person/documentation


## API Keys

-----BEGIN PGP MESSAGE-----
Comment: GPGTools - http://gpgtools.org

hQIMA8ScbQq5Og78AQ//bJhv/dngXTMP2Z/MpQRNz9jSHjSGTOSrucwi4ufR1upw
M4ndepfhI9/U8H8b9YvajHcRuT2T2RfvLNggpjqtc/8RJrIP5GglRDvu317q/gco
31s9BbqwKa4MbCg9t3uqPsQrFW9D3XZKAkLBhu+vtJE+KzPuaeIUg/SNbJcdN/B+
aUBsnVyleVmCjYwLPH30q5DFBp/ZNb79/XzgPid4b4Z99wmW6561FgTEZOjEZDXb
3ZcrtpVJuhbtOTJ9qL0DQOlwNV7vWuePGUbJPm2aZZdp6sMeArUvzyfAfOoVaIiO
PfUdAgl/tfdhzETs33WOnkDUHexiniNReLW26LmZS1qfOL9DwqhcZpuCm2pCykGW
21uu4RD1gvakQFmb9nmAEbcYviEtUDPdaFv2xInATPVm6G8MsaPrLxoqN0a7JuIW
6OC8/gq7u+NN0x54/gYsIGwaIAc2O8LtVxVv59Q35pGzItvZHFLRrnnFnyMPlIBq
vI0P7KlWWV8ysn0C7M442TX4X8WR00x/DT674R80SYWatmBArHADYchlqrF8msj5
BUzRWScULR5cZYlZtvalfpQPAu4ivCk+p0KqUapGf6y87Jzw0VSRMu9pIlYRFTFI
3TUKMaOF/DRd02w5Td9KUOZD0bvJq80dIET53jxT+PmoM/5tmpsFsayWcFeylrDS
uQEF0qeaBssTtKnfBUB6ZweK6AZUFXBNQo5/Xa8Iwq3yBc8Hs99ntIlzu2kbhaV/
An42oCarp2s5KC4Wqwv81Ih2Qr5oOE2s9RmgtMg0O1/D6tQx+30DNY53BXfJo0jX
GNB8P4tPxD8VEtZ1Hx4sK/RJb7fN5tRtrGjYNv0p6+W1y0IYN21yq+FrbLopxPEp
WIDbg6hFVPYzghyDUi4tH1jk03AIgt1TpuBbsO84ydPPsKoPiFXqMAg/
=Lftp
-----END PGP MESSAGE-----





# Authentication

## LDAP

### Security

Writing a secure LDAP authentication is not simple!

OWASP Recommendation

> Properties of Safe LDAP Client APIs

> Many developers (perhaps the majority) using LDAP client libraries are trying
> to accomplish a simple task: authenticate users to their application by
> leveraging the directory server's password store. An effective way to do this
> can be to simply attempt an LDAP bind as the user that is trying to
> authenticate, using the password provided by the user. This process often
> does not require the use of LDAP filter expressions and avoids the risk of
> search filter injection.

https://www.owasp.org/index.php/Projects/OWASP_Framework_Security_Project/Secure_LDAP_API_Standard#Documents_LDAP_Bind_Authentication_Without_Filter_Queries


## E-Mail



