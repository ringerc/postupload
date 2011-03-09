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


SETTING UP GLASSFISH FOR POSTUPLOAD
===================================

STARTING GLASSFISH
------------------

Start Glassfish if you need to. If you're using the default configuration and a local server, just:

	./asadmin start-domain domain1

CONFIGURING ACCESS CONTROL
--------------------------

The simplest and most direct easiest way to set up access control, so all
authenticated users have admin access and no guests have it, is to check the
"Default Principal To Role Mapping" option in 
  Configurations->server-config->Security
then add user(s) who should have access to the POSTUPLOAD_ADMIN group. If
you want to grant access to all allowed users, add POSTUPLOAD_ADMIN to the
"assign groups" list in:
  Configurations->server-config->Security->Realms->file
so all users get membership of the group.

If you need anything more sophisticated, you'll need to mess with role mappings
in the application's glassfish-web.xml . See the Glassfish administration manual.

DEPLOYING POSTUPLOAD
--------------------

Deploy postupload.war using the admin console (http://localhost:4848 by default); or

	./asadmin deploy --contextroot postupload --upload=true target/postupload.war

To redeploy an updated version, use:

	./asadmin redeploy --name postupload --upload=true target/postupload.war

CONFIGURING
===========

Application configuration is done via a web interface at /postupload/faces/admin/configure.xhtml .

Some features, like database access and mail delivery, are controlled via the
application server's administration console rather than the in-app
configuration page.


RUNNING
=======

To run postupload, open its base URL in your web browser. The exact URL will
depend on how you configured your application server and what context root you
used for postupload. Assuming your application server is listening on 'localhost:8080'
(as Glassfish and JBoss AS do by default) you should be able to open:

  http://localhost:4848/postupload/

You can test your configuration by loading 

  http://localhost:4848/postupload/faces/admin/configtest.xhtml


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
JavaMail Sessions in the admin console; or on the command line you can:

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
