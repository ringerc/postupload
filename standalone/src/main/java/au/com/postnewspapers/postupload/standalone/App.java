package au.com.postnewspapers.postupload.standalone;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

public class App 
{
    private static final Logger logger = Logger.getLogger(App.class.getName());
    private static final String 
            CFG_LISTEN_PORT = "listenPort",
            CFG_LISTEN_ADDRESS = "listenAddress",
            CFG_SMTP_HOST = "smtpHost",
            CFG_SMTP_USER = "smtpUser",
            CFG_SMTP_FROM = "smtpFrom",
            CFG_SMTP_PORT = "smtpPort",
            CFG_ADMIN_PASSWORD = "adminPassword";
    
    private final GlassFish glassfish;
    private final CommandRunner commandRunner;
    private final Deployer deployer;
    private String deploymentName;
    
    private final Properties configuration;
    
    
    private static class PostuploadError extends RuntimeException {
        public PostuploadError() { super(); }
        public PostuploadError(Throwable ex) { super(ex); }
        public PostuploadError(String msg) { super(msg); }
        public PostuploadError(String msg, Throwable ex) { super(msg,ex); }
    }
    
    public App(Properties cfg) throws GlassFishException {
        this.configuration = cfg;
        GlassFishProperties gfp = new GlassFishProperties(); // Glowing jellyfish! Yes, glassfish has driven me insane.
        setHttpParameters(gfp);
        glassfish = GlassFishRuntime.bootstrap().newGlassFish(gfp);
        glassfish.start();
        commandRunner = glassfish.getService(CommandRunner.class);
        deployer = glassfish.getService(Deployer.class);
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Supported glassfish parameters are: {0}", listGlassfishProperties(commandRunner));
        }
        CommandResult commandResult = commandRunner.run("set", "server-config.security-service.activate-default-principal-to-role-mapping=true");
        if (commandResult.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            throw new PostuploadError("Unable to set default role mapping" + commandResult.getOutput(), commandResult.getFailureCause());
        }
        createResources();
    }
    
    private String listGlassfishProperties(CommandRunner commandRunner) {
        CommandResult result = commandRunner.run("list","*");
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            throw new PostuploadError("Unable to list properties", result.getFailureCause());
        }
        return result.getOutput();
    }
    
    private void setHttpParameters(GlassFishProperties gfp) {
        // Magic! For some reason, despite the fact that running run("list","*") reports
        // this property name as "configs.config.server-config.network-config.network-listeners.network-listener.http-listener"
        // we have to refer to it with "embedded-glassfish-config" instead of "configs.config" when passing it as an init
        // param. Because it wasn't unintuitive enough already, I guess.
        //
        // If this changes randomly at some future point with an update to GlassFish, 
        // you can find out the new magic name by dumping the keys of the
        // Properties object that's returned by GlassFishProperties.getProperties(),
        // or by reading the soruce code of the appropriate version of GlassFishProperties.java .
        //
        gfp.setProperty("embedded-glassfish-config.server.network-config.network-listeners.network-listener.http-listener.address", configuration.getProperty(CFG_LISTEN_ADDRESS));
        gfp.setPort("http-listener", Integer.parseInt(configuration.getProperty(CFG_LISTEN_PORT)));
        if (gfp.getProperties().getProperty("embedded-glassfish-config.server.network-config.network-listeners.network-listener.http-listener.port",null) == null) {
            throw new PostuploadError("Internal error! It looks like Glassfish's property key names have changed. See the comments near this code.");
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Glassfish startup properties are:");
            Properties p = gfp.getProperties();
            Enumeration n = p.propertyNames();
            while (n.hasMoreElements()) {
                String s = (String)n.nextElement();
                logger.log(Level.FINE, "Key: " + s + ", Value: " + p.getProperty(s));
            }
            logger.log(Level.FINE, "End glassfish startup properties");
        }
    }
    
    private void createResources() {
        // TODO: handle custom glassfish-resources.xml if provided
        // TODO: test if resource already exists. Delete & recreate?
        logger.info("Creating JavaMail resource");
        CommandResult commandResult = commandRunner.run("create-javamail-resource",
		"--mailhost=" + configuration.getProperty(CFG_SMTP_HOST),
                "--mailuser="  + configuration.getProperty(CFG_SMTP_USER), 
		"--fromaddress=" + configuration.getProperty(CFG_SMTP_FROM),
                "--property","mail.smtp.port="+configuration.getProperty(CFG_SMTP_PORT),
                "mail/smtp");
        if (commandResult.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            throw new PostuploadError("Creation of JavaMail resource failed " + commandResult.getOutput(), commandResult.getFailureCause());
        }
        logger.info("Creating JAAS realm");
        // TODO: check if realm already exists
        commandResult = commandRunner.run("create-auth-realm",
                "--classname", "com.sun.enterprise.security.auth.realm.file.FileRealm",
                "--property", "file=postuploadKeyFile:jaas-context=postuploadRealm",
                "postuploadRealm");
        if (commandResult.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            throw new PostuploadError("Creation of authentication realm failed " + commandResult.getOutput(), commandResult.getFailureCause());
        }
        
        // FAILS because someone forgot about embedded and killed off --AS_ADMIN_USERPASSWORD
        // org.glassfish.api.admin.CommandValidationException: Password not allowed on command line: AS_ADMIN_USERPASSWORD
        commandResult = commandRunner.run(
                "create-file-user",
                "--AS_ADMIN_USERPASSWORD", configuration.getProperty(CFG_ADMIN_PASSWORD),
                "--authrealmname", "postuploadRealm",
                "--groups", "POSTUPLOAD_ADMIN",
                "admin");
        if (commandResult.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            throw new PostuploadError("Creation of admin user in postuploadRealm failed " + commandResult.getOutput(), commandResult.getFailureCause());
        }
    }
    
    public void deploy() throws GlassFishException {
        deploymentName = deployer.deploy(new File("../webapp/target/postupload-glassfish3.war").toURI(),
                "--contextroot=/", "--force=true");
    }
    
    public void undeploy() throws GlassFishException {
        deployer.undeploy(deploymentName);
    }
    
    public void stop() throws GlassFishException {
        glassfish.stop();
        glassfish.dispose();
    }
    
    private static void usage(String msg) throws IOException {
        Writer out = new BufferedWriter(new OutputStreamWriter(System.out));
        if (msg != null) {
            out.write(msg);
            out.write("\n");
        }
        out.write("Usage: java -jar postupload-standalone.jar [args]\n"
                + "\tOptional args include:\n"
                + "\t--config /path/to/properties/file.properties\n"
                );
        out.write("\n");
        
    }
    
    private static Properties processArgsAndConfig(String[] args) throws IOException {
        String configFile = null;
        
        int idx = 0;
        while (idx < args.length) {
            final String arg = args[idx];
            idx++;
            if (arg.equals("-c") || arg.equals("--config")) {
                if (idx == args.length) {
                    usage("--config requires the path to a properties file as an argument");
                    System.exit(1);
                }
                configFile = args[idx];
                idx++;
            }
        }
        return loadConfiguration(configFile);
    }
    
    private static Properties loadConfiguration(String configFile) throws IOException {
        InputStream is = App.class.getClassLoader().getResourceAsStream("au/com/postnewspapers/postupload/standalone/postupload.properties");
        if (is == null) {
            throw new PostuploadError("Unable to load default settings. This is a programming error.");
        }
        Properties defaultProps = new Properties();
        defaultProps.load(is);
        Properties cfg = new Properties(defaultProps);
        if (configFile != null) {
            cfg.load(new FileReader(configFile));
        }
        return cfg;
    }
    
    private static void waitForExit(App app) throws GlassFishException {
        try {
            System.out.println("Press return to terminate the server, or r to reload the app");
            InputStreamReader r = new InputStreamReader(System.in);
            while (true) {
                // Read one character and discard any further buffered input
                String input = new String(Character.toChars(r.read()));
                while (r.ready()) {
                    r.read();
                }
                // Then act on the command
                if (input.equals("r")) {
                    app.undeploy();
                    app.deploy();
                    continue;
                } else {
                    app.stop();
                    break;
                }
            }
        } catch (IOException ex) {
            logger.warning("Unable to read from stdin - stdin closed? Running non-interactively.");
        }
    }
    
    public static void main( String[] args ) throws GlassFishException, IOException {
        // Set a few key initial params
        System.setProperty("java.security.auth.login.config", "C:/Users/Craig/Developer/postupload/standalone/login.conf");
        // Process arguments and load configuration
        Properties cfg = processArgsAndConfig(args);
        
        // Then launch Glassfish
        App app = null;
        try {
            app = new App(cfg);
            app.deploy();
        } catch (PostuploadError ex) {
            logger.log(Level.SEVERE, "Unable to start the server", ex);
            if (app != null)
                app.stop();
            System.exit(2);
        }
        
        // The main thread may now terminate safely; glassfish has control.
        // However, if we can we'll listen on stdin for events.
        waitForExit(app);
    }
}
