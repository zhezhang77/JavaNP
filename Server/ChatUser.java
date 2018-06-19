import java.util.Vector;

// store user info
public class ChatUser {
	private String userName;
	private String passWord;

	// use Vector to store received msg 
	private Vector<ChatMsg> message;

	public ChatUser(String name) {
		message = new Vector<ChatMsg>();
		this.userName = name;
	}

	public String getName() {
		return userName;
	}

	public String getPass() {
		return passWord;
	}

	public void sendMsg(String sender, String msg) {
		ChatMsg reply = new ChatMsg("FROM", sender, userName, msg);
		message.add(reply);
	}

	public void sendPubMsg(String sender, String msg) {
		ChatMsg reply = new ChatMsg("FROMPUB", sender, userName, msg);
		message.add(reply);
	}

	public ChatMsg getOneMsg() {
		if (message.size() == 0)
			return null;
		return message.remove(0);
	}

	@Override
	public boolean equals(Object user) {
		ChatUser u = (ChatUser) user;
		if (u == null || u.userName == null || this.userName == null)
			return false;
		return this.userName.equalsIgnoreCase(u.userName);
	}
}
