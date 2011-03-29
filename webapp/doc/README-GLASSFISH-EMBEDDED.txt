Running postupload on embedded glassfish is pretty simple.

Compile:

  mvn -Pglassfish3embedded clean install

and run:

  mvn -Pglassfish3embedded embedded-glassfish:run



At present, support for running under embedded glassfish is somewhat incomplete, though, as there's no way to:

- configure an authentication principal to allow access to the protected configuration screen;
- configure a JavaMail session