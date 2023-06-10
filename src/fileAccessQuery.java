// Import necessary libraries
import java.io.*;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.List;
import java.util.Properties;

//reference:https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Callable.html
//reference: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/FutureTask.html

// Define a class named fileAccessQuery that extends UnicastRemoteObject and implements FDS
public class fileAccessQuery extends UnicastRemoteObject implements FDS {

    // Declare the instance variables
    public HashMap<String, Boolean> isAvailable;
    public int rnumber;

    // Constructor that throws RemoteException from its parent constructor
    private Properties loadProperties(String propFilePath) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream(propFilePath));
        return prop;
    }

    // Constructor that initializes instance variables and reads the property file
    fileAccessQuery(String propFilePath) throws IOException {
        super();
        isAvailable = new HashMap<>();
        Properties prop = loadProperties(propFilePath);
        //Reading each property value
        this.rnumber = Integer.parseInt(prop.getProperty("REPLICA_FACTOR"));
    }

    // Override the read method of FDS interface
    @Override
    public String read(String fname) throws Exception {
        // Create a FutureTask and a new thread to execute the readInternal method in parallel
        FutureTask<String> r_task = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return readInternal(fname);
            }
        });
        new Thread(r_task).start();
        return r_task.get();
    }

    // Define a private method readInternal to read the contents of a file
    private String readInternal(String fname) throws IOException {
        // Check if the file exists and is available for reading
        if (isAvailable.containsKey(fname) && !isAvailable.get(fname)) {
            File f = new File(fname);
            // If the file is a directory, return an empty string
            if (f.isDirectory()) return "";
            return Files.readString(Path.of(fname));
        }
        return null;
    }

    // Override the createnewfile method of FDS interface
    @Override
    public String createnewfile(String fname, String n_data) throws Exception {
        // Create a FutureTask and a new thread to execute the createFile method in parallel
        FutureTask newfiletask = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return createFile(fname, n_data);
            }
        });
        new Thread(newfiletask).start();
        return (String) newfiletask.get();
    }

    // Define a public method createFile to create a new file with the given name and data
    public String createFile(String fname, String data) throws Exception {
        try {
            // Write the data to the file
            writeToFile(fname, data);
            System.out.println("Successfully created " + fname);
            return fname;
        } catch (IOException io) {
            io.printStackTrace();
        }
        return null;
    }

    // Define a private method writeToFile to write the data to a file
    private void writeToFile(String fname, String data) throws IOException {
        FileWriter n_write = new FileWriter(fname);
        isAvailable.put(fname, false);
        n_write.write(data);
        n_write.close();
    }


    @Override
    public String createDirectory(String dname) throws Exception {
        // Create a new FutureTask with a new instance of the CreateDirectoryCallable class
        FutureTask<String> c_t = new FutureTask<>(new CreateDirectoryCallable(dname));
        // Start a new thread to run the FutureTask
        new Thread(c_t).start();
        // Wait for the FutureTask to complete and return the result
        return c_t.get();
    }

    // Private inner class that implements the Callable interface
    private class CreateDirectoryCallable implements Callable<String> {
        // Instance variable to store the directory name
        private final String dname;

        // Constructor that takes the directory name as a parameter and stores it in the instance variable
        public CreateDirectoryCallable(String d_name) {
            this.dname = d_name;
        }

        @Override
        public String call() throws Exception {
            // Try to create the directory using the helper method and return the directory name if successful
            try {
                createDirectoryHelper(dname);
                return dname;
            } catch (Exception io) {
                // Print the stack trace if there is an exception and return null
                io.printStackTrace();
            }
            return null;
        }

        // Private helper method that actually creates the directory
        private void createDirectoryHelper(String directoryName) {
            try {
                // Create a new File object with the given directory name
                File directory = new File(directoryName);
                // Set the availability of the directory to false in the isAvailable map
                isAvailable.put(directoryName, false);
                // Create the directory using the mkdirs method of the File class
                directory.mkdirs();
                // Print a success message to the console
                System.out.println("Successfully created a directory with name as: " + directoryName);
            } catch (Exception io) {
                // Print the stack trace if there is an exception
                io.printStackTrace();
            }
        }
    }

    // Method that writes data to a file using a new thread and a FutureTask
    public void writeToFile1(String filename, String data) throws Exception {
        // Create a new FutureTask with a new instance of an anonymous class that implements the Callable interface
        FutureTask writeTask = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                // Try to write the data to the file and return the filename if successful
                try {
                    FileWriter myWriter = new FileWriter(filename);
                    myWriter.write(data);
                    myWriter.close();
                    System.out.println("Successfully wrote to the " + filename);
                    return filename;
                } catch (IOException io) {
                    // Print the stack trace if there is an exception and return null
                    io.printStackTrace();
                }
                return null;
            }
        });
        // Start a new thread to run the FutureTask
        new Thread(writeTask).start();
    }

    @Override
    public String writedatatofile(String filename, String data) throws Exception {
        // Call the writeToFile1 method with the given filename and data
        writeToFile1(filename, data);
        // Create a new FutureTask with a new instance of an anonymous class that implements the Callable interface
        FutureTask writeTask = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                // Return the filename
                return filename;
            }
        });
        // Run the FutureTask
        writeTask.run();
        // Wait for the FutureTask to complete and return the result
        return (String) writeTask.get();
    }
}
