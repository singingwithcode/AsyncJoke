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

import java.io.*; //I/O libraries
import java.net.*; //Java networking libraries

public class AsyncJokeAdminClient {
	
	static String secondServPortNum;
	static int firstAdminPortNum = 5050;
	static int secondAdminPortNum = 5051;
	static String firstServerName = "localhost";
	static String secondServerName = "localhost";
	static int secondServerMode = 0;
	
	public static void main(String args[]) {
		
		// To see which port to use
		if (args.length < 1) {
			System.out.println("Server one: localhost, port: " + firstAdminPortNum);
		} else if (args.length == 1) {
			firstServerName = args[0];
			System.out.println("Server one: " + args[0] + ", port: " + firstAdminPortNum);
		} else if (args.length == 2) {
			firstServerName = args[0];
			secondServerName = args[1];
			System.out.println("Server one: " + args[0] + ", port: " + firstAdminPortNum);
			System.out.println("Server two: " + args[1] + ", port: " + secondAdminPortNum);
		} 
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in)); //For input
		try {
			String inputMessage;
			do {
				System.out.println("Did you want to change the mode of the Server? Hit enter. Or type (quit) to exit"); 
				System.out.flush();
				
				//Answer not used right now
				inputMessage = in.readLine();
				
				//From input change server mode
				if (inputMessage.equals("s")) {
					if (secondServerMode == 0) {
						secondServerMode = 1;
						log("Switched to secondary server");
						System.out.println("Switched to secondary server.");
					} else {
						secondServerMode = 0;
						System.out.println("Switched to primary server.");
						log("Switched to primary server");
					}
				}
				
				//Spawn a thread
				if (inputMessage.indexOf("quit") < 0) 
					requestModeChange(firstServerName);
			} while (inputMessage.indexOf("quit") < 0);
			log("Cancelled by user request."); 
		} catch (IOException x) {
			log("Failed to get input from Admin.");
		}
	}

	//Establishes Connection and Prints Response 
	static void requestModeChange(String serverName) {
		Socket sock;
		BufferedReader fromServer;
		PrintStream toServer;
		String textFromServer;

		try {
			
			if (secondServerMode == 0) {
				sock = new Socket(firstServerName, firstAdminPortNum);
			} else {
				sock = new Socket(secondServerName, secondAdminPortNum);
			}

			//Creates filter I/O streams for the socket
			fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			toServer = new PrintStream(sock.getOutputStream());
			
			//send blank request for something at this time
			toServer.println();
			toServer.flush();
			
			//Reads in messages from the server and prints
			for (int i = 1; i <= 3; i++) {
				textFromServer = fromServer.readLine(); 
				if (textFromServer != null)
					System.out.println(textFromServer);
			} 
			sock.close();
		} catch (IOException x) {
			System.out.println("A connection could not be made at this time.");
		}
	}
	
	//Establishes connection and sends message to log 
	static void log(String message) {
		Socket sock;
		PrintStream toServer;
		try {
			
			if (secondServerMode == 0) {
				sock = new Socket(firstServerName, firstAdminPortNum);
			} else {
				sock = new Socket(secondServerName, secondAdminPortNum);
			}
			
			toServer = new PrintStream(sock.getOutputStream());
			
			//Sends machine answer to server
			toServer.println("ADMIN: " + message);
			toServer.flush();
			
			sock.close();
		} catch (IOException x) {
			System.out.println("Sending log failed.");
		}
	}
}