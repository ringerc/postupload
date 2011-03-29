DEPLOYING THE APP
=================

postupload is a Java EE application. It runs in an application server container
rather than being a stand-alone program.  It is written for, and tested with,
the free Glassfish application server from Sun (now Oracle).

If you don't have Glassfish installed and set up, you may want to read
doc/README-GLASSFISH.txt before proceeding with these instructions.

SUPPORTED APPLICATION SERVERS
=============================

Currently, Oracle Glassfish (3.1 or newer) Full Profile is the only supported
application server, though it may work on others. Glassfish 3.0 or older are
not supported, do not work and will never work with postupload.

postupload is written to the Java EE 6 Full Profile standard. Web Profile is
not sufficient, because it lacks JAX-RS (REST services) and JavaMail.  A Web
Profile container with JAX-RS and JavaMail added should work, but is not
tested.

QUICKSTART FOR GLASSFISH ADMINISTRATORS
=======================================

If you already have a running Glassfish instance and are comfortable
administering it, the quick version of how to configure postupload is:

- compile postupload using maven
- deploy the .war with context root /postupload
- add POSTUPLOAD_ADMIN group membership for one or more users/groups/realms
- edit the created mail/smtp javamail resource if "localhost" isn't an smtp server
- visit $CONTEXTROOT/faces/admin/configure.xhtml

If none of that meant anything to you, don't stress. Read on for detailed
instructions on how to install postupload on Glassfish.

COMPILING POSTUPLOAD
====================

DEPENDENCIES
------------

You need the Java Development Kit (JDK) version 6 and Maven 3 to compile
postupload. You can download a JDK from:

  http://www.oracle.com/technetwork/java/javase/downloads/index.html

You can get Maven 3 here:

  http://maven.apache.org/download.html 

Alternately, you can get NetBeans 7, which bundles Maven 3 and provides a nice
IDE GUI for working with Java EE applications, here:

  http://netbeans.org/community/releases/70/

While postupload is developed using NetBeans, you don't need NetBeans to use it
or to modify it. It's nice to use, though.

COMPILE
-------

At present postupload is only distributed in source form, so you will need to
compile it.

If you have NetBeans, launch it, open the postupload source directory in
netbeans and choose "build" to produce the war file you can deploy in the next
step.

If you prefer to work on the command line, make sure the `mvn' command for
Maven 3 is on your PATH then cd to the postupload source directory and run:

  mvn clean package

to produce target/postupload.war .

If this is the first time you've built postupload, you will see a lot of maven plugins,
jar files, etc being downloaded. This is quite normal. They're cached on your computer
in ~/.m2 (unless configued otherwise) and will be re-used for future builds.

If you prefer a different IDE, any that supports Maven should work just fine.
For Eclipse users, apparently running "mvn eclipse:eclipse" makes an Eclipse project.


SETTING UP GLASSFISH FOR POSTUPLOAD
===================================

The default glassfish admin console is on http://localhost:4848 . The asadmin
command is in the bin/ directory of the glassfish install directory. You will
need to use these tools for the following instructions.

CONFIGURING WEBAPP ACCESS CONTROL
---------------------------------

By default, postupload creates a POSTUPLOAD_ADMIN group to match the internal
role of the same name. No Glassfish users are members of this group by default
so you need to assign membership and, if necessary, create users. Unless you
configured your app server differently, all this is all done in the "file" realm.

If you don't have any users, you'll want to create one or more users in the
file realm so you can use them to log in. You can either grant them
POSTUPLOAD_ADMIN membership when you create them or you can grant it by
default to all logged in users.

You can create users using the "manage users" button at:
  Configurations->server-config->Security->Realms->file
in the admin console, or using:

	asadmin create-file-user --groups POSTUPLOAD_ADMIN <username>

then log in with user name you created above when prompted by the web app later.

If you need anything more sophisticated, you'll need to mess with role mappings
in the application's glassfish-web.xml . See the Glassfish administration manual.

DEPLOYING POSTUPLOAD TO GLASSFISH
---------------------------------

Deploy postupload.war using the admin console or using asadmin:

	./asadmin deploy --contextroot postupload --upload=true target/postupload.war

To redeploy an updated version, use:

	./asadmin redeploy --name postupload --upload=true target/postupload.war

If you're deploying to a remote server, you don't have to scp the war over. You
can just add "--host <somehostname>" to the asadmin command line and it'll
connect to the remote server's admin interface.

CONFIGURING POSTUPLOAD
======================

Application configuration is done via a web interface at
http://localhost:8080/postupload/faces/admin/configure.xhtml (or wherever your
glassfish is listening).

Some features, like database access and mail delivery, are controlled via the
application server's administration console rather than the in-app
configuration page.


RUNNING POSTUPLOAD
==================

To run postupload, open its base URL in your web browser. The exact URL will
depend on how you configured your application server and what context root you
used for postupload. Assuming your application server is listening on 'localhost:8080'
(as Glassfish and JBoss AS do by default) you should be able to open:

  http://localhost:8080/postupload/

You can test your configuration by loading 

  http://localhost:8080/postupload/faces/admin/configtest.xhtml


JAVAMAIL
========

postupload requires a JavaMail resource from the container. When deployed to a
Glassfish server, it will automatically create a default JavaMail resource
that'll deliver mail via SMTP to localhost. For other application servers you
must create a suitable JavaMail resource yourself.

The JavaMail resource postupload expects to find is called "mail/smtp". You
may remap that however you like, so long as it the resource appears to be
named that way to postupload.

If you want to modify the JavaMail resource, look for "mail/smtp" under
JavaMail Sessions in the Glassfish admin console; or on the command line you
can:

Delete the old resource, if any:

	asadmin delete-javamail-resource 'mail/smtp'

and re-create with new settings:

	asadmin create-javamail-resource \
		--mailhost SERVERNAME --mailuser glassfish \
		--fromaddress glassfish@localnet 'mail/smtp'

Of course, you'll need to adapt the details for your environment. The mailuser
will be unused unless you're requiring smtp authentication, and the fromaddress
will not be used because postupload always sets one.

See "asadmin create-javamail-resource -h" and the javamail docs for additional
properties you might need to set for your environment.

DEBUGGING AND LOGGING
=====================

For most debugging, it's easiest to simply run postupload under NetBeans, letting
NetBeans take care of starting a local debug glassfish instance.

Sometimes you'll want to get more detailed logging of a production postupload
instance, though. In that case, you'll want to use the glassfish admin console
under "Configurations -> server-config -> Logger settings"
set logging for "au.com.postnewspapers.postupload" to "FINEST".

Alternately, to set the log level of postupload via asadmin, use:

  set-log-levels au.com.postnewspapers.postupload=FINEST

Use

  list-log-levels

to dump the current logging configuration. You'll need to restart the domain
for the logging changes to take effect.

It's also possible that you'll want to enable more detailed logging for
"com.sun.jersey" (the RESTful services component), "org.codehaus.jackson" (the
JSON component), "com.sun.faces" (the JSF component) etc.
