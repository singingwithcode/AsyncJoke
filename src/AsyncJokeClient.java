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

class Listener extends Thread {

	// Universal sock for thread
	DatagramSocket sock;

	// Constructor
	Listener(DatagramSocket s) {
		sock = s;
	}

	public void run() {

		// Create a buffer of bytes to store data
		byte[] buffer = new byte[6000];

		// Create a packet for buffer
		DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

		try {

			String s; // our message

			// Wait until we get something
			sock.receive(reply);

			// We got something, let's store it
			byte[] data = reply.getData();
			s = new String(data, 0, reply.getLength());

			// Tell the user that we have something!
			AsyncJokeClient.message = s;

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

public class AsyncJokeClient {

	// Set client to send UDP from clients put 1
	static int udpMode = 0;

	// Change as necessary, port
	static int port;
	static int firstServPort = 4545;

	static String message;

	public static void main(String args[]) {

		// To see which ports to use
		if (args.length < 1) {
			port = firstServPort;
		} else if (args.length == 1) {
			port = Integer.parseInt(args[0]);
		}

		System.out.println();
		if (udpMode == 0) {
			System.out.println("Client is making a TCP connection to Server through localhost at port " + port);
		} else if (udpMode == 1) {
			System.out.println("Client is making a UDP connection to Server through localhost at port " + port);
		}
		System.out.println();

		// Our socket
		DatagramSocket sock = null;

		// JokeKey
		// $ indicates first run
		String jokeKey = "$";

		// ProverbKey
		// $ indicates first run
		String proverbKey = "$";

		try {
			sock = new DatagramSocket();

			InetAddress host = InetAddress.getByName("localhost");

			while (true) {

				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("Press Return for joke or proverb: ");

				System.out.flush();
				in.readLine();

				// Compile message to send server
				message = jokeKey + " " + proverbKey + " " + sock.getLocalPort();

				// TCP CONNECTION ONLY
				if (AsyncJokeClient.udpMode == 0) {
					PrintStream toServer;
					Socket tCPsock = new Socket(host, port);
					toServer = new PrintStream(tCPsock.getOutputStream());
					toServer.println(message);

					tCPsock.close();
				}

				// UDP CONNECTION ONLY
				if (AsyncJokeClient.udpMode == 1) {
					// Convert to byte
					byte[] bite = message.getBytes();

					// Form the packet
					DatagramPacket dataPacket = new DatagramPacket(bite, bite.length, host, port);

					// Send the packet
					sock.send(dataPacket);
				}

				// Form and start thread for listening
				Listener listen = new Listener(sock);
				Thread t1 = new Thread(listen);
				t1.start();

				// Clear message so that we can check to see if thread returned
				// anything
				message = "";

				// Forever until message is found by listen() thread
				while (true) {

					// Each iteration, start sum at 0
					int sum = 0;

					// Check to see if message is here
					if (message != "") {

						// Message found!
						break;

					} else {
						// Perform addition
						System.out.println("Enter numbers to sum: ");
						String inputMessage;

						// Get input
						inputMessage = in.readLine();

						// Separate input by " "
						String[] result = inputMessage.split(" ");

						// Iterate through each number adding it along the way
						for (int i = 0; i < result.length; i++) {
							int r = Integer.parseInt(result[i]);
							sum = r + sum;
						}

						// Print to user
						System.out.println(sum);
						System.out.flush();
					}

				}

				// Grab Keys and assign
				String arr[] = message.split(" ", 3);
				jokeKey = arr[0];
				proverbKey = arr[1];

				// Print to client
				System.out.println(arr[2]);
			}
		}

		catch (IOException e) {
			System.err.println("IOException " + e);
		}
	}
}