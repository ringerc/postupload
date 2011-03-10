WHAT IS IT
==========

postupload is a complete web-based multi-file upload app that's intended to
replace abuse of email attachments for transferring large files.  It's
essentially a web-based anonymous-FTP-like dropbox with email notification.

The application uses Uploadify to handle multi-file uploads, introducing a
dependency on Adobe Flash. A single-file upload page without a flash
requirement is planned. In the longer term, support for plupload will be added
to enable use of the HTML5 file api for native multi-file uploads.

QUICKSTART (FOR TESTING AND TRYOUT ONLY)
========================================

Do not follow these instructions for production deployment. Please read
doc/README-DEPLOY.txt instead. These instructions are only for if you want to
take a really quick and dirty look at the app before deploying it properly.

Download the JDK from http://www.oracle.com/technetwork/java/javase/downloads/index.html and install it.

Download NetBeans 7 with Java from http://netbeans.org/community/releases/70/ . Make sure to
get the bundle that includes "Java Web and EE" support. Install it and the Glassfish server
it bundles.

Launch NetBeans, choose "File -> open project" and open the postupload source
folder. Click "Run project" or press F6. When prompted for the application
server to use, choose glassfish from the menu.

If this is the first time you've used Netbeans (or Maven) it'll spend quite a
while downloading library dependencies. Be patient. Once it's finished
downloading it'll compile the project, deploy it to the application server, and
open your default browser with the start page of the postupload application. 

There won't be any recipients listed, and you'll need to do some glassfish
setup before you can log in to the application's admin page to add them and to
set the output path, etc. See "CONFIGURING ACCESS CONTROL" in
doc/README-DEPLOY.txt. Once you've set up a login, visit
http://localhost:8080/postupload/faces/admin/configure.xhtml to set the app up.

WHY DOES POSTUPLOAD EXIST? WHAT IS IT FOR?
==========================================

This software was written for a newspaper to replace anonymous FTP and the
abuse of large email attachments. Most clients struggled to use or understand
FTP, and both clients and staff became increasingly frustrated with the
unreliability and unpredictable delivery times of large emails. An alternative
was needed, and the existing commercial options weren't deemed particularly
attractive.

There are many services that provide web-based facilities for sending large
files. There are rather fewer that provide a page allowing a user to accept
files from anybody. All of the sites I found that do offer such services have
one or more of the following issues:

- The web-based user interface can't batch download files. This is a limitation
  of current browsers, and probably a good one, but it means you need a desktop
  client.

- The desktop client is often Windows only, or at best also Mac OS X . The
  newspaper runs Linux thin clients in the sales department, so this is
  somewhat painful.

- There's no noninteractive client suitable for running on a file server to
  automatically download files as they're sent by clients.

- They don't offer a simple web api that could be used to easily implement
  such a client, or only offer it in an overpriced "Enterprise" version

- No "recipient address book" is offered; either all uploads go to a single
  recipient or the sender has to enter the recipient details each time.
  If a recipient address book is offered, it tends to come only in the
  expensive "enterprise" version.

- They want an unreasonable amount of money per month, and often per user.

HOW DOES POSTUPLOAD SOLVE THESE ISSUES?
=======================================

For the specific and admittedly narrow use case this software was written for,
postupload provides a simple way for clients to send files reliably and easily.
The files are automatically saved on a shared network volume where staff can
access them, so there's no need to jump through hoops with download clients and
web browsers.

It's easier than FTP, more reliable and faster than email, and cheaper than a
decent online upload service.

How do I use it?
================

Read doc/README-DEPLOY.txt for installation and use instructions

WHY USE JAVA ON THE SERVER SIDE?
================================

Postupload has a Java EE 6 server backend to receive the files, notify the
recipients, and track state. The user interface is all written in CSS, HTML,
and client-side JavaScript; there is NO CLIENT SIDE JAVA. Flash is used
client-side, which is almost worse.

Java was used for the server because other in-house projects are using it,
because Java EE 6 is really nice to use, and because its web services support
(JAX-RS) and JSON support saves a lot of time and hassle. In this regard Java
EE 6 is rather different to prior iterations of the standard - if you've been
horrified by Java EE before, you'll be reassured to know that Java EE 6 is a
very different beast. You don't need huge and expensive application server
software to run it, either - the free Glassfish server will do nicely.
