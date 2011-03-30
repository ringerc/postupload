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
        gfp.setPort("http-listener", Integer.parseInt(cfg.getProperty(CFG_LISTEN_PORT)));
        //gfp.setProperty("server-config.network-config.network-listeners.network-listener.http-listener.address", cfg.getProperty(CFG_LISTEN_ADDRESS)); //XXX FIXME TODO
        glassfish = GlassFishRuntime.bootstrap().newGlassFish(gfp);
        glassfish.start();
        commandRunner = glassfish.getService(CommandRunner.class);
        deployer = glassfish.getService(Deployer.class);
        CommandResult commandResult = commandRunner.run("set", "server-config.security-service.activate-default-principal-to-role-mapping=true");
        if (commandResult.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            throw new PostuploadError("Unable to set default role mapping" + commandResult.getOutput(), commandResult.getFailureCause());
        }
        //createHttpListeners();
        createResources();
    }
    
    private void createHttpListeners() {
        // The shipped domain.xml disables the http listeners and associated
        // thread pools. Make our own.
        CommandResult commandResult = commandRunner.run(
                "create-http-listener",
                "--listenerport=" + configuration.getProperty(CFG_LISTEN_PORT),
                "--listeneraddress=" + configuration.getProperty(CFG_LISTEN_ADDRESS),
                "--defaultvs=server",
                "my-http-listener");
        if (commandResult.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            throw new PostuploadError("HTTP listener creation failed: " + commandResult.getOutput(), commandResult.getFailureCause());
        }
        commandResult = commandRunner.run("create-threadpool",
                "--maxthreadpoolsize=200", "--minthreadpoolsize=200",
                "my-thread-pool");
        if (commandResult.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            throw new PostuploadError("HTTP thread pool creation failed: " + commandResult.getOutput(), commandResult.getFailureCause());
        }
        commandResult = commandRunner.run("set",
                "server.network-config.network-listeners.network-listener." +
                        "my-http-listener.thread-pool=my-thread-pool");
        if (commandResult.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            throw new PostuploadError("HTTP thread pool assignment failed: " + commandResult.getOutput(), commandResult.getFailureCause());
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
        // Because of the removal of the --userpassword option in the name of better
        // security, we have to write the password out to disk and read it back in
        // again. WTF.
        File p = new File("password-tmp");
        try {
            FileWriter w = new FileWriter(p);
            try {
                w.write("AS_ADMIN_USERPASSWORD=");
                w.write(configuration.getProperty(CFG_ADMIN_PASSWORD));
                w.write("\n");
            } finally {
                w.close();
            }
        } catch (IOException ex) {
            throw new PostuploadError("Unable to write password file for user creation", ex);
        }
        commandResult = commandRunner.run(
                "--passwordfile", "password-tmp", 
                "create-file-user",
                "--authrealmname", "postuploadRealm",
                "--groups", "POSTUPLOAD_ADMIN",
                "admin");
        if (commandResult.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            throw new PostuploadError("Creation of admin user in postuploadRealm failed " + commandResult.getOutput(), commandResult.getFailureCause());
        }
        p.delete();
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
