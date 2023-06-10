import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.*;
import java.rmi.server.*;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.rmi.RemoteException;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

//References: https://www.baeldung.com/java-aes-encryption-decryption
//References: https://www.baeldung.com/java-rsa
//References: https://www.baeldung.com/java-concurrency
//References: https://docs.oracle.com/en/java/javase/14/security/java-cryptography-architecture-jca-reference-guide.html



public class MasterQuery extends UnicastRemoteObject implements Master
{
    // lookup : Filename -> List of peers containing that file
    private static Map<String, Set<String>> lookup;
    // peers : Stores all the registered peers
    private Set<String> peers;
    // bin : to store what all files are deleted
    private Map<String, Boolean> isDeleted;
    // Permissions Hashmap to manage all the permissions related to a file
    private Map<String, Permissions> permissions;
    // Hashmap to manage encryption keys for each file
    private static Map<String, SecretKey> secretKeys;
    // Hashmap to store RSA public and private keys for each user
    private static Map<String, PublicKey> peerRSAPublicKey;
    // Replication Factor fetched from property file
    private Integer replicaFactor;

    // Default constructor to throw RemoteException
    // from its parent constructor
    public MasterQuery() throws IOException {
        super();
        lookup = new HashMap<>();
        peers = new HashSet<>();
        isDeleted = new HashMap<>();
        permissions = new HashMap<>();
        secretKeys = new HashMap<>();
        peerRSAPublicKey = new HashMap<>();
        Properties prop = new Properties();
        // prop.load(new FileInputStream("../resources/config.properties"));
        // Reading each property value
        // this.replicaFactor = Integer.parseInt(prop.getProperty("REPLICA_FACTOR"));
        this.replicaFactor = 3;
    }


    //Scheduler for malware check
    private final static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();


    @Override
    public Map.Entry<String, String> read(String fileName, String uri) throws RemoteException {
        try {
            String message;
            switch (hasFile(fileName) ? 1 : 0) {
                case 0:
                    message = fileName + " doesn't exist";
                    return new AbstractMap.SimpleEntry<>(message, null);
                case 1:
                    Permissions permissionObj = permissions.get(fileName);
                    switch (permissionObj.canRead(uri) ? 1 : 0) {
                        case 0:
                            message = "The peer doesn't have permission to read";
                            return new AbstractMap.SimpleEntry<>(message, null);
                        case 1:
                            List<String> paths = getPaths(fileName);
                            String peerPath = paths.get(0);
                            String key = Base64.getEncoder().encodeToString(secretKeys.get(fileName).getEncoded());
                            key = RSA.encrypt(key, peerRSAPublicKey.get(uri));
                            return new AbstractMap.SimpleEntry<>(peerPath, key);
                    }
            }
        } catch (NullPointerException | IllegalArgumentException | ClassCastException e) {
            System.err.println("Exception caught: " + e.getMessage());
            throw new RemoteException("Exception occurred while reading file", e);
        } catch (SecurityException e) {
            System.err.println("SecurityException caught: " + e.getMessage());
            throw new RemoteException("SecurityException occurred while reading file", e);
        } catch (Exception e) {
            System.err.println("Exception caught: " + e.getMessage());
            throw new RemoteException("Exception occurred while reading file", e);
        }
        return null; // unreachable statement
    }




