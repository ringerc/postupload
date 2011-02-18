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


RUNNING
=======

To run postupload, open its base URL in your web browser. The exact URL will
depend on how you configured your application server and what context root you
used for postupload. Assuming your application server is listening on 'localhost:8080'
(as Glassfish and JBoss AS do by default) you should be able to open:

  http://localhost:4848/postupload/

You can test your configuration by loading 

  http://localhost:4848/postupload/faces/configure.xhtml

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
