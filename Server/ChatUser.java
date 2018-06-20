//import java.io.Serializable;
import java.util.Vector;

// Store user info and messages from that user
public class ChatUser{
	private String userName;
	private String passWord;
	private String ip;
	private String status;

	// use Vector to store received msg 
	private Vector<ChatMsg> message;

	public ChatUser(String name) {
		message = new Vector<ChatMsg>();
		this.userName = name;
	}

	public String getName() { return userName; }
	public String getPass() { return passWord; }
	public String getIP() { return ip; }
	public String getStatus() { return status; }
	
	public void setPass(String strPass) { passWord = strPass; }
	public void setIP(String strIP) { ip = strIP; }
	public void setStatus(String strStatus) { status = strStatus; }
	
	// Add msg to the user's msg queue
	public void sendMsg(String sender, String msg) {
		ChatMsg reply = new ChatMsg("FROM", sender, userName, msg);
		message.add(reply);
	}

	// Add public msg to the user's msg queue
	public void sendPubMsg(String sender, String msg) {
		ChatMsg reply = new ChatMsg("FROMPUB", sender, userName, msg);
		message.add(reply);
	}

	// Retrieve one msg from msg queue
	public ChatMsg getOneMsg() {
		if (message.size() == 0)
			return null;
		return message.remove(0);
	}

	// Compare two users are equal or not
	@Override
	public boolean equals(Object user) {
		ChatUser u = (ChatUser) user;
		if (u == null || u.userName == null || this.userName == null)
			return false;
		return this.userName.equalsIgnoreCase(u.userName);
	}	
}
