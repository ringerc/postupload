WHAT IS IT
==========

postupload is a complete web-based multi-file upload app that's intended to
replace abuse of email attachments for transferring large files.  It's
essentially a web-based anonymous-FTP-like dropbox with email notification.

The application uses Uploadify to handle multi-file uploads, introducing a
dependency on Adobe Flash. A single-file upload page without a flash
requirement is planned. In the longer term, support for plupload will be added
to enable use of the HTML5 file api for native multi-file uploads.

WHY?
====

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

Read README-DEPLOY.txt for installation and use instructions
