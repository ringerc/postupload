package au.com.postnewspapers.postupload.standalone;

import java.io.File;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishRuntime;

public class App 
{
    public static void main( String[] args ) throws GlassFishException
    {
        final GlassFish glassfish = GlassFishRuntime.bootstrap().newGlassFish();
        glassfish.start();
        
        final CommandRunner commandRunner = glassfish.getService(CommandRunner.class);
        final Deployer deployer = glassfish.getService(Deployer.class);
                
        String deployedApp = deployer.deploy(new File("postupload-webapp.war").toURI(),
                "--contextroot=postupload", "--force=true");
        
        deployer.undeploy(deployedApp);

        /**Stop GlassFish.*/
        glassfish.stop();

        /** Dispose GlassFish. */
        glassfish.dispose();
    }
}