    @Override
    public boolean updatePublicKey(String uri, PublicKey publicKey) throws RemoteException {
        switch (peerRSAPublicKey.containsKey(uri) ? 1 : 0) {
            case 1:
                System.out.println("Successfully updated the RSA public key");
                break;
            case 0:
                System.out.println("Successfully received peer RSA public key");
                break;
        }

        boolean success = false;
        try {
            peerRSAPublicKey.put(uri, publicKey);
            success = true;
        } catch (NullPointerException | IllegalArgumentException | UnsupportedOperationException | ClassCastException e) {
            System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        return success;
    }




    @Override
    public Map.Entry<Set<String>, String> create(String fileName, String uri) throws RemoteException {
        try {
            if (hasFile(fileName)){
                System.out.println(fileName + " already exists");
                return null;
            }
            Set<String> peersURI = getPaths_RF();
            Permissions permissionObj = new PermissionsImpl(fileName, uri);
            permissions.put(fileName, permissionObj);
            lookup.put(fileName, peersURI);
            isDeleted.put(fileName, false);
            secretKeys.put(fileName, AESEncrypt.newSecretKey());
            String key = Base64.getEncoder().encodeToString(secretKeys.get(fileName).getEncoded());
            try {
                key = RSA.encrypt(key, peerRSAPublicKey.get(uri));
                System.out.println(fileName + " data updated in the lookup table");
                return new AbstractMap.SimpleEntry<>(peersURI, key);
            } catch (NullPointerException e) {
                System.out.println("Public key not found for URI: " + uri);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public String delegatePermission(String fileName, String uri, String otherURI, String permission) throws RemoteException {
        try {
            if (!hasFile(fileName)) {
                return fileName + " doesn't exist";
            }

            Permissions permissionObj = permissions.get(fileName);
            String message = null;

            switch (permission) {
                case "read":
                    if (permissionObj.canRead(uri)) {
                        message = permissionObj.canRead(otherURI) ? "The other peer already has " + permission : null;
                        permissionObj.setRead(otherURI);
                    } else {
                        message = "The peer doesn't have " + permission + " permission";
                    }
                    break;
                case "write":
                    if (permissionObj.canWrite(uri)) {
                        message = permissionObj.canWrite(otherURI) ? "The other peer already has " + permission : null;
                        permissionObj.setWrite(otherURI);
                    } else {
                        message = "The peer doesn't have " + permission + " permission";
                    }
                    break;
                case "delete":
                    if (permissionObj.canDelete(uri)) {
                        message = permissionObj.canDelete(otherURI) ? "The other peer already has " + permission : null;
                        permissionObj.setWrite(otherURI);
                    } else {
                        message = "The peer doesn't have " + permission + " permission";
                    }
                    break;
                default:
                    message = "Invalid permission specified";
                    break;
            }
            return message;
        } catch (RemoteException e) {
            return "RemoteException occurred: " + e.getMessage();
        } catch (NullPointerException e) {
            return "NullPointerException occurred: " + e.getMessage();
        } catch (Exception e) {
            return "Exception occurred: " + e.getMessage();
        }
    }



    @Override
    public String delete(String fileName, String uri) throws RemoteException {
        String message = null;
        boolean hasFile = hasFile(fileName);
        boolean canDelete = false;
        String peerURI = null;
        if (hasFile) {
            Permissions permissionObj = permissions.get(fileName);
            canDelete = permissionObj.canDelete(uri);
            if (!canDelete) {
                message = "The peer doesn't have permission to delete/restore";
            } else {
                List<String> paths = getPaths(fileName);
                peerURI = paths.get(0);
                isDeleted.put(fileName, true);
            }
        } else {
            message = fileName + " doesn't exist";
        }
        if (message != null) {
            throw new RemoteException(message);
        }
        return peerURI;
    }


    @Override
    public Map.Entry<Map.Entry<String, String>, Set<String>> update(String fileName, String uri) throws RemoteException {
        Map.Entry<Map.Entry<String, String>, Set<String>> response;
        String message = "";
        boolean fileExists = hasFile(fileName);
        boolean canWrite = fileExists && permissions.get(fileName).canWrite(uri);
        if (!fileExists) {
            message = fileName + " doesn't exist";
        } else if (!canWrite) {
            message = "The peer doesn't have permission to write";
        }
        String key = "";
        Set<String> paths = null;
        if (canWrite) {
            try {
                key = Base64.getEncoder().encodeToString(secretKeys.get(fileName).getEncoded());
                key = RSA.encrypt(key, peerRSAPublicKey.get(uri));
                paths = new HashSet<>(getPaths(fileName));
            } catch (Exception e) {
                message = e.getMessage();
            }
        }
        response = new AbstractMap.SimpleEntry<>(
                new AbstractMap.SimpleEntry<>(message, key),
                paths);
        return response;
    }




    @Override
    public String restore(String fileName, String uri) throws RemoteException {
        String message = null;
        if(!hasFile(fileName)) {
            Permissions permissionObj = permissions.get(fileName);
            if(permissionObj.canWrite(uri)){
                List<String> paths = new ArrayList<>(lookup.get(fileName));
                String peerPath = paths.get(0);
                isDeleted.put(fileName, false);
                return peerPath;
            } else {
                message = "The peer doesn't have permission to delete/restore";
            }
        } else {
            message = fileName + " already exist";
        }
        throw new RemoteException(message);
    }



    @Override
    public boolean hasFile(String filename) throws RemoteException {
        if (lookup == null) {
            throw new RemoteException("Lookup table is null");
        }

        if (isDeleted == null) {
            throw new RemoteException("isDeleted table is null");
        }

        if (lookup.containsKey(filename) && !isDeleted.getOrDefault(filename, false)) {
            System.out.println("Lookup Successful \nMaster has " + filename);
            return true;
        } else {
            return false;
        }
    }


    @Override
    public String getPath() throws IOException {
        int size = peers.size();
        int item = size > 0 ? new Random().nextInt(size) : -1;
        int i = 0;
        Iterator<String> it = peers.iterator();
        while (it.hasNext()) {
            String peer = it.next();
            if (i == item) {
                return peer;
            }
            i++;
        }
        throw new IOException("No peer found");
    }


    public Set<Integer> getRandomNumbers(int replicaFactor, int size) {
        Set<Integer> nums = null;
        Random rand = new Random();

        if (replicaFactor <= 0 || size <= 0) {
            throw new IllegalArgumentException("Replica factor and size must be greater than 0");
        }

        try {
            nums = new HashSet<>();
            int i = 0;
            while (i < replicaFactor) {
                int num = rand.nextInt(size);
                while (nums.contains(num)) {
                    num = rand.nextInt(size);
                }
                nums.add(num);
                i++;
            }
        } catch (NullPointerException e) {
            System.err.println("NullPointerException caught: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException caught: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Exception caught: " + e.getMessage());
        }

        return nums;
    }


    public Set<String> getPaths_RF() {
        int size = peers.size();
        if (size <= this.replicaFactor) {
            return peers;
        }

        Set<Integer> randomIntegers = getRandomNumbers(this.replicaFactor, size);
        Set<String> newPeers = new HashSet<>();

        int index = 0;
        Iterator<String> iterator = peers.iterator();
        while (iterator.hasNext() && newPeers.size() < this.replicaFactor) {
            String peer = iterator.next();
            if (randomIntegers.contains(index)) {
                newPeers.add(peer);
            }
            index++;
        }
        return newPeers;
    }



    @Override
    public List<String> getPaths(String filename) throws RemoteException {
        if (hasFile(filename)) {
            Set<String> setOfPaths = lookup.get(filename);
            List<String> paths = new ArrayList<>(setOfPaths);
            return paths;
        } else {
            throw new RemoteException("File not found: " + filename);
        }
    }


    @Override
    public int registerPeer(String peerData) throws IOException {
        int result = -1;

        if (peerData == null || peerData.isEmpty()) {
            throw new IllegalArgumentException("peerData cannot be null or empty.");
        }

        if (!peers.contains(peerData)) {
            peers.add(peerData);
            result = 1;
        } else {
            result = 0;
        }

        return result;
    }



    public static boolean maliciousCheck() {
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Iterator<String> fileNamesIterator = lookup.keySet().iterator();
                while (fileNamesIterator.hasNext()) {
                    String fileName = fileNamesIterator.next();
                    Iterator<String> peerPathsIterator = lookup.get(fileName).iterator();
                    while (peerPathsIterator.hasNext()) {
                        FDS peerServer = null;
                        String fileData = null;
                        String peerPath = peerPathsIterator.next();
                        try {
                            // connect with server
                            peerServer = (FDS) Naming.lookup(peerPath);
                            fileData = peerServer.read(AESEncrypt.encryption(fileName, secretKeys.get(fileName)));
                        } catch (NotBoundException e) {
                            System.out.println("NotBoundException occurred while connecting with server: " + peerPath);
                            e.printStackTrace();
                        } catch (IOException e) {
                            System.out.println("IOException occurred while reading file data from server: " + peerPath);
                            e.printStackTrace();
                        } catch (Exception e) {
                            System.out.println("Exception occurred while connecting with server: " + peerPath);
                            e.printStackTrace();
                        }

                        if (fileData == null) {
                            System.out.println("Malicious activity detected in the Master Server......");
                            System.out.println("Exiting......");
                            System.exit(1);
                        }
                    }
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
        return true;
    }




}