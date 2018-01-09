
Administriers - leihs-admin  (working title)
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


## Compile Stylesheets

The Bootstrap sources require a relative recent version of SASS and the one
provided with lein-sassy 1.0.8 (time of writing) does not suffice.

    sass --watch resources/all/public/admin/css/site.sass:resources/all/public/admin/css/site.css

Minified css saves only about 10% of the size. We deemed that not worth to mess
with it. In production mode effective caching is enabled.


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

* Refactoring `is_admin`, change code in legacy (should be "easy")
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
* `sign_in_enabled` see auth system refactoring
* set and use user image, needs adjustments in legacy
* delete user incl. data transfer

## New Password Sign-in

* new password authentication
* sign-in via session, sign-out
* new setting server secret as password

## Initial Admin (preliminary)


TODOs
-----

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

* protect and redirect initial-admin resource


### Sync Service & Code

For ZHdK and also proof of concept.
