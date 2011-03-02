DEPLOYING THE APP
=================

postupload is a Java EE application. It runs in an application server container
rather than being a stand-alone program. 

SUPPORTED APPLICATION SERVERS
=============================

Currently, Oracle Glassfish (3.1 or newer) Full Profile is the only supported
application server, though it may work on others. Glassfish 3.0 or older are
not supported and will not work.

postupload is written to the Java EE 6 Full Profile standard. Web Profile is
not sufficient, because it lacks JAX-RS (REST services) and JavaMail.  A Web
Profile container with JAX-RS and JavaMail added should work, but is not
tested.

You can get Glassfish 3.1 as part of the NetBeans 7 beta download, or directly
from Oracle:

  http://glassfish.java.net/public/downloadsindex.html

or (once Oracle break that link too):

  http://www.oracle.com/technetwork/middleware/glassfish/downloads/index.html


COMPILING
=========

While postupload is developed using NetBeans, you don't need NetBeans to use it
or to modify it.

postupload is built with maven 3. Assuming your `mvn' is on the PATH, you should
be able to cd to the `postupload' directory and run:

  mvn war:war

to produce target/postupload.war

If this is the first time you've built postupload, you will see a lot of maven plugins,
jar files, etc being downloaded. This is quite normal. They're cached on your computer
in ~/.m2 (unless configued otherwise) and will be re-used for future builds.

You can get Maven 3 here:

  http://maven.apache.org/download.html 


DEPLOYING TO AN APPLICATION SERVER
==================================

Glassfish
---------
Deploy postupload.war using the admin console; or

./asadmin deploy --contextroot postupload --upload=true target/postupload.war

Tomcat 7
--------
[TODO]

JBoss AS 6
----------
[TODO]


CONFIGURING
===========

Configuration is done via a web interface at /faces/admin/configure.xhtml .

Access is controlled by the application server. The web app's configuration declares
that this web page is only accessible to users in the `POSTUPLOAD_ADMIN' role. This role is
mapped to particular user names or user groups by the application server configuration.

The application server can use all sorts of different authentication methods transparently
to the application. You can authenticate users using password files, LDAP, Kerberos, PAM 
(if running as root - eek!), or whatever your app server supports.

Only you know how your user database is structured, so you must map the application's user
roles to something that is meaningful for your local setup. This app is nice and simple,
because it only has one role, the `POSTUPLOAD_ADMIN' role. Map it to a user or group in your local
authentication system that makes sense.

Glassfish 3.1
-------------

Glassfish provides a default `file' realm that authenticates against a simple
glassfish-specific password file. This is used by default, unless any other
security realms are provided. You can manage this default file authentication
realm from the admin console, via 
  Configurations->server-config->Security->Realms->file
and using the "Manage Users" button (top).

Because postupload's requirements are so simple (just a single admin user) the
file realm is a reasonable choice. For anything less trivial you'd want to use
Glassfish's support for LDAP/Kerberos/etc authentication.

You must map the `POSTUPLOAD_ADMIN' role to one or more local users or groups
in order for access to be permitted. The easiest way to do this is to enable
"Default Principal To Role Mapping" in 
  Configurations->server-config->Security
then add user(s) who should have access to the POSTUPLOAD_ADMIN group. If
you want to grant access to all allowed users, add POSTUPLOAD_ADMIN to the
"assign groups" list in:
  Configurations->server-config->Security->Realms->file
so all users get membership of the group.

If you need anything more sophisticated, you'll need to mess with role mappings
in the application's glassfish-web.xml .

(Note: If you change "Default Principal To Role Mapping" you must re-deploy
postupload for the settings change to take effect.)


Tomcat 7
--------

[TODO]

JBoss AS 6
----------

[TODO]



RUNNING
=======

To run postupload, open its base URL in your web browser. The exact URL will
depend on how you configured your application server and what context root you
used for postupload. Assuming your application server is listening on 'localhost:8080'
(as Glassfish and JBoss AS do by default) you should be able to open:

  http://localhost:4848/postupload/

You can test your configuration by loading 

  http://localhost:4848/postupload/faces/admin/configtest.xhtml

At this point, the configuration test should report a problem with JavaMail.



JAVAMAIL
========

postupload requires a JavaMail resource from the container.
It doesn't currently support creating its own JavaMail session.
Though this would be trivial to add, it'd need associated configuration
machinery that I don't particularly want to implement when the container
can do it already.

The JavaMail resource postupload expects to find is called "mail/smtp". You
may remap that however you like, so long as it the resource appears to be
named that way to postupload.


Glassfish
---------
In the admin console, create a new JavaMail resource named "mail/smtp" ; or run

asadmin create-javamail-resource \
	--mailhost SERVERNAME --mailuser glassfish \
	--fromaddress glassfish@localnet 'mail/smtp'

... adapting the default from address and default user name as appropriate
for your mail server. See "asadmin create-javamail-resource -h" and the
javamail docs for additional properties you might need to set for your
environment. Postupload will not use the default fromaddress.

Tomcat 7
--------
[TODO] requires messing with context.xml

JBoss AS 6
----------
[TODO]
