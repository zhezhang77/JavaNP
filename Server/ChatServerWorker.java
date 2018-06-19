import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;

// worker thread
public class ChatServerWorker extends Thread {
	private Socket worker;
	private String userName;
	
	public ObjectInputStream in;
	public ObjectOutputStream out;

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
			out = new ObjectOutputStream(worker.getOutputStream());
			in = new ObjectInputStream(worker.getInputStream());
			while (true) {
				Thread.sleep(500);
				
				// parse received msg
				ChatMsg msg;
				msg = (ChatMsg) in.readObject();
				// TalkLog.log("RECV: " + cmd.toString());//debug
				parse(msg);

				if (logout)
					break;

				// send queued message to client
				sendMsg();
			}
		} catch (IOException e) {
			e.printStackTrace();
			logout();
		} catch (InterruptedException e) {
			e.printStackTrace();
			logout();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			logout();
		}

		// quit worker
		try {
			in.close();
			out.close();
			worker.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
		ChatLog.log("WORKER END: " + Thread.currentThread().getId());
	}

	private ChatUser thisUser() {
		if (this.userName == null)
			return null;
		return Users.get(Users.indexOf(new ChatUser(userName)));
	}

	public void sendMsg() {
		if (userName == null)
			return;
		ChatMsg msg;
		while ((msg = thisUser().getOneMsg()) != null) {
			ChatMsg.sendMsg(msg, out);
		}
	}

	// check username/password
	public boolean login(ChatMsg msg) {
		if (this.userName != null)
			return false;

		int toUserId;
		if ((toUserId = Users.indexOf(new ChatUser(msg.msgSend))) != -1) {
			if (Users.get(toUserId).getPass().equals(msg.msgRecv)) {
				// Password Match
				return true;
			} else {
				msg.msgLoad = "Password not match";
				return false;
			}
		} else {
			// Create new user
			this.userName = msg.msgSend;
			Users.add(new ChatUser(this.userName));
			return true;
		}
	}

	// send user list
	public void getUsers() {
		String usrs = "";
		for (ChatUser usr : Users) {
			if (!usr.equals(thisUser())) {
				usrs += usr.getName() + ",";
			}
		}

		ChatMsg reply = new ChatMsg("USERS", "", this.userName, usrs);
		ChatMsg.sendMsg(reply, out);
	}

	// send msg to specific user
	public boolean send(String userName, String msg) {
		int receiverId;
		if ((receiverId = Users.indexOf(new ChatUser(userName))) != -1) {
			if (Users.get(receiverId).getName().equalsIgnoreCase(ChatServerMaster.strPublic))
				// publish msg to every user
				for (int i = receiverId + 1; i < Users.size(); ++i) {
					if (i != Users.indexOf(thisUser()))
						Users.get(i).sendPubMsg(this.userName, msg);
				}
			else if (userName.equals(this.userName))
				// send msg to public
				Users.get(receiverId).sendMsg(ChatServerMaster.strPublic, msg);
			else
				// send private msg
				Users.get(receiverId).sendMsg(this.userName, msg);
			return true;
		} else
			return false;
	}

	// send logout msg
	public void logout() {
		if (userName == null) {
			logout = true;
			return;
		}
		Users.remove(new ChatUser(userName));
		ChatLog.log("LOGOUT: " + this.userName);
		this.userName = null;
		logout = true;
		return;
	}

	// parse msg
	public void parse(ChatMsg cmd) {
		if (cmd == null)
			return;

		ChatMsg msg = (ChatMsg) cmd;

		if (msg.msgType.equalsIgnoreCase("LOGIN")) {
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
			String toUser = msg.msgRecv;
			if (send(toUser, msg.msgLoad))
				ChatLog.log(String.format("Success: %s send to %s", thisUser().getName(), toUser));
			else
				ChatLog.log(String.format("Failed: %s send to %s", thisUser().getName(), toUser));
		} else if (msg.msgType.equalsIgnoreCase("LOGOUT")) {
			logout();
		} else if (msg.msgType.equalsIgnoreCase("GETUSRS")) {
			getUsers();
		} else {
			ChatLog.log("Unknown Msg: " + msg.msgType);
		}
	}
}
