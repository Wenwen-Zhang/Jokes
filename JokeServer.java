
/*--------------------------------------------------------

1. Name / Date: 

	Wenwen Zhang  09/23/2017

2. Java version used, if not the official version for the class: 

	build 1.8.0_121-b13

3. Precise command-line compilation examples / instructions:

	> javac JokeServer.java

4. Precise examples / instructions to run this program:

	Run the following files in separate shell windows:

	> java JokeServer
	> java JokeClient
	> java JokeClientAdmin

	All acceptable commands are displayed on the various consoles.

	For this JokeServer, if no argument is passed, it is a primary server.
	If an argument "secondary" (> java JokeServer secondary) is passed, 
	then it is a secondary server.

5. List of files needed for running the program.

	a. JokeServer.java
	b. JokeClient.java
	c. JokeClientAdmin.java

6. Notes:

	a. There are 10 jokes and 10 proverbs stored in this server. 
	b. When a client gets connected, its UUID then is stored in this server along with 4 randomly 
	selected jokes and 4 random proverbs. 
	c. Depending on the mode, jokes and proverbs are sent to the client one at a time, when all 4 
	jokes/proverbs have been sent, randomly select new 4 jokes/proverbs to be stored with the unique UUID.
	d. The server communicates with clients with state which is a 8-bits integer. The server reads, interprets 
	and updates the state integer each time the clients send, and then send it back to the clients.
	e. Each time when a request is processed, the server shows which joke/proverb has been sent.
	f. When the adminClient gets connected, the server would show the connection and the operations (if any).

----------------------------------------------------------*/



import java.io.*; // Import the java input/output package  
import java.net.*; // Import the java networking package

// Import data structure ArrayList and HashMap to store data.
import java.util.ArrayList; 
import java.util.HashMap; 

// Import random package to enable the random selections of jokes/proverbs.
import java.util.Random;

public class JokeServer
{	
	// Store the id of each connected client and the four random selected jokes/proverbs.
	public static HashMap <String, ArrayList<Integer> > jokesClients; 
	public static HashMap <String, ArrayList<Integer> > proverbsClients;
	
	// 10 jokes and 10 proverbs stored in each hashMap.
	public static HashMap <Integer, String> jokes;
	public static HashMap <Integer, String> proverbs;
	
	// Server mode, J for Joke, P for Proverb, Joke mode is the default setting.
	public static String mode = "J";
	
	public static void main(String args[]) throws IOException
	{
		int q_len = 6; // Maximum requests the server can accept simultaneously. 
		int port = 4545; // Primary and default port.
		Socket sock; // Create a local socket. 

		// Initialize these two hash maps to be ready to store client's information.
		jokesClients = new HashMap<>();
		proverbsClients = new HashMap<>(); 
		
		// Get jokes and proverbs ready for use.
		getPoolReady();
		
		// Start a new thread to wait for the Admin Client's connection.
		AdminThread AT = new AdminThread(); 
	    Thread t = new Thread(AT);
	    t.start(); 
	    
		/* Create a server socket object which is bounded to the specified port number, 
		and has a capacity of 6 simultaneous requests. 
		*/	
		ServerSocket primarysock = new ServerSocket(port, q_len); 

		// Print out the message that the server is working and ports at which are listening for connections.
		System.out.println("Wenwen Zhang's Joke Server staring up, listening at Port 4545.");
		System.out.println("If connected as a secondary server, using Port 4546.\n");
		
		// Check the arguments. If "secondary" is presented, start a new thread to handle the clients. 
		if(args.length > 0)
		{
			if (args[0].toLowerCase().equals("secondary")) // Make the argument consistently lower case.
			{				
				// New thread for regular clients.
				SecondWorker second = new SecondWorker(); 
			    Thread t3 = new Thread(second);
			    t3.start();
			    
			    // New thread for admin clients.
			    AdminThread AT2 = new AdminThread(5051); 
			    Thread t2 = new Thread(AT2);
			    t2.start(); 		    		    
			}
		}

		while (true) // The server runs forever, waiting for connections.
		{
			/* Server socket listens for connection requests made from clients, 
			and if any, accepts it and assigns to the local socket.
			*/
			sock = primarysock.accept(); 
			
			// Create a new thread with this connected local socket and start running this thread.
			new Worker(sock).start(); 
		}
	}
	
