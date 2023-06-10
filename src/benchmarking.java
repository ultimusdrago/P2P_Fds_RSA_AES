import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class benchmarking implements Runnable {
    String fileAccess;

    // This method runs the benchmarking tool by creating a benchmarking object,
    // starting a master server, and running clients and servers.
    public static void runBenchmark(String fileAccess) throws IOException, InterruptedException, Exception {
        // Create a benchmarking object with the specified file access path.
        benchmarking benchMark = new benchmarking(fileAccess);
        // Create a master server object.
        MasterServer mserver = new MasterServer();
        // Start the master server with the specified file access path.
        mserver.run(fileAccess);
        // Run clients and servers with the benchmarking object.
        runClientsAndServers(benchMark);
    }

    // This method runs clients and servers with the specified benchmarking object.
    public static void runClientsAndServers(benchmarking benchMark) throws InterruptedException, IOException, Exception {
        // Create three clients with the benchmarking object.
        List<PeerClient> clients = benchMark.createClients(3);
        // Create three servers with the benchmarking object.
        List<PeerServer> servers = benchMark.createServers(3);
        // Run the servers with the benchmarking object.
        benchMark.runServers(servers);
        // Run benchmarks with the specified number of threads, clients, and servers.
        benchMark.runBenchMarks(3, clients, servers);
    }

    // This method runs benchmarks with the specified number of threads, clients, and servers.
    private void runBenchMarks(int mthread, List<PeerClient> c, List<PeerServer> s) throws InterruptedException {
        // Create a list of threads with the specified number of threads.
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < mthread; i++) {
            // Add a new thread with the specified client and file access path to the list of threads.
            threads.add(new Thread(new benchmarking(c.get(i), this.fileAccess)));
        }
        // Get the start time in milliseconds.
        long tstart = System.currentTimeMillis();
        // Start the threads and wait for them to finish.
        startThreadsAndWait(threads);
        // Get the end time in milliseconds.
        long tend = System.currentTimeMillis();
        // Print the total execution time in milliseconds.
        printExecutionTime(tend - tstart);
    }

    // This method runs the specified clients.
    private void runClients(List<PeerClient> clients) throws Exception {
        int numClients = clients.size();
        for (int i = 0; i < numClients; i++) {
            PeerClient client = clients.get(i);
            client.run();
        }
    }

    // This method runs the specified servers.
    private void runServers(List<PeerServer> servers) {
        int numServers = servers.size();
        for (int i = 0; i < numServers; i++) {
            PeerServer server = servers.get(i);
            server.run();
        }
    }

    // This method starts the specified threads and waits for them to finish.
    private void startThreadsAndWait(List<Thread> threads) throws InterruptedException {
        // Start the threads.
        for (Thread thread : threads) thread.start();
        // Wait for the threads to finish.
        for (Thread thread : threads) thread.join();
    }

    // This method prints the execution time in milliseconds
    private void printExecutionTime(long executionTimeInMillis) {
        System.out.print("Total time taken for execution is " + executionTimeInMillis + " ms");
    }

    // This method creates a list of PeerServer objects
    private List<PeerServer> createServers(int maxPeerServers) throws IOException {
        List<PeerServer> svers = new ArrayList<>();

        int id = 1;
        while (id <= maxPeerServers) {
            svers.add(new PeerServer(id, this.fileAccess)); // creates a new PeerServer object and adds it to the list
            id++; // increments the id counter
        }

        return svers; // returns the list of PeerServer objects
    }

    // This method creates a list of PeerClient objects
    private List<PeerClient> createClients(int maxPeerClients) throws Exception{
        List<PeerClient> clients = new ArrayList<>();
        for (int id = 1; id <= maxPeerClients; id++) {
            clients.add(new PeerClient(id)); // creates a new PeerClient object and adds it to the list
        }
        return clients; // returns the list of PeerClient objects
    }

    // This is a constructor for the benchmarking class that sets the file access path
    benchmarking(String propFilePath) {
        this.fileAccess = propFilePath; // sets the file access path
    }

    PeerClient p_client; // declares a PeerClient object

    // This is a constructor for the benchmarking class that sets the PeerClient object and file access path
    benchmarking(PeerClient p_client, String propFilePath) {
        this.p_client = p_client; // sets the PeerClient object
        this.fileAccess = propFilePath; // sets the file access path
    }

    // This method runs the benchmarking process
    @Override
    public void run() {
        System.out.println("Processing 10000 create requests");
        int maxRequests = 10000;
        for (int request = 1; request <= maxRequests; request++) {
            processRequest(request); // processes a request
        }
    }

    // This method processes a request by creating, reading, and writing a file
    public void processRequest(int request) {
        String filename = "" + request + "" + System.currentTimeMillis(); // generates a filename with the request number and current timestamp
        this.p_client.createFile(filename, "Create data: " + request); // creates a file with the generated filename and data
        this.p_client.readFile(filename); // reads the file
        this.p_client.writeFile(filename, "new data: " + request); // writes new data to the file
    }

    // This is the main method of the benchmarking class
    public static void main(String[] args) {
        String fileAccess = "./resources/benchmark.properties"; // sets the file access path
        benchmarking findings = null; // initializes a benchmarking object
        MasterServer m_server = null; // initializes a MasterServer object
        try {
            runBenchmark(fileAccess); // runs the benchmarking process
        } catch (Exception e) {
            System.err.println("Error in processing the request " + e.getMessage()); // prints an error message
            e.printStackTrace(); // prints the stack trace of the exception
        }
    }
}
