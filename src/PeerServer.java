import java.io.FileNotFoundException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

//References: https://docs.oracle.com/en/java/javase/14/docs/api/java.rmi/module-summary.html
//References: https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/Properties.html

public class PeerServer {
    public String mainMasP;
    public String mainAddr;
    public String mainPort;
    public String maincurrAddr;
    public String FilePathForProp;


    private Properties loadProperties(String FilePathForProp) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream(FilePathForProp));
        return prop;
    }

    private void initializeServerProperties(Properties prop, int peerServerId) {
        mainMasP = prop.getProperty("MASTER_PORT");
        mainAddr = prop.getProperty("MASTER_IP");
        mainPort = prop.getProperty("SERVER_PORT");
        maincurrAddr = prop.getProperty("SERVER_IP");
    }

    public PeerServer(String FilePathForProp) throws IOException {
        this.FilePathForProp = FilePathForProp;
        Properties prop = loadProperties(FilePathForProp);
        initializeServerProperties(prop, 0);
      
    }

    public PeerServer(int peerServerId, String FilePathForProp) throws IOException {
        this.FilePathForProp = FilePathForProp;
        Properties prop = loadProperties(FilePathForProp);
        initializeServerProperties(prop, peerServerId);
        
    }


    private void printProperties() {
        System.out.println("Master Port: " + mainMasP);
        System.out.println("Master IP: " + mainAddr);
        System.out.println("My Port: " + mainPort);
        System.out.println("My IP: " + maincurrAddr);
    }

    public void printRegistrationStatus(int response) {
        if (response == 1) {
            System.out.println("Peer Server has been successfully registered.");
        } else if (response == 0) {
            System.out.println("Peer Server is already registered.");
        } else {
            System.out.println("Failed to register Peer Server.");
        }
    }





    public void run(){
        try
        {
            System.out.println("Peer Server started");
            Master masterAccess =
                    (Master)Naming.lookup("rmi://"+mainAddr+":"+mainMasP+"/master");
            int response = masterAccess.registerPeer("rmi://"+maincurrAddr+":"+mainPort+"/peer");

            printRegistrationStatus(response);

            // implementation class
            FDS obj = new fileAccessQuery(this.FilePathForProp);

            // rmiregistry within the server JVM with
            System.out.println("port:"+mainPort);
            LocateRegistry.createRegistry(Integer.parseInt(mainPort));

            // Binds the remote object by the name
            Naming.rebind("rmi://"+maincurrAddr+":"+mainPort+"/peer",obj);
        }
        catch(Exception ae)
        {
            ae.printStackTrace();
        }
    }
    public static void main(String args[]) throws IOException {
        new PeerServer("../resources/config.properties").run();
    }
}
