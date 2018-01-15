
/*--------------------------------------------------------

1. Name / Date: 

	Wenwen Zhang  09/23/2017

2. Java version used, if not the official version for the class: 

	build 1.8.0_121-b13

3. Precise command-line compilation examples / instructions:

	> javac JokeClientAdmin.java

4. Precise examples / instructions to run this program:

	Run the following files in separate shell windows:

	> java JokeServer
	> java JokeClient
	> java JokeClientAdmin

	All acceptable commands are displayed on the various consoles.
	
	For this JokeClient:
	
		If no argument is passed, it would be connected to the local host.
			> java JokeClientAdmin 
			
		If one argument is passed, it would be connected to the IP address 
		that the argument specified. 
			> java JokeClientAdmin 140.192.1.22
			
		If a second argument is presented, then the first argument is the 
		primary server, and the second one is the secondary server.
			> java JokeClientAdmin localhost 140.192.1.22

5. List of files needed for running the program.

	a. JokeServer.java
	b. JokeClient.java
	c. JokeClientAdmin.java

6. Notes:
	
	a. When connected to two servers, this admin client can switch servers, and 
	 there might be errors when trying to send signals to a server which is already 
	 shutdown by this admin. 

----------------------------------------------------------*/

import java.io.*; // Import the input/output package
import java.net.*; // Import the Java networking package

public class JokeClientAdmin
{
	// Two port number, port1 for primary server, port2 for secondary server.
	public static int port1 = 5050;
	public static int port2 = 5051;
	
	// A boolean flag to check if a secondary server is connected.
	public static boolean second = false;
	
	// A string flag to check if the current connected server is primary or secondary, primary is the default setting.
	public static String serverOn = "P";
	
	public static void main(String args[])
	{
		String primaryServer = null; // Primary server name.
		String secondServer = null; // Secondary server name.
		
		if (args.length < 1) // When no argument, local host is the default server.
			primaryServer = "localhost";
		else if (args.length == 1) // With one argument, the IP address passed is the primary server.
			primaryServer = args[0];
		else if (args.length > 1) // When there are two arguments, the first one is primary, the second one is secondary.
		{
			primaryServer = args[0];
			secondServer = args[1];
			second = true; // Turn on the flag to true saying a secondary server is connected.
			
			// Check if the two servers are different, if so, change the serverOn flag to secondary.
			if(primaryServer.equals(secondServer))
			{
				serverOn = "S";
			}
		}
		
		System.out.println("This is an Administration client.");
		System.out.println("Primary server: " + primaryServer + ", Port: " + port1 + "\n");
		
		// Print the information of the secondary server if present.
		if(second)
		{
			System.out.println("Secondary server: " + secondServer + ", Port: " + port2 + "\n");
		}
		
		//According the server flag, print if the currently connected server is primary or secondary.
		if(serverOn.equals("P"))
		{
			System.out.println("Currently is connected with primary server. \n");
		}
		else
		{
			System.out.println("Currently is connected with secondary server. \n");
		}
		
		// Create a BufferedReader object to store the user's input.
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		try
		{
			String entry; // To hold the input.
			
			do
			{
				System.out.print("Enter your operation: \n"
						+ "	'J' for Joke Mode,\n"
						+ "	'P' for Proverb Mode,\n"
						+ "	'S' to switch server, \n"
						+ "	'quit' to end, \n"
						+ "	'shutdown' to close the server: ");
				System.out.flush();
				
				//Get the input, and make it consistently upper case for easy comparison,
				entry = in.readLine().toUpperCase();  

				// Get the server name based on the serverOn flag.
				String toSend = (serverOn.equals("P")? primaryServer: secondServer);
				
				// If the input command is 'j' or 'p', switch the server mode.
				if(entry.equals("J") || entry.equals("P")) 
					changeServerMode(entry, toSend);
				
				// If the input command is 's', it means to switch servers.
				else if(entry.equals("S"))
				{
					if(second) // Do the following only when a secondary server is available.
					{
						// Switch the servers, and print summary messages on the console.
						if(serverOn.equals("P"))
						{
							serverOn = "S";
							System.out.println("Now admistrating: " + secondServer + ", port " + port2 + ".\n");
						}
						else
						{
							serverOn = "P";
							System.out.println("Now administraing: " + primaryServer + ", port " + port1 + ".\n");
						}
					}
					
					// Warning that there is no secondary available.
					else
					{
						System.out.println("No secondary server being used.\n");
					}
					
					
					// If two servers are the same, which means this admin is connected to different ports on a same 
					// server, send the 's' command to the server to change port.
					if(primaryServer.equals(secondServer)) 
					{
						changeServerMode(entry, primaryServer);
					}
				}
				
				// If the command is 'shutdown', close the server.
				else if(entry.equals("SHUTDOWN"))
				{
					changeServerMode(entry, toSend);
					System.out.println("Server " + toSend + " has been shut down.\n");					
				}
				
				// Ask for valid command if the input is not recognized.
				else if (!entry.equals("QUIT"))
				{
					System.out.println("Invalid input, please follow the instruction and re-enter.");
					System.out.flush();
				}				
			}
			while(!entry.equals("QUIT")); // If 'quit' is typed, close this client.

			System.out.println("AdminClient Cancelled."); 
			System.exit(0);
		}

		/* If anything goes wrong, catch the exception, and print out the error information,
		so that the program can keep running.
		*/
		catch(IOException x) 
		{
			x.printStackTrace();
		}
	}

	// This method is to send command typed to the connected server to manage the server.
	public static void changeServerMode(String mode, String serverName)
	{
		Socket sock; // Create a socket.
		
		BufferedReader fromServer;
		PrintStream toServer; 
		String textFromServer;
		int port = (serverOn.equals("P")? 5050: 5051); // Get the correct port number according to the serverOn flag.

		try
		{
			// Create a socket connecting to the the server and the specified port number passed in.
			sock = new Socket(serverName, port); 

			// Create a bufferedReader object to buffer the input stream got from the socket.
			fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			// Create a printStream to store the output stream that will be sending to the server.
			toServer = new PrintStream(sock.getOutputStream());
			
			// Tell the server what mode to be running.
			toServer.println(mode);
			toServer.flush();

			// Read the responses from the server after sending an operation command.
			textFromServer = fromServer.readLine(); 
			if (textFromServer != null)
				System.out.println(textFromServer);
				
			sock.close(); // Close the local socket.
		}
		catch(IOException x)
		{
			System.out.println("Socket error.");
			x.printStackTrace();
		}
	}

}