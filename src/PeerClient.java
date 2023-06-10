import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.rmi.Naming;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.io.FileInputStream;

//References: https://www.youtube.com/results?search_query=java+RMI
//References: https://docs.oracle.com/javase/tutorial/rmi/
//References: https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html
//References: https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/security/package-summary.html

public class PeerClient {
    String masterPORT;
    String masterIP;
    String serverPORT;
    String serverIP;
    String myURI;
    Master masterServer;
    KeyPair rsaKeyPair;

    HashMap<String, String> usersCredentials = new HashMap<>();

    PeerClient(){
        try{
            Properties prop = new Properties();
            prop.load(new FileInputStream("../resources/config.properties"));
            //Fetching each property value
            this.masterPORT = prop.getProperty("MASTER_PORT");
            this.masterIP = prop.getProperty("MASTER_IP");
            this.serverPORT = prop.getProperty("SERVER_PORT");
            this.serverIP = prop.getProperty("SERVER_IP");
            this.myURI = "rmi://" + this.serverIP+":"+this.serverPORT + "/peer";
            this.masterServer =
                    (Master)Naming.lookup("rmi://"+this.masterIP+":"+this.masterPORT+"/master");
            this.rsaKeyPair = RSA.generateKeyPair();
            this.masterServer.updatePublicKey(this.myURI, rsaKeyPair.getPublic());
            System.out.println("Successfully generated RSA Public and private keys " +
                    "and shared with master server");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    PeerClient(int clientID){
        try{
            Properties prop = new Properties();
            prop.load(new FileInputStream("./resources/benchmark.properties"));
            //Fetching each property value
            this.masterPORT = prop.getProperty("MASTER_PORT");
            this.masterIP = prop.getProperty("MASTER_IP");
            this.serverPORT = prop.getProperty("SERVER_PORT_"+clientID);
            this.serverIP = prop.getProperty("SERVER_IP_"+clientID);
            this.myURI = "rmi://" + this.serverIP+":"+this.serverPORT + "/peer";
            this.masterServer =
                    (Master)Naming.lookup("rmi://"+this.masterIP+":"+this.masterPORT+"/master");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void createFile(String fileName, String fileData){
        try{
            //fetch IP and PORT of random peer
            Map.Entry<Set<String>, String> response = masterServer.create(fileName, myURI);
            Set<String> peersURI = response.getKey();
            String decryptedKey = RSA.decrypt(response.getValue(), rsaKeyPair.getPrivate());
            byte[] decodedKey = Base64.getDecoder().decode(decryptedKey);
            SecretKey key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
            if(peersURI == null) {
                System.out.println(fileName + " already exists");
                return;
            }
            // lookup method to find reference of remote object
            for(String peerURI : peersURI){
                FDS peerServer =
                        (FDS)Naming.lookup(peerURI);
                //System.out.println(AESEncrypt.encryption(fileName, key));
                peerServer.createnewfile(AESEncrypt.encryption(fileName, key),
                        AESEncrypt.encryption(fileData, key));
            }
            System.out.println("Successfully created " + fileName);
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    public void generateRSAPair() {
        try{
            this.rsaKeyPair = RSA.generateKeyPair();
            this.masterServer.updatePublicKey(this.myURI, this.rsaKeyPair.getPublic());
            PublicKey publicKey = this.rsaKeyPair.getPublic();
            PrivateKey privateKey = this.rsaKeyPair.getPrivate();

            // Print public key
            byte[] publicKeyBytes = publicKey.getEncoded();
            String publicKeyString = Base64.getEncoder().encodeToString(publicKeyBytes);
            //System.out.println("Public key: " + publicKeyString);

            // Print private key
            byte[] privateKeyBytes = privateKey.getEncoded();
            String privateKeyString = Base64.getEncoder().encodeToString(privateKeyBytes);
           // System.out.println("Private key: " + privateKeyString);
            System.out.println("Successfully updated the RSA Key value pair and " +
                    "shared the updated public key with master server");
        } catch(Exception e) {
            System.out.println(e);
        }
    }

    public void readFile(String fileName){
        try{
            Map.Entry<String, String> response = masterServer.read(fileName, myURI);
            String message = response.getKey();

            if(message.equals(fileName + " doesn't exist")){
                System.out.println(message);
                return;
            } else if(message.equals("The peer doesn't have permission to read")){
                System.out.println("You don't have permission to read");
                return;
            }
            String decryptedKey = RSA.decrypt(response.getValue(), rsaKeyPair.getPrivate());
            byte[] decodedKey = Base64.getDecoder().decode(decryptedKey);
            SecretKey key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

            // connect with server
            FDS peerServer =
                    (FDS)Naming.lookup(message);
            System.out.println("Encrypted file name");
            String fileData = peerServer.read(AESEncrypt.encryption(fileName, key));
            if(fileData==null){
                System.out.println("Failed to read " + fileName);
                return;
            }
            System.out.println("File Data : \n"+ AESEncrypt.decryption(fileData, key));
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void updateFile(String fileName, String newData){
        try {
            Map.Entry<Map.Entry<String, String>, Set<String>> response = masterServer.update(fileName, myURI);
            String message = response.getKey().getKey();
            String decryptedKey = RSA.decrypt(response.getKey().getValue(), rsaKeyPair.getPrivate());
            byte[] decodedKey = Base64.getDecoder().decode(decryptedKey);
            SecretKey key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
            Set<String> peersPath = response.getValue();
            if(message.equals(fileName + " doesn't exist")){
                System.out.println(response);
                return;
            } else if(message.equals("The peer doesn't have permission to write")){
                System.out.println("You don't have permission to write");
                return;
            }
            // connect with server
            for(String peer : peersPath){
                FDS peerServer =
                        (FDS)Naming.lookup(peer);
                String oldData = AESEncrypt.decryption(peerServer.read(AESEncrypt.encryption(fileName, key)), key);

                peerServer.writedatatofile(AESEncrypt.encryption(fileName, key),
                        AESEncrypt.encryption(oldData+newData, key));
            }
            System.out.println("Successfully updated the " + fileName + " data");
        }
            catch(Exception e){
            System.out.println(e);
        }
    }
    public void createDirectory(String directoryName){
        try {
            Map.Entry<Set<String>, String> response = masterServer.create(directoryName, myURI);
            Set<String> peersURI = response.getKey();
            String decryptedKey = RSA.decrypt(response.getValue(), rsaKeyPair.getPrivate());
            byte[] decodedKey = Base64.getDecoder().decode(decryptedKey);
            SecretKey key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

//            Map.Entry<Set<String>, SecretKey> response = masterServer.create(directoryName, myURI);
//            Set<String> peersURI = response.getKey();
//            SecretKey key = response.getValue();
            if(peersURI == null) {
                System.out.println(directoryName + " already exists");
                return;
            }
            // lookup method to find reference of remote object
            for(String peerURI : peersURI){
                FDS peerServer =
                        (FDS)Naming.lookup(peerURI);
                peerServer.createDirectory(AESEncrypt.encryption(directoryName, key));
            }
            System.out.println("Successfully created " + directoryName);
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
    public void writeFile(String fileName, String data){
        try {
            Map.Entry<Map.Entry<String, String>, Set<String>> response = masterServer.update(fileName, myURI);
            String message = response.getKey().getKey();
            String decryptedKey = RSA.decrypt(response.getKey().getValue(), rsaKeyPair.getPrivate());
            byte[] decodedKey = Base64.getDecoder().decode(decryptedKey);
            SecretKey key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

            Set<String> peers = response.getValue();
            if(message.equals(fileName + " doesn't exit")){
                System.out.println(response);
                return;
            } else if(message.equals("The peer doesn't have permission to write")){
                System.out.println("You don't have permission to write");
                return;
            }
            // connect with server
            for(String peer : peers){
                FDS peerServer =
                        (FDS)Naming.lookup(peer);
                peerServer.writedatatofile(AESEncrypt.encryption(fileName, key),
                        AESEncrypt.encryption(data, key));
            }
            System.out.println("Successfully wrote to the " + fileName);
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
    public void delete(String fileName){
        try{
            String response = masterServer.delete(fileName, myURI);
            if(response.equals(fileName + " doesn't exit")){
                System.out.println(response);
                return;
            } else if(response.equals("The peer doesn't have permission to delete/restore")){
                System.out.println("You don't have permission to delete");
                return;
            }

//            // connect with server
//            FDS peerServer =
//                    (FDS)Naming.lookup(response);
//            peerServer.delete(fileName);
            System.out.println("successfully deleted "+ fileName);
        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    public void restore(String fileName){
        try{
            String response = masterServer.restore(fileName, myURI);
            System.out.println(response);
            if(response.equals(fileName + " already exist")){
                System.out.println(response);
                return;
            } else if(response.equals("The peer doesn't have permission to delete/restore")){
                System.out.println("You don't have permission to delete/restore");
                return;
            }
//            FDS peerServer =
//                    (FDS)Naming.lookup(response);
//            peerServer.restore(fileName);
            System.out.println(fileName + " Successfully Restored ");
        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    public void delegatePermission(String fileName, String otherURI, String permission) {
        try{
            String response = masterServer.delegatePermission(fileName, myURI, otherURI, permission);
            System.out.println(response);
        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    public boolean validateUser(String username, String password){
        if(usersCredentials.containsKey(username)){
            if(!password.equals(usersCredentials.get(username))){
                System.out.println("invalid credentials: \n");
                System.exit(0);
                return false;

            }
        }else{
            System.out.println("invalid credentials: \n");
            System.exit(0);
            return false;
        }
        return true;
    }

    public void getUserDetails(){
        try{
            Scanner sc = new Scanner(System.in);
            System.out.println("Please login: \n");
            System.out.println("username : ");
            String username = sc.nextLine();
            System.out.println("Password : ");
            String password = sc.nextLine();
            if(validateUser(username, password)){
                System.out.println("User Authorized");
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void loadUsers(){
        try{
            File fileObj = new File("../resources/users.txt");
            Scanner reader = new Scanner(fileObj);
            String users[] = new String[2];
            int i=0;
            while (reader.hasNextLine()) {
                String data = reader.nextLine();
                users[i++] = data;
            }
            String usernames[] = users[0].split("=")[1].split(",");
            String passwords[] = users[1].split("=")[1].split(",");
            for(i=0;i<usernames.length;i++){
                usersCredentials.put(usernames[i], passwords[i]);
            }
            reader.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        String response;
        int userInput;
        Scanner sc = new Scanner(System.in);
        loadUsers();
        getUserDetails();

        try
        {
            System.out.println("Welcome to the Peer to Peer Distributed File System");
            while(true){
                System.out.println( "Please select one of the below options" + "\n" +
                        "1. Create file" + " " +
                        "2. Create Directory"+" "+
                        "3. Read file" + " " +
                        "4. Write file" + "\n" +
                        "5. Update file" + " " +
                        "6. Delete file" + " " +
                        "7. Restore file" + "\n" +
                        "8. Delegate permissions " +
                        "9. Generate RSA Pair " +
                        "10. Help " +
                        "11. Exit"
                );
                if(sc.hasNextInt())
                    userInput = Integer.parseInt(sc.nextLine());
                else {
                    sc.nextLine();
                    userInput = 0;
                }
                if(userInput == 1) {
                    System.out.println("Enter File name to be created - ");
                    String fileName = sc.nextLine();
                    System.out.println("Enter File data - ");
                    String fileData = sc.nextLine();
                    this.createFile(fileName, fileData);
                }
                else if (userInput == 2) {
                    System.out.println("Enter Directory name to be created - ");
                    String directoryName = sc.nextLine();
                    this.createDirectory(directoryName);
                } else if (userInput == 3){
                    System.out.println("Enter File name to read - ");
                    String fileName = sc.nextLine();
                    this.readFile(fileName);
                } else if (userInput == 4){
                    System.out.println("Enter File name to be updated - ");
                    String fileName = sc.nextLine();
                    System.out.println("Enter the file data - ");
                    String fileData = sc.nextLine();
                    this.writeFile(fileName, fileData);
                } else if (userInput == 5){
                    System.out.println("Enter File name to be updated - ");
                    String fileName = sc.nextLine();
                    System.out.println("Enter new data to be appended - ");
                    String fileData = sc.nextLine();
                    this.updateFile(fileName, fileData);
                } else if (userInput == 6){
                    System.out.println("Enter File name to delete - ");
                    String fileName = sc.nextLine();
                    this.delete(fileName);
                } else if (userInput == 7){
                    System.out.println("Enter File name to restore - ");
                    String fileName = sc.nextLine();
                    this.restore(fileName);
                } else if(userInput == 8){
                    System.out.println("Enter file name to delegate the permission");
                    String fileName = sc.nextLine();
                    System.out.println("Enter IP address and port ex:10.0.4.245:1099");
                    String uri = sc.nextLine();
                    uri = "rmi://" + uri +"/peer";
                    System.out.println("Enter permission i.e, read, write, delete");
                    String permission = sc.nextLine();
                    this.delegatePermission(fileName, uri, permission);
                } else if (userInput == 9){
                    this.generateRSAPair();
                }else if (userInput == 10){
                    this.displayHelp();
                } else if (userInput == 11){
                    System.out.println("Exiting out of file distributed system");
                    break;
                }
                else{
                    System.out.println("Invalid Choice, please enter correct choice");
                }
            };
        }
        catch(Exception ae)
        {
            System.out.println(ae);
        }
    }



    public static void main(String args[]) {
        new PeerClient().run();
    }

    public void displayHelp() {
        System.out.println(
                "1. Create file - create a file with the given name and given data" + "\n" +
                "2. Create Directory - create a new directory with the given name"  + "\n" +
                "3. Read file - read a file with the given name" + "\n" +
                "4. Write file - Replace the contents of the file with new given data" + "\n" +
                "5. Update file - Append new content to the existing contents of the file" + "\n" +
                "6. Delete file - Delete the file" + "\n" +
                "7. Restore file - Restore the file" + "\n" +
                "8. Delegate Permissions - Delegate read,write and delegate permissions to a given peer" + "\n" +
                "9. Generate RSA Pair - Generate a new pair of Public and Private keys and register the Public key with Master Server"
        );
    }
}
