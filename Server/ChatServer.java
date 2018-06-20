// Entry point of chat server
public class ChatServer {
	public static void main(String[] args) {
		// Create the instance of server master thread  
		ChatServerMaster server = new ChatServerMaster();
		
		// Start listening inbounds connections
		server.startup();
	}
}
