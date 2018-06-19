import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

// msg class which could be serialized to transfer through ObjectStream
public class ChatMsg implements Serializable {
	private static final long serialVersionUID = 7924951247387843168L;

	public String msgType;
	public String msgSend;
	public String msgRecv;
	public String msgLoad;

	public ChatMsg(String type, String send, String recv, String load) {
		this.msgType = type;
		this.msgSend = send;
		this.msgRecv = recv;
		this.msgLoad = load;
	}

	@Override
	public String toString() {
		return "{\'msgType\'=\'" + msgType + "\', \'msgSend\'=\'" + msgSend + 
				"\'," + "\'msgRecv\'=\'" + msgRecv + "\', \'msgLoad\'=\'" + msgLoad + "\'}";
	}

	public static void sendMsg(ChatMsg msg, ObjectOutputStream out) {
		try {
			out.writeObject(msg);
			out.flush();
			// ChatLog.log("SEND: " + msg.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
