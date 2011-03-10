DEPLOYING THE APP
=================

postupload is a Java EE application. It runs in an application server container
rather than being a stand-alone program. 

SUPPORTED APPLICATION SERVERS
=============================

Currently, Oracle Glassfish (3.1 or newer) Full Profile is the only supported
application server, though it may work on others. Glassfish 3.0 or older are
not supported, do not work and will never work with postupload.

postupload is written to the Java EE 6 Full Profile standard. Web Profile is
not sufficient, because it lacks JAX-RS (REST services) and JavaMail.  A Web
Profile container with JAX-RS and JavaMail added should work, but is not
tested.

GETTING AND INSTALLING JAVA AND GLASSFISH
=========================================

Before installing Glassfish, make sure you have the Java Development Kit (JDK)
or Java Runtime Environment (JRE) version 6 (1.6.0_22) or newer installed. You
only need the JDK to compile postupload; it is not needed to deploy or run
Glassfish, so it's fine to install just the JRE on the server and put the JDK
on your dev machine/workstation/laptop. On the other hand, installing the JDK
on the server does no harm and it can be handy, so I'd recommend doing so
unless space is a concern.

You do NOT need the "JDK 6 Update 24 with Java EE"; I recommend installing the
plain, unbundled JDK then separately installing Glassfish and (if desired)
NetBeans. The bundled versions are sometimes out of date. Thanks, Oracle.

You can download the JDK and JRE here:

  http://www.oracle.com/technetwork/java/javase/downloads/index.html 


You can download Glassfish 3.1 from Oracle:

  http://glassfish.java.net/public/downloadsindex.html

or (once Oracle break that link too):

  http://www.oracle.com/technetwork/middleware/glassfish/downloads/index.html

If you're installing to a headless server, get the .zip archive and unpack it
where you want to run Glassfish from. Make sure to get the full profile not
the web profile installer. If you get the wrong one, you can use the update
tool once Glassfish is installed to add the full profile packages.

For testing, you can simply use the graphical Glassfish installer and run Glassfish
on your laptop/workstation. For production, I STRONGLY recommend that you run a
production Glassfish instance in an isolated user account on your server, like
you should any other service that doesn't require root. This requires the
creation of a suitable startup script for your server, but just to get started
on a Linux box you can:

	sudo -i
	mkdir -p /opt
	cd /opt
	unzip /path/to/glassfish-3.1.zip
	adduser -s glassfish -d /opt/glassfish3
	chown -R glassfish /opt/glassfish3
	chmod -R go-rwx /opt/glassfish3
	sudo su -c "/opt/glassfish3/bin/asadmin start-domain domain1" glassfish

You may need to "export JAVA_HOME=/path/to/your/java" before starting Glassfish if
your system doesn't have java on the PATH or a JAVA_HOME set by default.


COMPILING POSTUPLOAD
====================

While postupload is developed using NetBeans, you don't need NetBeans to use it
or to modify it. You can install NetBeans and use it to compile postupload if
you'd like, though.

At present postupload is only distributed in source form, so you will need to
compile it. postupload is built with maven 3. Assuming your `mvn' is on the
PATH, you should be able to cd to the `postupload' directory and run:

  mvn war:war

to produce target/postupload.war . Alternately, you can open the postupload
source directory in netbeans and choose "build" to produce the war file.

If this is the first time you've built postupload, you will see a lot of maven plugins,
jar files, etc being downloaded. This is quite normal. They're cached on your computer
in ~/.m2 (unless configued otherwise) and will be re-used for future builds.

You can get Maven 3 here:

  http://maven.apache.org/download.html 

Alternately, you can get NetBeans 7 (which bundles Maven 3) here:

  http://netbeans.org/community/releases/70/

You can use any other IDE that supports Maven if you prefer.


SETTING UP GLASSFISH FOR POSTUPLOAD
===================================

SETTING THE GLASSFISH PORT
--------------------------

Glassfish runs on ports 8080 (main http server) and 4848 (http admin console) as
well as several other ports for various services. If you have something else
running on port 8080, Glassfish will not start. You can change the port the
default glassfish domain runs on by editing
glassfish/domains/domain1/config/domain.xml and looking for the
<network-listeners> XML stanza.

STARTING GLASSFISH
------------------

Once you've made sure Glassfish won't be fighting anything else for port 8080,
start glassfish:

	./asadmin start-domain domain1


If there is a port conflict, you'll see a message like this:

[#|2011-03-10T11:21:23.762+0800|SEVERE|glassfish3.1|javax.enterprise.system.core.com.sun.enterprise.v3.server|_ThreadID=1;_ThreadName=main;|Shutting down v3 due to startup exception : No free port within range: 8080=com.sun.enterprise.v3.services.impl.monitor.MonitorableSelectorHandler@7038b9|#]

in the startup messages from Glassfish - even though `asadmin' reports that it
successfully started the server. If you have this problem, see "SETTING THE
GLASSFISH PORT".

ADMINISTERING GLASSFISH
-----------------------

Glassfish administration is done via the "asadmin" command line tool (see
"asadmin list-commands", "asadmin help" and "asadmin help <commandname>") and
via the http web console on port 4848.

CONFIGURING ACCESS CONTROL
--------------------------

The simplest and most direct easiest way to set up access control, so all
authenticated users have admin access and no guests have it, is to use the
Glassfish admin console to check the "Default Principal To Role Mapping" option
in :
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

Deploy postupload.war using the admin console or using asadmin:

	./asadmin deploy --contextroot postupload --upload=true target/postupload.war

To redeploy an updated version, use:

	./asadmin redeploy --name postupload --upload=true target/postupload.war

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
