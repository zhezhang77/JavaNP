import java.io.IOException;
import java.net.ServerSocket;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Master thread of server, handle creating worker threads
public class ChatServerMaster {
	private static ServerSocket s;	// Listen socket
	public static int port = 8080; // Listen port
	public static String strPublic = "Public"; // Name of public
	static Vector<ChatUser> Users; // Vector to store user info

	public void startup() {
		Users = new Vector<ChatUser>();
		// Create a virtual user "Public" to deal with broadcast msg
		ChatUser userPublic = new ChatUser(strPublic);
		userPublic.setStatus("online");
		userPublic.setPass("");
		userPublic.setIP("0.0.0.0");
		Users.add(userPublic); //Add user "Public"
		
		// Create listen socket
		try {
			s = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ChatLog.log("Server started");

		// Create a scalable thread pool to handle connections
		// Let the system to decide the size of thread pool
		ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
		 
		while (true) {
			try {
				// When accept new connections, thread pool will arrange a new thread 
				cachedThreadPool.execute(new ChatServerWorker(s.accept()));
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
}
