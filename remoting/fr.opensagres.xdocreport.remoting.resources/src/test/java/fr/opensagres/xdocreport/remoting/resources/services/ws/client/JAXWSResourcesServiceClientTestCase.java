package fr.opensagres.xdocreport.remoting.resources.services.ws.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import fr.opensagres.xdocreport.remoting.resources.domain.BinaryData;
import fr.opensagres.xdocreport.remoting.resources.domain.Resource;
import fr.opensagres.xdocreport.remoting.resources.services.FileUtils;
import fr.opensagres.xdocreport.remoting.resources.services.ResourceComparator;
import fr.opensagres.xdocreport.remoting.resources.services.ResourcesService;

public class JAXWSResourcesServiceClientTestCase
{

    private static final int PORT = 9999;

    private static Server server;

    private static final String BASE_ADDRESS = "http://localhost:" + PORT;

    public static File srcFolder = new File( "src/test/resources/fr/opensagres/xdocreport/remoting/resources" );

    public static File tempFolder = new File( "target" );

    @BeforeClass
    public static void startServer()
        throws Exception
    {

        // 1) Copy resources in the target folder.
        File resourcesFolder = new File( tempFolder, "resources" );
        if ( resourcesFolder.exists() )
        {
            resourcesFolder.delete();
        }
        FileUtils.copyDirectory( srcFolder, resourcesFolder );

        // 2) Start Jetty Server

        ServletHolder servlet = new ServletHolder( CXFNonSpringServlet.class );
        servlet.setInitParameter( "timeout", "60000" );
        server = new Server( PORT );

        ServletContextHandler context = new ServletContextHandler( server, "/", ServletContextHandler.SESSIONS );

        context.addServlet( servlet, "/*" );
        server.start();

        // String address = BASE_ADDRESS + "/ResourcesServiceImplPort";
        // javax.xml.ws.Endpoint.publish( address, new MockJAXWSResourcesService() );

    }

    // @Test
    public void name()
    {
        ResourcesService client = JAXWSResourcesServiceClientFactory.create( BASE_ADDRESS );
        String name = client.getName();
        Assert.assertEquals( "Test-RepositoryService", name );
    }

    // @Test
    public void root()
    {
        ResourcesService client = JAXWSResourcesServiceClientFactory.create( BASE_ADDRESS );
        Resource root = client.getRoot();

        // Document coming from the folder src/test/resources/fr/opensagres/xdocreport/resources/repository
        // See class MockRepositoryService
        Assert.assertNotNull( root );
        Assert.assertEquals( "resources", root.getName() );
        Assert.assertEquals( 4, root.getChildren().size() );

        // Sort the list of Resource because File.listFiles() doeesn' given the same order
        // between different OS.
        Collections.sort( root.getChildren(), ResourceComparator.INSTANCE );

        Assert.assertEquals( "Custom", root.getChildren().get( 0 ).getName() );
        Assert.assertEquals( Resource.FOLDER_TYPE, root.getChildren().get( 0 ).getType() );
        Assert.assertEquals( "Opensagres", root.getChildren().get( 1 ).getName() );
        Assert.assertEquals( Resource.FOLDER_TYPE, root.getChildren().get( 1 ).getType() );
        Assert.assertEquals( "Simple.docx", root.getChildren().get( 2 ).getName() );
        Assert.assertEquals( "Simple.odt", root.getChildren().get( 3 ).getName() );
    }

    // @Test
    public void downloadARootFile()
        throws FileNotFoundException, IOException
    {
        String resourcePath = "Simple.docx";
        ResourcesService client = JAXWSResourcesServiceClientFactory.create( BASE_ADDRESS );
        BinaryData document = client.download( resourcePath );
        Assert.assertNotNull( document );
        Assert.assertNotNull( document.getContent() );
        createFile( document.getContent(), resourcePath );
    }

    // @Test
    public void downloadAFileInFolder()
        throws FileNotFoundException, IOException
    {
        String resourcePath = "Custom/CustomSimple.docx";
        ResourcesService client = JAXWSResourcesServiceClientFactory.create( BASE_ADDRESS );
        BinaryData document = client.download( resourcePath );
        Assert.assertNotNull( document );
        Assert.assertNotNull( document.getContent() );
        createFile( document.getContent(), resourcePath );
    }

    private void createFile( byte[] flux, String filename )
        throws FileNotFoundException, IOException
    {
        File aFile = new File( tempFolder, this.getClass().getSimpleName() + "_" + filename );
        FileOutputStream fos = new FileOutputStream( aFile );
        fos.write( flux );
        fos.close();
    }

    @AfterClass
    public static void stopServer()
        throws Exception
    {
        server.stop();
    }
}