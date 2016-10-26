package LSSpackage;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.*;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text field for entering messages and a
 * text area to see the whole dialog.
 *
 * The client follows the Chat Protocol which is as follows.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired screen name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with "MESSAGE " then all characters following
 * this string should be displayed in its message area.
 */


public class ChatClient {

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");         //create frame
    JButton button = new JButton("Whisper");      //make button
    JTextField textField = new JTextField(40);      //textField
    JTextArea messageArea = new JTextArea(8, 40);   //area that show chatting result

    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Return in the
     * listener sends the textfield contents to the server.  Note
     * however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED  
     * message from the server.
     */
    public ChatClient() {

        // Layout GUI
        textField.setEditable(false); //lock textField from users not to be editted
        messageArea.setEditable(false);  //lock messageArea from users not to be editted
        frame.getContentPane().add(textField, "North");     //put textField on North
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");  //put messageArea on center
        frame.getContentPane().add(button, "South");  //put whisper button on south
        frame.pack();   //pack all of them(textField,messageArea,button) in a package
        
        
        // Add Listeners
        textField.addActionListener(new ActionListener() 
        	{
            /**
             * Responds to pressing the enter key in the textfield by sending
             * the contents of the text field to the server.    Then clear
             * the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) 
            {
                out.println(textField.getText());  //print entered texts on textField
                textField.setText("");   //set textField to "".
            }
        });
        
        button.addActionListener(new ActionListener()
        		{
        	  public void actionPerformed(ActionEvent e)
        	  {
        		  String to=getWhisperName();// whisper to 'to'
        		  String whispMessage=getWhisperMessage();//whispMessage is the message
          		out.println(to+"/ "+whispMessage);//print whispering name and message at textField in format
          		textField.setText("");
        	  }
        	});
    }
   
    
    private String getWhisperName()//get which client to whisper to
    {
    	return JOptionPane.showInputDialog(
    			frame,
    			"Enter target name to whisper:",//Question
    			"Speak silently!!",				//box(frame)'s title on top
    			JOptionPane.QUESTION_MESSAGE); //put '?' on message
    }
    
    private String getWhisperMessage()
    {
    	return JOptionPane.showInputDialog(
    			frame,
    			"Enter your message to whisper to :",//Question
    			"Speak silently!!",				//box(frame)'s title on top
    			JOptionPane.QUESTION_MESSAGE); //put '?' on message
    }
    
    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() 
    {
        return JOptionPane.showInputDialog(
            frame, //frame
            "Enter IP Address of the Server:",   //Question
            "Welcome to the Chatter",         //box(frame)'s title on top
            JOptionPane.QUESTION_MESSAGE);      //put '?' on message
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() 
    {
        return JOptionPane.showInputDialog(
            frame, //frame
            "Choose a screen name:",   //Question
            "Screen name selection",   //box(frame)'s title on top
            JOptionPane.PLAIN_MESSAGE);      //no '?' on message
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException 
    {

        // Make connection and initialize streams
        String serverAddress = getServerAddress();   //get client's server address directly from keyboard
        Socket socket = new Socket(serverAddress, 9001);   //make socket(ipaddress=serverAddress, port number=9001);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream())); //get line by line to in by user
        out = new PrintWriter(socket.getOutputStream(), true);  //PrintWriter for temply store ready to send out to server
        String Name=null;
        
        // Process all messages from server, according to the protocol.
        while (true) 
        {
            String line = in.readLine(); //read a line message from server
            if (line.startsWith("SUBMITNAME")) //if client started,
            	{
            	Name=getName(); //get name and store at Name
                out.println(Name+" "+serverAddress);  //send Name, serverAddress to server
            } 
            else if (line.startsWith("NAMEACCEPTED")) //if accepted reply come from server
            {
               textField.setEditable(true);  //textField is now usable
               messageArea.append(line.substring(13) + "\n");// put message from server
            } 
            else if(line.startsWith("WHISPER from")) //when whisper format 'name/ message' has been replied by server
            {
            	String split[]=line.split(" ");// split line by blank "WHIPER from <from> to <to> <whipMessage>"
            	String from=split[2]; //from is the name who send whisper message
            	String to=split[4]; //to is the name who will get whisper message
            	String whispMessage=line.substring(19+from.length()+to.length()*2); //message to transmit
            	messageArea.append("<from "+from+">: "+whispMessage+"\n");
            }
            else if (line.startsWith("MESSAGE")) //if message sent
            {
                messageArea.append(line.substring(8) + "\n"); //put message with enter on messageArea
            } 
            else if(line.startsWith("LEFT"))// if client frame closed
            {
               messageArea.append(line.substring(5) + "\n"); // put "client has left" message
            }
        }
    }

    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();  //create ChatClient object
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //if frame is closed, client thread end
        client.frame.setVisible(true);  //frame is visible to user
        client.run(); //run client
    }
}