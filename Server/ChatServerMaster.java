
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Vector;

public class ChatServerMaster {
	private static ServerSocket s;
	public static int port = 8080; // listen port
	public static String strPublic = "Public"; // name of public
	static Vector<ChatUser> Users; // Vector to store user info

	public void boot() {
		Users = new Vector<ChatUser>();
		ChatUser userPublic = new ChatUser(strPublic);
		userPublic.setStatus("online");
		userPublic.setPass("");
		userPublic.setIP("0.0.0.0");
		Users.add(userPublic); //Add user "Public"
		try {
			s = new ServerSocket(port);
		} catch (IOException e) {

			e.printStackTrace();
		}

		ChatLog.log("Server started");

		while (true) {
			ChatServerWorker worker = null;
			try {
				// Create worker thread when accept connection
				worker = new ChatServerWorker(s.accept());
				worker.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
