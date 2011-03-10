DEPLOYING THE APP
=================

postupload is a Java EE application. It runs in an application server container
rather than being a stand-alone program. 

These instructions are more detailed and longer than you'd expect because they explain
in detail how to install, configure and secure the Glassfish application server as well
as how to install postupload.

SUPPORTED APPLICATION SERVERS
=============================

Currently, Oracle Glassfish (3.1 or newer) Full Profile is the only supported
application server, though it may work on others. Glassfish 3.0 or older are
not supported, do not work and will never work with postupload.

postupload is written to the Java EE 6 Full Profile standard. Web Profile is
not sufficient, because it lacks JAX-RS (REST services) and JavaMail.  A Web
Profile container with JAX-RS and JavaMail added should work, but is not
tested.

IF YOU ALREADY HAVE A GLASSFISH INSTALL (and know how to use it)
================================================================

If you already have a running Glassfish instance and know the app server, the quick version of how to configure postupload is:

- compile postupload using maven
- deploy the .war with context root /postupload
- add POSTUPLOAD_ADMIN membership for one or more users/groups/realms
- edit the created mail/smtp javamail resource if "localhost" isn't an smtp server
- visit /postupload/faces/admin/configure.xhtml

If none of that meant anything to you, don't stress. Read on for detailed
instructions on how to get started when you've never used Glassfish or any
other Java EE application server before.

GETTING AND INSTALLING JAVA AND GLASSFISH
=========================================

GETTING JAVA
------------

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


GETTING GLASSFISH
-----------------

You can download Glassfish 3.1 from Oracle:

  http://glassfish.java.net/public/downloadsindex.html

or (once Oracle break that link too):

  http://www.oracle.com/technetwork/middleware/glassfish/downloads/index.html

If you're installing to a headless server, get the .zip archive and unpack it
where you want to run Glassfish from. Make sure to get the full profile not
the web profile installer. If you get the wrong one, you can use the update
tool once Glassfish is installed to add the full profile packages.

You can find Glassfish documentation here:

  http://glassfish.java.net/docs/index.html

It's a good idea to read the quick start guide you'll see there, though you do
not need to in order to follow this guide.

INSTALLING GLASSFISH - TESTING
------------------------------

For testing, you can simply use the graphical Glassfish installer and run Glassfish
on your laptop/workstation.

INSTALLING GLASSFISH - PRODUCTION (UNIX/LINUX/BSD SERVER)
---------------------------------------------------------

For production use I STRONGLY recommend that you run a production Glassfish
instance in an isolated user account on your server, like you should any other
service that doesn't require root. This requires the creation of a suitable
startup script for your server. First, you'll want to unpack glassfish, create
a user for it to run as, and set the permissions appropriately:

	sudo -i
	mkdir -p /opt
	cd /opt
	unzip /path/to/glassfish-3.1.zip
	adduser -s glassfish -d /opt/glassfish3
	chown -R glassfish /opt/glassfish3
	chmod -R go-rwx /opt/glassfish3

You can now manually start glassfish with:

	sudo su -c "/opt/glassfish3/glassfish/bin/startserv domain1" glassfish

... which will run in the foreground (useful for init scripts) or:

	sudo su -c "/opt/glassfish3/bin/asadmin start-domain domain1" glassfish

which will background after starting. If you're running glassfish under a dedicated
user as shown above, you'll want to run "sudo -u glassfish -i" to become the glassfish
user before running any of the asadmin commands given below.

You'll also need to grant the "glassfish" user the permissions to write to the
final output directory you want to save uploaded files to.

For Upstart-based ubuntu systems you can use the upstart configuration file
shipped with postupload to start glassfish. Install glassfish in
/opt/glassfish3, create the glassfish3 user, set permissions as above, copy
doc/upstart/glassfish.conf to /etc/init and run "service glassfish start".

INSTALLING GLASSFISH - PRODUCTION (Windows server)
---------------------------------------------------------

Configuring Glassfish to run as a Windows service is beyond the scope of this
documentation, but you'll find plenty of info about it on the 'net. The concept
is the same: run glassfish as a service, using an unpriveleged service account
that prevents it from accessing the rest of the system. Then grant it write
permission on the upload output directory.

Submissions for a quick HOWTO for this task on Windows would be appreciated.


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

If there is a port conflict, you'll see a message like this:

[#|2011-03-10T11:21:23.762+0800|SEVERE|glassfish3.1|javax.enterprise.system.core.com.sun.enterprise.v3.server|_ThreadID=1;_ThreadName=main;|Shutting down v3 due to startup exception : No free port within range: 8080=com.sun.enterprise.v3.services.impl.monitor.MonitorableSelectorHandler@7038b9|#]

in the startup messages from Glassfish - even though `asadmin' reports that it
successfully started the server.

STARTING GLASSFISH
------------------

Once you've made sure Glassfish won't be fighting anything else for port 8080,
start glassfish. For a production install use the commands given in "INSTALLING
GLASSFISH - PRODUCTION" to run it as an unpriveleged user. For testing, just run:

	./asadmin start-domain domain1

to run it under your own user account. 

*** NEVER RUN GLASSFISH AS ROOT ***


ADMINISTERING GLASSFISH
-----------------------

Glassfish administration is done via the "asadmin" command line tool (see
"asadmin list-commands", "asadmin help" and "asadmin help <commandname>") and
via the http web console on port 4848.

SECURING GLASSFISH
------------------

Glassfish ships configured for convenience, not security. This isn't a good idea for production.

To lock down glassfish, set a non-empty admin admin password, replacing the old blank password:

	asadmin change-admin-password

then enable secure admin:

	asadmin --user admin enable-secure-admin

and re-start Glassfish:

	asadmin --user admin stop-domain domain1
	asadmin --user admin start-domain domain1

Now when you visit the admin page you should be redirected to a https URL and
asked to log in. All asadmin commands will require a password, though you can
save that password in the asadmin password file by running:

	asadmin --user admin login

Make sure to set the permissions on the password file the login command output
mentions so only you can read it:

	chmod 0600 $HOME/.asadminpass

Note that the glassfish admin user lives in the "admin-realm"; it's not
available to authenticate users of webapps and the "admin" name doesn't collide
with an "admin" user in the file-realm used by webapps by default.

When considering security, remember that it's vital for the security of the
rest of your system to run glassfish as an unpriveleged user. See "INSTALLING
GLASSFISH - PRODUCTION".

CONFIGURING WEBAPP ACCESS CONTROL
---------------------------------

By default, postupload creates a POSTUPLOAD_ADMIN group to match the internal
role of the same name. No Glassfish users are members of this group by default
so you need to assign membership and, if necessary, create users. Unless you
configured your app server differently, all this is all done in the "file" realm.

If you don't have any users, you'll want to create one or more users in the
file realm so you can use them to log in. You can either grant them
POSTUPLOAD_ADMIN membership when you create them or you can grant it by
default to all logged in users:

If you want to grant access to all allowed users, open the admin console
(http://localhost:4848) and add POSTUPLOAD_ADMIN to the "assign groups" list
in:
  Configurations->server-config->Security->Realms->file
so all users get membership of the group. You can create users using the
"manage users" button there.

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
