
/*--------------------------------------------------------

1. Name / Date: 

	Wenwen Zhang  09/23/2017

2. Java version used, if not the official version for the class: 

	build 1.8.0_121-b13

3. Precise command-line compilation examples / instructions:

	> javac JokeClient.java

4. Precise examples / instructions to run this program:

	Run the following files in separate shell windows:

	> java JokeServer
	> java JokeClient
	> java JokeClientAdmin

	All acceptable commands are displayed on the various consoles.
	
	For this JokeClient:
	
		If no argument is passed, it would be connected to the local host.
			> java JokeClient 
			
		If one argument is passed, it would be connected to the IP address 
		that the argument specified. 
			> java JokeClient 140.192.1.22
			
		If a second argument is presented, then the first argument is the 
		primary server, and the second one is the secondary server.
			> java JokeClient localhost 140.192.1.22

5. List of files needed for running the program.

	a. JokeServer.java
	b. JokeClient.java
	c. JokeClientAdmin.java

6. Notes:
	
	a. The client keeps two states, one for primary server, the other for 
	secondary server.
	b. User can switch servers by typing "s".
	c. The client gets notified by the server about whether it is Joke mode 
	or Proverb mode currently, and then print out the corresponding output 
	according to the updated state sent from the server.

----------------------------------------------------------*/


import java.io.*; // Import the input/output package
import java.net.*; // Import the Java networking package
import java.util.UUID; // Import this package to enable the generation of UUID.

public class JokeClient
{
	// Maintain two states, one for the primary server, the other for the secondary server.
	// A state is a 8-bits integer 00000000, the left four bits indicate the joke state, while the right four bits are for proverb.
	public static int state1 = 0x00;
	public static int state2 = 0x00;
	
	// Initialize a string to hold the user's name.
	public static String name = null;
	
	// Masks to get the corresponding four bits for J/P mode.
	public static final int JMask = 0xF0;
	public static final int PMask = 0x0F;
	
	// Initialize a string to hold the server's mode, either Joke mode or Proverb mode.
	public static String mode = null;
	
	// Two port number, port1 for primary server, port2 for secondary server.
	public static final int port1 = 4545;
	public static final int port2 = 4546;
	
	// A string flag to check if the current connected server is primary or secondary, primary is the default setting.
	public static String serverOn = "P";
	
