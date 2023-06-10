
// Import required Java libraries for encryption and decryption
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Reference : https://www.geeksforgeeks.org/what-is-java-aes-encryption-and-decryption/
// Reference : https://www.section.io/engineering-education/implementing-aes-encryption-and-decryption-in-java/


// Create an AES encryption class
public class AESEncrypt {    private static byte[] aeskey; // Initializing the AES key as a byte array
    private final static int keysize = 128; // Setting the key size for AES encryption to 128 bits

    private static Cipher encrypt_cipher, decrypt_cipher; // Initializing the Cipher objects for encryption and decryption
    private static SecretKeySpec seckey; // Initializing the SecretKeySpec object for holding the secret key

    // Static block to initialize the Cipher objects for encryption and decryption
    static {
        try {
            initializeCiphers(encrypt_cipher, decrypt_cipher);
           } catch (NoSuchAlgorithmException | NoSuchPaddingException e) { // Catching any NoSuchAlgorithmException and NoSuchPaddingException that may occur
            throw new RuntimeException(e); // Throwing a RuntimeException if any of these exceptions occur
        }
    }
    public static void initializeCiphers(Cipher encrypt_cipher, Cipher decrypt_cipher) throws NoSuchAlgorithmException, NoSuchPaddingException {
        encrypt_cipher = Cipher.getInstance("AES/GCM/NoPadding"); // Creating the Cipher object for encryption
        decrypt_cipher = Cipher.getInstance("AES/GCM/NoPadding"); // Creating the Cipher object for decryption
    }

    // Method to generate a new secret key
    public static SecretKey newSecretKey() {
        try {
            SecretKey secretKey = generateAESKey(keysize);
            return secretKey;
        } catch (Exception e) { // Catching any exception that may occur
            System.out.printf("An error occurred while generating secret key: " + e.getMessage()); // Printing an error message if an exception occurs
            return null; // Returning null if an exception occurs
        }

    }
    public static SecretKey generateAESKey(int keysize) throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("AES"); // Creating a KeyGenerator object for AES
        generator.init(keysize); // Initializing the KeyGenerator with the specified key size
        SecretKey aeskey = generator.generateKey(); // Generating a new AES secret key
        return aeskey; // Returning the new secret key
    }

    // Method to generate the final AES key
    public static void keyfinal(final String secret_key) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest variable = null; // Initializing the MessageDigest object
        aeskey = secret_key.getBytes("UTF-8"); // Converting the secret key to a byte array
        variable = MessageDigest.getInstance("SHA-1"); // Creating a MessageDigest object for SHA-1
        aeskey = variable.digest(aeskey); // Hashing the secret key with SHA-1
        aeskey = Arrays.copyOf(aeskey, 16); // Truncating the byte array to 16 bytes (128 bits)
        seckey = new SecretKeySpec(aeskey, "AES"); // Creating a SecretKeySpec object with the truncated byte array and "AES" as the algorithm
    }



    public static String encryption(String message, SecretKey aeskey) throws Exception {
        String secret = Base64.getEncoder().encodeToString(aeskey.getEncoded()); // Get the encoded key
        keyfinal(secret); // Initialize the SecretKey from the encoded key
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // Create a new Cipher instance for AES encryption
        cipher.init(Cipher.ENCRYPT_MODE, seckey); // Initialize the Cipher for encryption with the SecretKey
        String response = Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes("UTF-8"))); // Encrypt the message
        return response.replace("/", "1029"); // Replace forward slashes with a custom delimiter
    }

    // Method to decrypt the message with the SecretKey
    public static String decryption(String message, SecretKey aeskey) throws Exception {
        message = message.replace("1029", "/"); // Replace the custom delimiter with forward slashes
        String secret = Base64.getEncoder().encodeToString(aeskey.getEncoded()); // Get the encoded key
        keyfinal(secret); // Initialize the SecretKey from the encoded key
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // Create a new Cipher instance for AES decryption
        cipher.init(Cipher.DECRYPT_MODE, seckey); // Initialize the Cipher for decryption with the SecretKey
        return new String(cipher.doFinal(Base64.getDecoder().decode(message))); // Decrypt the message
    }

    public static void printdata(String message1, String message2){
        System.out.printf("Encrypted Message is : " + message1);
        System.out.printf("Decrypted Message is: " + message2);

    }
    public static void performEncryptionDecryption() throws Exception {
        AESEncrypt aes = new AESEncrypt();
        SecretKey aeskey = aes.newSecretKey(); // Generate a new SecretKey
        String encryptedMessage1 = aes.encryption("1", aeskey);
        String encryptedMessage2 = aes.encryption("2", aeskey); // Encrypt a message with the SecretKey
        String decryptedMessage1 = aes.decryption(encryptedMessage1, aeskey);
        String decryptedMessage2 = aes.decryption(encryptedMessage2, aeskey);

        printdata(encryptedMessage1, decryptedMessage1);
        printdata(encryptedMessage2, decryptedMessage2);
    }

    // Main method to test the encryption and decryption
    public static void main(String[] args) throws Exception {
        performEncryptionDecryption();
    }




}


