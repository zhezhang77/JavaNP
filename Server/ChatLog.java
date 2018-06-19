import java.text.SimpleDateFormat;
import java.util.Date;

// Helper debug output function
public class ChatLog {
	public static void log(String log) {
		System.out.printf("[%s] %s\n", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()), log);
	}
}
