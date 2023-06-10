import java.io.FileInputStream;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.*;
import java.io.FileInputStream;


//References: https://javaee.github.io/tutorial/rmi.html
//References: https://docs.oracle.com/javase/tutorial/rmi/index.html

public class MasterServer {
    public void run(String path){
        String masterPORT;
        String masterIP;

        try
        {
            Properties prop = new Properties();
            //ResourceBundle prop
            //  = ResourceBundle.getBundle("config.properties");
            prop.load(new FileInputStream(path));
            //Reading each property value
            masterPORT = prop.getProperty("MASTER_PORT");
            masterIP = prop.getProperty("MASTER_IP");

            // Create an object of the interface
            // implementation class
            Master obj = new MasterQuery();
//            MasterQuery.maliciousCheck();



            // rmiregistry within the server JVM with
            // port number 1901
            LocateRegistry.createRegistry(Integer.parseInt(masterPORT));

            // Binds the remote object by the name
            // geeksforgeeks
            Naming.rebind("rmi://"+masterIP+":"+masterPORT+"/master",obj);
            System.out.println("successfully started master server");
        }
        catch(Exception ae)
        {
            ae.printStackTrace();
        }
    }
    public static void main(String args[]) {
        new MasterServer().run("../resources/config.properties");
    }
}
