/*-------------------------------------------------------------------------
 * 
 * Author: singingwithcode
 * Date: March 1 2017
 * Java version used: build 1.8.0_111-b14
 * 
 *Precise command-line compilation examples / instructions:
 * 
 * 	To start the server:
 * 	> javac AsyncJokeServer.java
 * 	> java AsyncJokeServer
 * 
 * 	Or for a Server to declare a port:
 *  > javac AsyncJokeServer.java
 * 	> java AsyncJokeServer 3245
 * 
 * 	To start a client (shell 2):
 * 	> javac AsyncJokeClient.java
 * 	> java AsyncJokeClient
 * 
 * 	To start an admin client (shell 3):
 * 	> javac AsyncJokeAdminClient.java
 * 	> java AsyncJokeAdminClient
 * 
 *Files Needed:
 *	AsyncJokeServer.java
 *	AsyncJokeClient.java
 *	AsyncJokeAdminClient.java
 * 
 *Instructions:
 * General Notes:
 * 	--You can start any program in any order. You must run the files from the same directory.
 * 	--You are able to switch the client sending the server TCP or UDP connections. In order to do this
 * 	the server and client  must both be on the same udpMode. See further directions under Features.
 * 	
 * The Server:
 *	--Once compiled and ran, no other input is necessary.
 *	--The server will output a detailed JokeLogDetailed.txt in the directory in which 
 *	the server was ran. 
 *	
 * The Client:
 *	--Once compiled and ran, hit enter for the client to connect to the server with either TCP or UDP
 *	depending on the mode you set. 
 *	--You will be asked to input numbers so they can be added together 
 *	--There is a 40 second delay per joke/proverb. Note UDP packets can get lost. The program will wait
 *	until you have recied your summation answer and then output the joke/proverb. 
 *	--The joke/proverb will have a format of <J/P> (Joke or Proverb) with <A> <B> <C> or <D> (Examp.: JB)
 *	followed by the joke
 *	--Each joke is random, and would only appear once until all jokes are used.
 *
 *The Admin:
 *	--Once compiled and ran, you can hit enter to change the mode from Joke to Proverb or from Proverb
 *	back to Joke. 
 *	--Type exit to leave the program. 
 * 
 *Features:
 * DIRECTIONS ON HOW TO HAVE THE SERVER RECEIVE UDP PACKETS FROM CLIENTS INSTEAD OF A USING A TCP CONNECTION:
 * 	1. Change the global variable in AsyncJokeServer.java > AsyncJokeServer() > udpMode = 1
 * 	2. Change the global variable in AsyncJokeClient.java > AsyncJokeClient() > udpMode = 1
 * 	NOTE: To change the connection back to TCP, put udpMode back to 0
 * 
 * THE LOG:
 * 	--Will automatically appear in your home directory via JokeLogDetailed.txt. The Admin will send reports 
 * 	to the server from an underlying TCP connection. 
 * 
 * MULTIPLE SERVERS AND CLIENTS
 * 	--Can run multiple servers by declaring the port at each server - otherwise starting 2 servers with the
 * 	same port will have errors
 * 	Shell1:
 * 		> java AsyncJokeServer 3245
 * 	Shell2:
 * 		> java AsyncJokeServer 3246
 * 	--Can run multiple clients by starting them in different shells
 * 	Shell1:
 * 	> java AsyncJokeClient 3245
 * 	Shell2:
 * 	> java AsyncJokeClient 3246
 * -------------------------------------------------------------------------
 */


import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//Handles all incoming client requests to server
class ClientHandler extends Thread {

	// Global
	int port;

	// Constructor
	ClientHandler(int port) {
		this.port = port;
	}

	// Our universal lists for lines
	// These will hold all the jokes and proverbs
	static List<String> jokes = new ArrayList<>();
	static List<String> proverbs = new ArrayList<>();

