Deployment of postupload to JBoss AS 6 doesn't currently work, because
the JAX-RS implementation (Jersey) used by postupload conflicts with
the RESTEasy implementation bundled with JBoss AS.

JBoss doesn't offer any way for us to selectively disable RESTEasy just for our
app. We need to pick one implementation, RESTEasy or Jersey, and stick with it
because the JAX-RS spec doesn't cover file uploads well; we can't just stick to
the pure JAX-RS implementation-agnostic APIs.

To get postuplaod working on jboss, we either need to be able to deploy a
working Jersey to JBoss, or we need a RESTEasy based implementation of the file
upload handler that uses the built-in JAX-RS support in JBoss AS 6.

Just removing resteasy.deployer from server/default/deployers doesn't
seem to be enough - there are apparent xml handling conflicts as well.