	// Initialize the joke maps and proverb maps, put 10 entries into each one.
	public static void getPoolReady()
	{
		jokes = new HashMap<>();
		proverbs = new HashMap<>();
		
		jokes.put(0, "What's orange and sounds like a parrot? A carrot.");
		jokes.put(1, "What do you call it when Batman skips church? Christian Bale.");
		jokes.put(2, "Two fish are sitting in a tank. One looks over at the other and says: \"Hey, do you know how to drive this thing?\"");
		jokes.put(3, "I told my doctor that I broke my arm in two places. He told me to stop going to those places.");
		jokes.put(4, "I told my girlfriend she drew her eyebrows too high. She seemed surprised.");
		jokes.put(5, "Two cows are sitting in a field, and one says to the other, \"so, how about that mad cow disease? Scary stuff, right?\" To which to other replies, \"terrifying. But what do I care? I’m a helicopter.\"");
		jokes.put(6, "What did the 0 say to the 8? Nice belt!");
		jokes.put(7, "Why is six afraid of seven? Because seven ate nine.");
		jokes.put(8, "Two muffins are in an oven. One muffin says \"gosh, it’s hot in here\". The other muffin screams \"AAAH!! A talking muffin!\"");
		jokes.put(9, "What do you call bears with no ears? B");
		
		proverbs.put(0, "The early bird catches the worm.");
		proverbs.put(1, "Actions speak louder than words.");
		proverbs.put(2, "When in Rome, do as the Romans.");
		proverbs.put(3, "The squeaky wheel gets the grease.");
		proverbs.put(4, "When the going gets tough, the tough get going.");
		proverbs.put(5, "Fortune favors the bold.");
		proverbs.put(6, "Hope for the best, but prepare for the worst.");
		proverbs.put(7, "Birds of a feather flock together.");
		proverbs.put(8, "Better late than never.");
		proverbs.put(9, "There's no such thing as a free lunch.");
	}
}

/* Inherits Thread class and override the run() method so that the server can be multi-threaded.*/
class Worker extends Thread 
{
	Socket sock; // A local socket for connection.
	Worker (Socket s) {sock = s;} // Assign s to sock when this constructor is called.
	
	public static final int JMask = 0xF0; // Used to get the bits for client's joke state. 
	public static final int PMask = 0x0F; // Used to get the bits for client's proverb state. 

	public void run()
	{
		PrintStream out = null; 
		BufferedReader in = null; 

		try
		{
			// Get the client's input, in this case, including client's id and state.
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			// Used to write the outputs that will be displayed on the client's side.
			out = new PrintStream(sock.getOutputStream());

			try
			{
				// Get the client's unique id which will used as a key to associate the jokes/proverbs array.
				String ID = in.readLine(); 
				
				// Get the client's current state and convert it to an integer so that it can be interpreted and updated.
				int state = Integer.parseInt(in.readLine());

				// Call this method to process the client's state and give corrects things back.
				giveSomething(ID, state, out);
			}

			/* If anything goes wrong, keep the program running by catch the exception 
			and print out the error message.
			*/
			catch(IOException x) 
			{
				System.out.println("Server read error");
				x.printStackTrace();
			}

			sock.close(); // Close this local socket.
		}
		catch(IOException ioe)
		{
			System.out.println(ioe);
		}
	}

