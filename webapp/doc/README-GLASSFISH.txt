INSTALLING GLASSFISH
====================

postupload is a Java EE application. It runs in an application server container
rather than being a stand-alone program. It is written for and tested on the
Glassfish application server.

REQUIRED APPLICATION SERVER
===========================

Currently, Oracle Glassfish (3.1 or newer) Full Profile is the only supported
application server, though it may work on others. Glassfish 3.0 or older are
not supported, do not work and will never work with postupload.

postupload is written to the Java EE 6 Full Profile standard. Web Profile is
not sufficient, because it lacks JAX-RS (REST services) and JavaMail.  A Web
Profile container with JAX-RS and JavaMail added should work, but is not
tested.

IF YOU ALREADY HAVE A GLASSFISH INSTALL (and know how to use it)
================================================================

Skip these instructions and follow README-DEPLOY.txt .

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