	// A boolean flag to check if the two servers connected are different.
	public static boolean differentServer = false;
	
	
	public static void main(String args[])
	{
		final String ID = UUID.randomUUID().toString(); // Generate an unique id and convert it to a string. 
		
		String primaryServer = null; // Primary server name.
		String secondServer = null; // Secondary server name.
		boolean second = false; // A boolean flag to check if a secondary server is connected.
		
		if (args.length < 1) // When no argument, local host is the default server.
			primaryServer = "localhost";
		else if (args.length == 1) // With one argument, the IP address passed is the primary server.
			primaryServer = args[0];
		else if (args.length > 1) // When there are two arguments, the first one is primary, the second one is secondary.
		{
			primaryServer = args[0];
			secondServer = args[1];
			second = true; // Turn on the flag to true saying a secondary server is connected.
			
			// Check if the two servers are different, if so, turn on the flag.
			if (!primaryServer.equals(secondServer)) 
			{
				differentServer = true;
			}
		}

		System.out.println("Wenwen Zhang's JokeClient. \n");
		System.out.println("Primary server: " + primaryServer + ", Port: " + port1 + ".\n");
		
		// Print the information of the secondary server if present.
		if(second)
		{
			System.out.println("Secondary server: " + secondServer + ", Port: " + port2 + ".\n");
		}

		// To get the user's name.
		BufferedReader nameHolder = new BufferedReader(new InputStreamReader(System.in));
		
		// To get the signals that will be sent to the server.
		BufferedReader inputHolder = new BufferedReader(new InputStreamReader(System.in));

		try
		{
			System.out.print("Enter your name: ");
			System.out.flush();
			name = nameHolder.readLine(); // Get the user's name, and print it out in following outputs.
			System.out.println();

			String entry = null; // To store the operation user typed in.
		
			do
			{
				System.out.print("Choose your operation:\n"
						+ "	Enter to get a response,\n "
						+ "	'S' to switch server,\n"
						+ "	'quit' to end           : ");
				System.out.flush();
				entry = inputHolder.readLine(); // Read the input.

				// If the input is nothing, send a request to the server and get a joke/proverb.
				if(entry.equals("")) 
				{
					// Send a request to the secondary server when there is a secondary server 
					// and it is different from the primary server.
					if(serverOn.equals("S") && differentServer)  
					{		
						getSomething(secondServer, port2, ID, state2);
					}
					// Send a request to the primary server with state1.
					else
					{
						getSomething(primaryServer, port1, ID, state1);
					}
				}
				// If the input is 's', switch the server.
				else if(entry.toLowerCase().equals("s")) // make the input consistently lower case so that it can be compared with 's'.
				{
					if(second) // Do the following only when a secondary server is connected.
					{
						// Switch the servers, and print summary messages on the console.
						
						if(serverOn.equals("P"))
						{
							serverOn = "S";
							System.out.println("Now communicating with: " + secondServer + ", port " + port2 + "\n");
						}
						else
						{
							serverOn = "P";
							System.out.println("Now communicating with: " + primaryServer + ", port " + port1 + "\n");
						}
						
					}
					// Warning that there is no secondary available.
					else
					{
						System.out.println("No secondary server being used.\n");
					}
				}
			}
			
			// When 'quit' is typed in, close this client.
			while(!entry.equals("quit"));
			System.out.println("Cancelled by user request."); 
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

	public static void getSomething(String serverName, int port, String id, int s)
	{
		Socket sock; // Create a local socket.
		
		BufferedReader fromServer;
		PrintStream toServer; 
		String textFromServer;
		String index;

		try
		{
			// Create a socket connecting to the the server and the specified port number passed in.
			sock = new Socket(serverName, port); 

			// Create a bufferedReader object to buffer the input stream got from the socket.
			fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			// Create a printStream to store the output stream that will be sending to the server.
			toServer = new PrintStream(sock.getOutputStream());
			
			// Tell the server the unique ID of this client.
			toServer.println(id);
			toServer.flush();
			
			// Send the client's state.
			toServer.println(s);
			toServer.flush();

			// Read the current mode of the server, which is used to get the correct output index.
			mode = fromServer.readLine();
			
			// Check if a secondary server is available and is different from primary server.
			if(serverOn.equals("S") && differentServer)
			{
				// If so, get state2 and obtain the output index.
				state2 = Integer.parseInt(fromServer.readLine());
				index = getIndex(state2);
			}
			
			// If this client is connected to the primary server, get the updated state1 from the server, 
			// and get the corresponding output index.
			else
			{
				state1 = Integer.parseInt(fromServer.readLine());
				index = getIndex(state1);
			}

			// Get the joke/server sent from the server.
			textFromServer = fromServer.readLine(); 

			// If there is a secondary server, add <S2> in front of the regular outputs.
			if(serverOn.equals("S")) 
			{
				System.out.print("<S2> ");
			}
			
			// Print the correct index according to the state, and the user name.
			System.out.print(index + name + ": ");
			
			// Print the joke/proverb sent from the server.
			if (textFromServer != null)
				System.out.print(textFromServer);
			System.out.println();
			System.out.println();

			sock.close(); // Close the local socket.
		}
		catch(IOException x)
		{
			System.out.println("Socket error.");
			x.printStackTrace();
		}
	}
	
	// Using the updated client state to get the correct output index.
	public static String getIndex(int n)
	{
		int ret = 0;
		String s = null;
		
		// Get the corresponding four bits according to the server mode.
		int temp = (mode.equals("P")? (n ^ PMask):(n ^ JMask));
		
		// Using offset to get each correct bit.
		int offset = (mode.equals("P")? 0:4);
		
		for(int i = 0 + offset; i < 4 + offset; i++)
		{
			ret += (temp >> i) & 1; // Count the 1s in the corresponding four bits.
		}
		
		// Based on the server mode, get the correct output index.
		// P for proverbs, J for jokes.
		if(mode.equals("P"))
		{			
			switch(ret) 
			{
			case 3:
				s = "PA ";
				break;
			case 2:
				s = "PB ";
				break;
			case 1:
				s = "PC ";
				break;
			case 0:
				s = "PD ";
				break;
			}
			
			return s;
		}
		else
		{					
			switch(ret) 
			{
			case 3:
				s = "JA ";
				break;
			case 2:
				s = "JB ";
				break;
			case 1:
				s = "JC ";
				break;
			case 0:
				s = "JD ";
				break;
			}
			
			return s;
		}
		
	}

}