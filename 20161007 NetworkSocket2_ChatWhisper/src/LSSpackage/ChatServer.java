package LSSpackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.HashMap;

/**
 * A multithreaded chat room server.  When a client connects the
 * server requests a screen name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name.  The
 * broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple
 * chat server, there are a few features that have been left out.
 * Two are very useful and belong in production code:
 *
 *     1. The protocol should be enhanced so that the client can
 *        send clean disconnect messages to the server.
 *
 *     2. The server should do some logging.
 */
public class ChatServer {

    /**
     * The port that the server listens on.
     */
    private static final int PORT = 9001;

    /*
     * HashMap<String,String> names store the user Information name(String),IPaddress(String) in related set
     * HashMap<String,PrintWriter> nameMap store user's name(String) and writer(PrintWriter) for printing
     */
    private static HashMap<String,String> names = new HashMap<String,String>();
    private static HashMap<String,PrintWriter> nameMap=new HashMap<String,PrintWriter>();
    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages.
     */
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
    
    /**
     * The application main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    private static class Handler extends Thread {
        private String name;
        private String IPaddress;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {

                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));//read from client
                out = new PrintWriter(socket.getOutputStream(), true); //temporarily store PrintWriter to write to client by writer

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME");//give "SUBMITNAME" message to client
                    String clientMessage = in.readLine();//read "name IPaddress" message to clientMessage
                    String split[]=clientMessage.split(" ");
                    name=split[0];//store name of client which indicate who set name to client itself.
                    IPaddress=split[1];//store IPaddress of client
                    if (clientMessage == null) //while there are no clientMessage, busy waiting.
                    {
                        return;
                    }
                    synchronized (names) //synchronization the names hashMap
                    {
                        if (!names.containsKey(name)) 
                        {
                            names.put(name,IPaddress);//if there are no name in HashSet, add it to set
                            break;// after adding, break from while loop.
                        }
                    }
                }
                
                synchronized(nameMap)//synchronize hashMap nameMap
                {
                	if(!nameMap.containsKey(name))
                	{
                		nameMap.put(name,out);//put name and it's writer to hashMap nameMap
                	}
                }

                
                for (PrintWriter writer : writers) //broadcast hashMap names's node is accepted to all threads
                {
                    writer.println("NAMEACCEPTED " + name +"("+IPaddress+") has entering"); 
                }
                writers.add(out);  //add new writer to writers hashSet

                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) 
                {
                    String input = in.readLine();//input is key(name)
                    if (input == null) //busy waiting
                    {
                        return;
                    }
                    String split[]=input.split("/");//if input inseted, split it by '/' to distinct whisper and message
                    String inputName=split[0];//target name to whisper to.
                    
                    if(names.containsKey(inputName))//whisper from name to inputName
                    {
                    	for(String userName : names.keySet())
                    	{
                    		String userAddress=names.get(userName);
                    		System.out.println(userName);
                    		for(String mapName : nameMap.keySet())
                    		{
                    			if(userName.equals(inputName)&&userName.equals(mapName))//search for target names hashMap
                    			{
                    				PrintWriter mapWriter=nameMap.get(mapName);//mapWriter is target's PrintWriter
                    				mapWriter.println("WHISPER from "+name+" to "+inputName+" "+input);// send whisper message to target thread
                    			}
                    		}
                    	}
                    	
                    }
                    else//broadcast message to all users
                    {
                    	 for (PrintWriter writer : writers) {
                         	writer.println("MESSAGE " + name + ": " + input);//broadcasting
                         }
                    }
                    
                }//while loop end
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
               for (PrintWriter writer : writers) //if client thread suddenly ended(closed)
               {
                    writer.println("LEFT " + name + "has left");//broadcast "LEFT" message to all users
                }
               if (name != null) {
                    names.remove(name);//erase name from names
                }
                if (out != null) {
                    writers.remove(out);//erase out from writers
                }
                try {
                    socket.close();//close socket.
                } catch (IOException e) {
                }
            }
        }
    }
}