package au.com.postnewspapers.postupload.standalone;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishRuntime;

public class App 
{
    private static final Logger logger = Logger.getLogger(App.class.getName());
    
    private final GlassFish glassfish;
    private final CommandRunner commandRunner;
    private final Deployer deployer;
    private String deploymentName;
    
    private class PostuploadError extends RuntimeException {
        public PostuploadError() { super(); }
        public PostuploadError(Throwable ex) { super(ex); }
        public PostuploadError(String msg) { super(msg); }
        public PostuploadError(String msg, Throwable ex) { super(msg,ex); }
    }
    
    public App() throws GlassFishException {
        glassfish = GlassFishRuntime.bootstrap().newGlassFish();
        glassfish.start();
        commandRunner = glassfish.getService(CommandRunner.class);
        deployer = glassfish.getService(Deployer.class);
        
        // The shipped domain.xml disables the http listeners and associated
        // thread pools. Make our own.
        CommandResult commandResult = commandRunner.run(
                "create-http-listener", "--listenerport=9090",
                "--listeneraddress=0.0.0.0", "--defaultvs=server",
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
    
    public static void main( String[] args ) throws GlassFishException {
        App app = new App();
        app.deploy();
        // The main thread may now terminate safely; glassfish has control.
        // However, if we can we'll listen on stdin for events.
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
}