	public static void giveSomething(String id, int n, PrintStream out) throws IOException
	{
			
		int update; // The updated integer which is used to get the joke/provert that will be send to client.
		
		int stateOld = readState(n); // Call the readState() method to get the client's state, it would be 0 to 4.
		
		if (JokeServer.mode.equals("P")) // Check if the server is on Proverb mode
		{
			// if the state is 1, 2, or 3, it means that the client is not first time connected, and has an associated 
			// array list of 4 proverbs which has had several items being sent before.
			if(stateOld > 0 && stateOld < 4)
			{
				update = stateOld; // Get the current proverb state of the client
			}
			
			// if the state is not 1, 2, or 3, it means either the client is first time connected, or the associated 
			// 4 proverbs had all been sent to the client, a new cycle need to be started.
			else
			{
				// Call the randList() method to generate an array with 4 random selected proverb's indexes in range [0, 9].
				JokeServer.proverbsClients.put(id, randList()); 
				update = 0; // Start a new cycle, grab the first item in the id-associated array.
			}
			
			// Print out on the console which proverb will be sent to the client in this request.			
			System.out.println("Sending Proverb #" + JokeServer.proverbsClients.get(id).get(update) + " to Clent " +id + ".\n");
			
		}
		else // The server is on Joke mode
		{
			// if the state is 1, 2, or 3, it means that the client is not first time connected, and has an associated 
			// array list of 4 jokes which has had several items being sent before.
			if(stateOld > 0 && stateOld < 4)
			{
				update = stateOld; // Get the current joke state of the client
			}
			
			// if the state is not 1, 2, or 3, it means either the client is first time connected, or the associated 
			// 4 proverbs had all been sent to the client, a new cycle need to be started.
			else
			{
				// Call the randList() method to generate an array with 4 random selected proverb's indexes in range [0, 9].
				JokeServer.jokesClients.put(id, randList());
				update = 0; // Start a new cycle, grab the first item in the id-associated array.
			}
			
			// Print out on the console which proverb will be sent to the client in this request.
			System.out.println("Sending Joke #" + JokeServer.jokesClients.get(id).get(update) + " to Clent " +id + ".\n");
			
		}
		
		// Write the current server mode, the updated state, 
		// and the joke/proverb which is in correct order to the client in three lines.
		out.println(JokeServer.mode); 
		out.println(updateState(n, stateOld)); 
		out.println(getOutputs(id, update));
	}
	
	// This method is to generate an array containing 4 integers in range [0, 9]
	public static ArrayList<Integer> randList()
	{
		Random rand = new Random();
		ArrayList<Integer> arr = new ArrayList<Integer> ();
		int i = 0;
		while(i < 4)
		{
			int n = rand.nextInt(10);
			if (! arr.contains(n)) // Add to the array only when the random integer is not in the array, 
			{
				arr.add(n);
				i++;
			}
		}	
		
		return arr;
	}
	
	// This method is to get the corresponding Joke/Proverb state of the client.
	public static Integer readState(int n)
	{
		int ret = 0;
		
		// Mask the state to get the corresponding four bits. 
		int temp = (JokeServer.mode.equals("P")? (n ^ PMask):(n ^ JMask)); 
		
		// Offset is used to pointed to the correct bits in different mode.
		
		int offset = (JokeServer.mode.equals("P")? 0:4);
		
		// Count how many bits are 1, the result is exactly how many jokes or proverbs had been sent before.
		for(int i = 0 + offset; i < 4 + offset; i++)
		{
			ret += (temp >> i) & 1;
		}
			
		return ret;
				
	}
	
	// This method is to update the client's state.
	public static Integer updateState(int n, int s)
	{
		int ret = 0;
		
		// Offset is used to pointed to the correct bits in different mode
		int offset = (JokeServer.mode.equals("P")? 0:4);
		
		// If the state is 1, 2, or 3, turn on the next bit.
		if (s > 0 && s < 4) 
		{
			ret = n | (1 << (s + offset));
		}
		
		// If the state is 0 or 4, switch all 4 bits to zero, and turn on the first bit to start a new cycle.
		else
		{
			n = (JokeServer.mode.equals("P")? (n & 0xF0):(n & 0x0F));
			ret = n | (1 << offset);
		}
		return ret;	
	
	}
	
	// This method is to grab the corresponding joke/proverb based on the client state.
	public static String getOutputs(String id, int n)
	{
		if(JokeServer.mode.equals("P"))
		{
			return JokeServer.proverbs.get(JokeServer.proverbsClients.get(id).get(n));
		}
		else
		{
			return JokeServer.jokes.get(JokeServer.jokesClients.get(id).get(n));
		}		
	}	
}


