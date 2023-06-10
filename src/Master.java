// Importing necessary libraries
import javax.crypto.SecretKey;
import java.io.IOException;
import java.rmi.*;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;


//References: https://docs.oracle.com/en/java/javase/14/docs/api/java.rmi.html
//References:  https://docs.oracle.com/javase/tutorial/rmi/index.html
//References:  https://docs.oracle.com/en/java/javase/14/security/java-cryptography-architecture-jca-reference-guide.html
//References:  https://docs.oracle.com/en/java/javase/14/security/java-cryptography-architecture-jca-reference-guide.html


// Remote interface for the master server
public interface Master extends Remote {
    // Method to check if a file with the given filename is present
    // on the server
    public boolean hasFile(String filename) throws RemoteException;

    // Method to get a list of paths where a file with the given filename
    // is present on the server
    public List<String> getPaths(String filename) throws RemoteException;

    // Method to get a path on the server
    public String getPath() throws IOException;

    // Method to register a peer on the server and return an integer
    public int registerPeer(String peerData) throws IOException;

    // Method to read a file from the server and return its data
    public Map.Entry<String, String> read(String fileName, String uri) throws RemoteException;

    // Method to create a file on the server and return a set of peers that
    // the file is replicated on
    public Map.Entry<Set<String>, String> create(String fileName, String uri) throws RemoteException;

    // public Map.Entry<Map.Entry<String, SecretKey>, Set<String>> write(String fileName, String uri) throws RemoteException;

    // public Set<String> createDirectory(String fileName, String uri) throws RemoteException;
    // Method to delete a file from the server and return a message
    public String delete(String fileName, String uri) throws RemoteException;

    // Method to update a file on the server and return a map with the new data
    // and the set of peers that the file is replicated on
    public Map.Entry<Map.Entry<String, String>, Set<String>> update(String fileName, String uri) throws RemoteException;

    // Method to restore a file on the server and return a message
    public String restore(String fileName, String uri) throws RemoteException;

    // Method to delegate a permission for a file from one user to another and
    // return a message
    public String delegatePermission(String fileName, String uri, String otherURI, String permission) throws RemoteException;

    // Method to update the public key of a user on the server and return a boolean
    public boolean updatePublicKey(String uri, PublicKey publicKey) throws RemoteException;
}