	// Joke 0 or Proverb 1
	// The mode of the Server when it's started
	static int mode = 0;

	// Keep track of all incoming clients with [][]
	// First is ID, Second is if sent out, rest are jokes or proverbs
	static String[][] jokeAddresses;
	static String[][] proverbAddresses;

	// # of clients we can have at a time
	// Adjust as needed if resources allow
	static int clientAmount = 100;

	public void run() {
		try {

			// Adds all jokes in to [] structures
			// jokeAddresses and proverbAddresses
			addJokesProverbs();

			// Initialize the [][]s to keep track of clients
			String[][] jokeAddressesF = new String[clientAmount][jokes.size() + 2];
			String[][] proverbAddressesF = new String[clientAmount][proverbs.size() + 2];
			initializeValues(jokeAddressesF);
			initializeValues(proverbAddressesF);
			jokeAddresses = jokeAddressesF;
			proverbAddresses = proverbAddressesF;

			// Create the server socket at the desired port
			DatagramSocket servsock = new DatagramSocket(port);

			Socket sock = null;
			ServerSocket tCPservsock = null;
			tCPservsock = new ServerSocket(port, 6);

			// communication loop
			while (true) {
				
				// Create new buffer for packet
				// Had issues with buffer size because packet too large
				byte[] buffer = new byte[6000];

				// Form new packet to store incoming packet
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

				//TCP CONNECTION ONLY
				if (AsyncJokeServer.udpMode == 0) {
					sock = tCPservsock.accept();
				
					// Make clientWorker handle the request
					new ClientWorker(servsock, packet, sock).start();
				}
				
				//UDP CONNECTION ONLY
				if (AsyncJokeServer.udpMode == 1) {
					servsock.receive(packet);

					// Make clientWorker handle the request
					new ClientWorker(servsock, packet, sock).start();
				}
			}
		}

		catch (IOException e) {
			System.err.println("IOException " + e);
		}
	}

	// Helper to update the server mode
	// 0 is joke, 1 is proverb
	public static void changeMode() {
		if (mode == 0) {
			mode = 1;
		} else {
			mode = 0;
		}
	}

	// Helper () to assign all the keys/addresses
	// [0][y] where y 0->1 defining joke (0) or proverb (1)
	// [1][y] where y 0->z are all the keys
	// [x][y] where x is all lines and y is 0->1. (0) not used, (1) used
	private void initializeValues(String[][] ary) {
		int count = clientAmount;
		String sCount;
		for (int row = 0; row < ary.length; row++) {
			for (int column = 0; column < ary[row].length; column++) {
				sCount = String.valueOf(count);
				if (column == 0) {
					ary[row][column] = sCount;
					count++;
				} else {
					ary[row][column] = "0";
				}
			}
		}
	}

	// Helper () for printing [][], debugging
	public static void printAry() {
		String[][] ary = jokeAddresses;
		for (int row = 0; row < ary.length; row++) {
			for (int column = 0; column < ary[row].length; column++) {
				System.out.print(ary[row][column] + " ");
			}
			System.out.println();
		}
	}

	// Helper () to add all jokes
	// Adding jokes from a text file was originally made but the assignment
	// wouldn't allow me to
	// submit another file.
	private void addJokesProverbs() {
		jokes.add("An SQL query goes into a bar, walks up to two tables and asks, 'Can I join you?'");
		jokes.add("To understand what recursion is, you must first understand recursion.");
		jokes.add("Keyboard not found, press F1 to continue.");
		jokes.add("ASCII stupid question, get a stupid ANSI.");

		proverbs.add("In computer science, we stand on each others feet.");
		proverbs.add("The computer only crashes when printing a document that you haven't saved.");
		proverbs.add("Computers are not intelligent. They only think they are.");
		proverbs.add("Man is still the most extraordinary computer of all.");
		AsyncJokeServer.log("SERVER: Jokes imported successfully");
	}

}

// Handles each client request
class ClientWorker extends Thread {

