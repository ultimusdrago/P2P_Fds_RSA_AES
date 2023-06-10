import java.rmi.Remote;                   // Importing the Remote interface
import java.rmi.RemoteException;           // Importing the RemoteException class
import java.util.List;                     // Importing the List interface

public interface FDS extends Remote {      // Declaring an interface named FDS that extends the Remote interface
    // Declaring method prototypes that can throw an Exception, as required by the Remote interface
    public String read(String filename) throws Exception;
    public String createnewfile(String filename, String data) throws Exception;
    public String createDirectory(String directoryname) throws Exception;

    // Declaring another method prototype that can throw an Exception
    public String writedatatofile(String filename, String data) throws Exception;
}