// A thread class to enable the connections and executions of admin clients.
class AdminThread implements Runnable 
{	
	  public static int port = 5050; // The default port for admin client
	  
	  AdminThread(){} // constructor.
	    
	  AdminThread(int newPort) // constructor with specified port number as an argument. 
	  {
		  port = newPort;
	  }

	  public void run() 
	  { 	        
	    int q_len = 6; 
	    Socket sock;

	    try
	    {
	      ServerSocket adminsock = new ServerSocket(port, q_len);
	      while (true) 
	      {
	    	  // wait for the connections requested from admin clients.
	    	  sock = adminsock.accept();
	    	  new AdminWorker (sock).start(); 
	      }
	    }
	    catch (IOException ioe) {System.out.println(ioe);}
	  }
}

// An admin worker thread to run the admin clients.
class AdminWorker extends Thread 
{
	Socket sock; // A local socket object sock.
	AdminWorker (Socket s) {sock = s;} // Assign s to sock when this constructor is called.

	public void run()
	{
		PrintStream out = null; 
		BufferedReader in = null; 
		
		// Print out that the admin client has been connected.
		System.out.println("AdminClient connected at port " + AdminThread.port + ".");

		try
		{
			// Get the command send by the admin client.
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			// Print out the message to show on the console of admin client.
			out = new PrintStream(sock.getOutputStream());

			try
			{				
				String oldMode = JokeServer.mode; // Get the current mode of the server.
				JokeServer.mode = in.readLine(); // Get the command sent by the admin.
				String toDisplay = null; // Initialize a String which will be displayed on both the server and the admin.
				
				// If the command is 'shutdown', print a message and notify the admin, then close the server. 
				if(JokeServer.mode.equals("SHUTDOWN"))
				{
					System.out.println("Shut down by the Admin Client. Closing...");
					System.out.flush();
					out.println("Server has been shut down.");
					System.exit(0);
				}
				
				// If the command is 's', which means the admin now is connecting to both ports of this server.
				// Change the current port to the other port, and print out summary message both on console and to the admin.
				else if (JokeServer.mode.equals("S"))
				{
					AdminThread.port = ((AdminThread.port == 5051)? 5050:5051); // Switch the port.
					System.out.println("Admin Client now is switched to using port " + AdminThread.port + ".\n");
					System.out.flush();
					out.println("Server port switched to " + AdminThread.port + ".");			
				}
				
				// If the command is P or J, switch between proverb mode and joke mode.
				else
				{	
					toDisplay = (JokeServer.mode.equals("P")? "Proverb Mode.":"Joke Mode.");
				}
				
				
				// If the server is already on the demanded mode, notify the admin that nothing needs to be changed.
				if(JokeServer.mode.equals(oldMode))
				{
					out.println("Currently on this mode, no need to change.");
				}
				
				// Print summary message indicating the current mode the server is on.
				else
				{
					System.out.println("Server Mode has been changed, now it is on " + toDisplay + "\n");
					System.out.flush();
					out.println("Server Mode has been changed, now it is on " + toDisplay + ".");
				}
				
			}

			/* If anything goes wrong, keep the program running by catch the exception 
			and print out the error message.
			*/
			catch(IOException x) 
			{
				System.out.println("Server read error");
				x.printStackTrace();
			}

			sock.close(); // Close this socket.
		}
		catch(IOException ioe)
		{
			System.out.println(ioe);
		}
	}
}

// A second worker class to run the secondary server which is listening at port 4546.
class SecondWorker implements Runnable 
{	  
	  public void run()
	  {
		  
	    int q_len = 6;
	    int port = 4546;  
	    Socket sock;

	    try
	    {
	      ServerSocket secondsock = new ServerSocket(port, q_len);
	      
	      while (true) 
	      {
	    	  sock = secondsock.accept();
	    	  new Worker (sock).start(); 
	      }
	    }
	    catch (IOException ioe) {System.out.println(ioe);}
	  }
}