	// Global
	DatagramSocket servsock = null;
	DatagramPacket packet = null;
	Socket sock;

	// Constructor
	ClientWorker(DatagramSocket servsock, DatagramPacket packet, Socket s) {
		this.servsock = servsock;
		this.packet = packet;
		this.sock = s;
	}

	public void run() {
		String message = "";
		
		// TCP CONNECTION ONLY
		if (AsyncJokeServer.udpMode == 0) {
			message = "";
		
			BufferedReader in = null; // For in from client side
			try {
				in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				message = in.readLine();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		
		// UDP CONNECTION ONLY
		if (AsyncJokeServer.udpMode == 1) {
			// Buffer for data
			byte[] data = new byte[packet.getLength()];
		
			// Get data from Client
			data = packet.getData();
		
			// Convert data to a message
			message = new String(data);
		}
		
		
		// Grab Keys and assign
		String arr[] = message.split(" ");
		String jokeKey = arr[0];
		String proverbKey = arr[1];
		
		
		// TCP CONNECTION ONLY
		if (AsyncJokeServer.udpMode == 0) {

			String sockStuff = "";
			sockStuff = arr[2];

			packet.setAddress(sock.getInetAddress());
			packet.setPort(Integer.parseInt(sockStuff));//sock.getPort());
		}
		
		// Create our response
		String response = generateResponse(jokeKey, proverbKey);
		
		AsyncJokeServer.log(response);

		// Form a packet to return to client
		DatagramPacket dp = new DatagramPacket(response.getBytes(), response.getBytes().length, packet.getAddress(),
				packet.getPort());

		// Sleep, 40,000 is 40 seconds
		try {
			Thread.sleep(40000);

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Send UDP Packet
		try {
			servsock.send(dp);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String generateResponse(String jokeKey, String proverbKey) {
		// Assign a key if needed
		if (ClientHandler.mode == 0 && jokeKey.startsWith("$")) { // Joke Mode
			// Search for next unused key
			for (int i = 0; i < ClientHandler.clientAmount; i++) {
				if (ClientHandler.jokeAddresses[i][1].equals("0")) {
					// Found Unused Key
					jokeKey = ClientHandler.jokeAddresses[i][0]; // assigns key
					// Mark it used
					ClientHandler.jokeAddresses[i][1] = "1";
					break;
				}
			}
			AsyncJokeServer.log("SERVER: Assigned joke key: " + jokeKey);
		} else if (ClientHandler.mode == 1 && proverbKey.startsWith("$")) { // Proverb
																			// Mode
			// Search for next unused key
			for (int i = 0; i < ClientHandler.clientAmount; i++) {
				if (ClientHandler.proverbAddresses[i][1].equals("0")) {
					// Found Unused Key
					proverbKey = ClientHandler.proverbAddresses[i][0]; // assigns
																		// key
					// Mark it used
					ClientHandler.proverbAddresses[i][1] = "1";
					break;
				}
			}
			AsyncJokeServer.log("SERVER: Assigned proverb key " + proverbKey);
		}

		// Figure out key row
		int keyRow = 0;
		if (ClientHandler.mode == 0) { // for jokes
			for (int i = 0; i < ClientHandler.clientAmount; i++) {
				if (ClientHandler.jokeAddresses[i][0].equals(jokeKey)) {
					keyRow = i;
					break;
				}
			}
		} else if (ClientHandler.mode == 1) { // for proverbs
			for (int i = 0; i < ClientHandler.clientAmount; i++) {
				if (ClientHandler.proverbAddresses[i][0].equals(proverbKey)) {
					keyRow = i;
					break;
				}
			}
		}

		// Get the state of the Client in the defined mode.
		// Form an int[] that has all the unused things
		List<Integer> indexes = new ArrayList<Integer>(); // the positions not
															// used yet
		if (ClientHandler.mode == 0) { // for jokes
			// add 2 because the first 2 in the [][] are key and ifUsed
			for (int i = 2; i < ClientHandler.jokes.size() + 2; i++) {
				if (ClientHandler.jokeAddresses[keyRow][i].equals("0")) {
					// Is not yet used
					indexes.add(i - 2); // preparing it for jokes[]
				}
			}
		} else if (ClientHandler.mode == 1) { // for proverbs
			for (int i = 2; i < ClientHandler.proverbs.size() + 2; i++) {
				if (ClientHandler.proverbAddresses[keyRow][i].equals("0")) {
					// Is not yet used
					indexes.add(i - 2); // preparing it for proverbs[]
				}
			}
		}

		// Randomize number and log + update pick
		int index = 0;
		if (ClientHandler.mode == 0) { // for jokes
			// if all the lines are already used
			if (indexes.isEmpty()) {
				// set all the lines back to available
				for (int i = 2; i < ClientHandler.jokes.size() + 2; i++) {
					ClientHandler.jokeAddresses[keyRow][i] = "0";
					indexes.add(i - 2);
				}
			}
			// Generate random number
			Random randomGenerator = new Random();
			index = randomGenerator.nextInt(indexes.size());
			// Mark index as used
			ClientHandler.jokeAddresses[keyRow][indexes.get(index) + 2] = "1";
		} else if (ClientHandler.mode == 1) { // for proverbs
			// if all the lines are already used
			if (indexes.isEmpty()) {
				// set all the lines back to available
				for (int i = 2; i < ClientHandler.proverbs.size() + 2; i++) {
					ClientHandler.proverbAddresses[keyRow][i] = "0";
					indexes.add(i - 2);
				}
			}
			// Generate random number
			Random randomGenerator = new Random();
			index = randomGenerator.nextInt(indexes.size());
			// Mark index as used
			ClientHandler.proverbAddresses[keyRow][indexes.get(index) + 2] = "1";
		}

		// Create line identifier i.e. JA, JB, .. PA, PD
		// Only for formatting output to assignment liking
		String identifier = null;

		if (ClientHandler.mode == 0) { // for jokes
			char letterNum = 0;
			if (indexes.get(index) == 0) {
				letterNum = 'A';
			} else if (indexes.get(index) == 1) {
				letterNum = 'B';
			} else if (indexes.get(index) == 2) {
				letterNum = 'C';
			} else if (indexes.get(index) == 3) {
				letterNum = 'D';
			}
			identifier = "J" + letterNum;
		} else if (ClientHandler.mode == 1) { // for proverbs
			char letterNum = 0;
			if (indexes.get(index) == 0) {
				letterNum = 'A';
			} else if (indexes.get(index) == 1) {
				letterNum = 'B';
			} else if (indexes.get(index) == 2) {
				letterNum = 'C';
			} else if (indexes.get(index) == 3) {
				letterNum = 'D';
			}
			identifier = "P" + letterNum;
		}

		// Get Joke
		String retval = null;
		if (ClientHandler.mode == 0) { // for jokes
			retval = identifier + ": " + ClientHandler.jokes.get(indexes.get(index));
			AsyncJokeServer.log(
					"SERVER: Sent " + jokeKey + " Joke line: " + indexes.get(index) + " Identifier: " + identifier);
		} else if (ClientHandler.mode == 1) { // for proverbs
			retval = identifier + ": " + ClientHandler.proverbs.get(indexes.get(index));
			AsyncJokeServer.log("SERVER: Sent " + proverbKey + " Proverb line: " + indexes.get(index) + " Identifier: "
					+ identifier);
		}

		// Compile response
		return jokeKey + " " + proverbKey + " " + retval;
	}
}

// Handles all incoming admin requests to server
class AdminHandler extends Thread {

	// Constructor
	int port;
	int q_len;

	AdminHandler(int port) {
		this.q_len = 6;
		this.port = port;
	}

	// Run Thread
	public void run() {

		Socket sock = null;

		ServerSocket servsock = null;
		try {
			servsock = new ServerSocket(port, q_len);
		} catch (IOException e) {
			AsyncJokeServer.log("Unable to generate servSock");
		}

		// Run admin thread forever
		while (true) {
			try {
				sock = servsock.accept(); // Waiting for admin connection
			} catch (IOException e) {
				AsyncJokeServer.log("Unable to accept()");
			}
			new AdminWorker(sock, port).start(); // Sends worker to handle it
		}

	}

}

//Handles all tasks from AdminHandler
class AdminWorker extends Thread {
	Socket sock; // This is a socket for networking

	// Constructor
	AdminWorker(Socket s, int port) {
		sock = s; // Assigns s to Worker's socket
	}

	// Our Worker working
	public void run() {
		PrintStream out = null;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintStream(sock.getOutputStream());
			try {
				String message = in.readLine();

				//See if this is a log report or mode change
				String arr[] = message.split(" ");
				if (arr[0].equals("ADMIN:") || arr[0].equals("CLIENT:")) {
					// Log Report
					AsyncJokeServer.log(message);
				} else {
					// calling for a mode change
					changeMode(message, out);
					printModeChange(out);
				}

			} catch (IOException x) {
				AsyncJokeServer.log("SERVER: read error");
			}
			sock.close();
		} catch (IOException ioe) {
			AsyncJokeServer.log("SERVER: " + ioe);
		}
	}

	// Prints on the Admin side
	public void printModeChange(PrintStream out) {
		if (ClientHandler.mode == 0) {
			out.println("Boom! Done. Now Server is in Joke Mode.");
		} else {
			out.println("Boom! Done. Now Server is in Proverb Mode.");
		}
	}

	// Changes the Mode of JokeServer
	public void changeMode(String answer, PrintStream out) {
		// Change Mode
		ClientHandler.changeMode();
		AsyncJokeServer.log("SERVER: Changed mode to: " + ClientHandler.mode);
	}
}

public class AsyncJokeServer {
	
	//Set server to receive UDP from clients put 1
	static int udpMode = 0;

	//Server and Admin default ports
	static int serverPort = 4545;
	static int adminPort = 5050;

	public static void main(String a[]) throws InterruptedException {
		
		if (a.length < 1) {
			serverPort = 4545;
			adminPort = 5050;
		} else if (a.length == 1) {
			serverPort = Integer.parseInt(a[0]);
			adminPort = 5050;
		}
		
		System.out.println();
		if (udpMode == 0) {
			System.out.println("SERVER is accepting TCP connections only from Clients at port " + serverPort + " and Admin TCP connections at " + adminPort);
			log("SERVER: is accepting TCP connections only from Clients at port " + serverPort + " and Admin TCP connections at " + adminPort);
		} else if (udpMode == 1) {
			System.out.println("SERVER is accepting UDP connections only from Clients at port " + serverPort + " and Admin TCP connections at " + adminPort);
			log("SERVER: is accepting UDP connections only from Clients at port " + serverPort + " and Admin TCP connections at " + adminPort);
		}
		System.out.println();
		System.out.println("See a detailed report of server communication in JokeLogDetailed.txt in the directory it was ran.");

		// Create an Object of each class to spawn
		ClientHandler clientSocket = new ClientHandler(serverPort);
		AdminHandler adminSocket = new AdminHandler(adminPort);

		// Compile the two Threads to spawn
		Thread t1 = new Thread(clientSocket);
		Thread t2 = new Thread(adminSocket);

		// Spawn the Threads
		t1.start();
		t2.start();
	}

	// Helper to print to JokeLog.txt
	public static void log(String message) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter("JokeLogDetailed.txt", true), true);
		} catch (IOException e) {
			log("Logging error");
		}
		out.write(message + System.lineSeparator());
		out.close();
	}

}