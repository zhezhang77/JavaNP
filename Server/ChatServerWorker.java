import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;

// Server worker thread
public class ChatServerWorker implements Runnable {
	// Socket to connect server and client
	private Socket worker;
	private String userName;
	
	// Use object stream to transfer serializable object 
	public ObjectInputStream in;
	public ObjectOutputStream out;

	// Vector is thread-safe container
	// Every threads share the same vector of user info
	private static Vector<ChatUser> Users;
	private boolean logout;

	public ChatServerWorker(Socket socket) {
		this.worker = socket;
	}

	@Override
	public void run() {
		ChatLog.log("WORKER START: " + Thread.currentThread().getId());
		Users = ChatServerMaster.Users;
		
		try {
			// Server would create output stream first to avoid deadlock with client 
			out = new ObjectOutputStream(worker.getOutputStream());
			in = new ObjectInputStream(worker.getInputStream());
			while (true) {
				Thread.sleep(500);
				

				ChatMsg msg;
				// Read one msg from input stream
				msg = (ChatMsg) in.readObject();
				// Parse received msg
				parse(msg);

				// Quit the worker loop when something wrong or use logout
				if (logout)
					break;

				// Send queued message to client
				sendMsg();
			}
		} catch (IOException e) {
			// e.printStackTrace();
			logout();
		} catch (InterruptedException e) {
			e.printStackTrace();
			logout();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			logout();
		}

		// quit worker, release resources
		try {
			in.close();
			out.close();
			worker.close();
		} catch (IOException e) {
			// e.printStackTrace();
		}
		
		ChatLog.log("WORKER END: " + Thread.currentThread().getId());
	}

	// Get current user object
	private ChatUser thisUser() {
		if (this.userName == null)
			return null;
		return Users.get(Users.indexOf(new ChatUser(userName)));
	}

	// Send out msgs in the queue
	public void sendMsg() {
		if (userName == null)
			return;
		ChatMsg msg;
		while ((msg = thisUser().getOneMsg()) != null) {
			ChatMsg.sendMsg(msg, out);
		}
	}

	// User login, check username/password
	public boolean login(ChatMsg msg) {
		if (this.userName != null) // Prevent redundant login
			return false;

		int toUserId;
		if ((toUserId = Users.indexOf(new ChatUser(msg.msgSend))) != -1) {
			// Old user login, check the password
			if (Users.get(toUserId).getPass().equals(msg.msgRecv)) {
				// Password Match
				this.userName = msg.msgSend;
				Users.get(toUserId).setStatus("online");
				Users.get(toUserId).setIP(this.worker.getInetAddress().getHostAddress());
				return true;
			} else {
				// Password wrong
				msg.msgLoad = "Password not match";
				return false;
			}
		} else {
			// New user login, create new record
			this.userName = msg.msgSend;
			ChatUser newUser = new ChatUser(this.userName);
			newUser.setPass(msg.msgRecv);
			newUser.setStatus("online");
			newUser.setIP(this.worker.getInetAddress().getHostAddress());
			Users.add(newUser);
			return true;
		}
	}

	// Send user list
	public void getUsers() {
		// Each user info is a string like "username,status,ip"
		// Whole user list like "user_info_1@user_info_2@user_info_3@..."
		String strUsers = "";
		for(int i=0;i<Users.size();i++) {
			ChatUser usr = Users.get(i);
			if (i == 0) {
				strUsers += usr.getName() + "," + usr.getStatus()+ "," + usr.getIP();
			} else {
				strUsers += "@" + usr.getName() + "," + usr.getStatus()+ "," + usr.getIP();
			}
		}
		
		// Send the list back with msg type USERS
		ChatMsg reply = new ChatMsg("USERS", "", this.userName, strUsers);
		ChatMsg.sendMsg(reply, out);
	}

	// Send msg to specific user
	public boolean send(String userName, String msg) {
		int receiverId;
		// Find the ID of receiver by name
		if ((receiverId = Users.indexOf(new ChatUser(userName))) != -1) {
			if (Users.get(receiverId).getName().equalsIgnoreCase(ChatServerMaster.strPublic)) {
				// if receiver is "Publish", msg is user->Public
				for (int i = receiverId + 1; i < Users.size(); ++i) {
					if (i != Users.indexOf(thisUser()))
						Users.get(i).sendPubMsg(this.userName, msg);
				}
			} else if (userName.equals(this.userName)) {
				// if receive is user itself, msg is Public->user
				Users.get(receiverId).sendMsg(ChatServerMaster.strPublic, msg);
			} else {
				// send private msg
				Users.get(receiverId).sendMsg(this.userName, msg);
			}
			return true;
		} else {
			return false;
		}
	}

	// send logout command
	public void logout() {
		if (userName == null) {
			logout = true;
			return;
		}
		
		// mark user as offline when logout
		Users.get(Users.indexOf(new ChatUser(userName))).setStatus("offline");
		ChatLog.log("LOGOUT: " + this.userName);
		this.userName = null;
		logout = true;
		return;
	}

	// parse msg send from user
	public void parse(ChatMsg cmd) {
		if (cmd == null)
			return;

		ChatMsg msg = (ChatMsg) cmd;
		
		if (msg.msgType.equalsIgnoreCase("LOGIN")) {
			// User login
			if (login(msg)) {

				ChatMsg reply = new ChatMsg("OK", "", "", "");
				ChatMsg.sendMsg(reply, out);
				ChatLog.log("LOGIN OK: " + msg.msgSend);
			} else {
				ChatMsg reply = new ChatMsg("FAILED", "", "", msg.msgLoad);
				ChatMsg.sendMsg(reply, out);
				ChatLog.log("LOGIN FAILD: " + msg.msgSend);
			}
		} else if (msg.msgType.equalsIgnoreCase("SEND")) {
			// User send msg
			String toUser = msg.msgRecv;
			if (send(toUser, msg.msgLoad))
				ChatLog.log(String.format("Success: %s send to %s", userName, toUser));
			else
				ChatLog.log(String.format("Failed: %s send to %s", userName, toUser));
		} else if (msg.msgType.equalsIgnoreCase("LOGOUT")) {
			// User logout
			logout();
		} else if (msg.msgType.equalsIgnoreCase("GETUSRS")) {
			// User ask for the list of all users
			getUsers();
		} else {
			ChatLog.log("Unknown Msg: " + msg.msgType);
		}
	}
}
