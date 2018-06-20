
import javafx.application.Application;
import javafx.application.Platform;
//import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
//import java.util.Vector;
//import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
//import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
//import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
//import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
//import sun.misc.BASE64Decoder;

public class ChatClient extends Application {
	private static Socket s;
	private static ObjectInputStream in;
	private static ObjectOutputStream out;
	private static Stage stage;
	private ObservableList<String> userList;
	private Map<String, String> userMsg;
	private Map<String, Boolean> userMsgNotify;
	private TextArea listMsg, sendMsg;
	private ListView<String> userListView;
	private boolean msgReachEnd;
	String ip = "127.0.0.1", userName = "", passWord = "";

	// Display login window
	private void loginScene() throws IOException {
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Text scenetitle = new Text("Login");
		scenetitle.setFont(Font.font("Consolas", FontWeight.NORMAL, 20));

		Label ipLabel = new Label("IP Address:");
		grid.add(ipLabel, 0, 1);

		TextField ipField = new TextField();
		ipField.setText(ip);
		grid.add(ipField, 1, 1);

		Label userNameLabel = new Label("Username:");
		grid.add(userNameLabel, 0, 2);

		TextField userNameTextField = new TextField();
		userNameTextField.setText(userName);
		grid.add(userNameTextField, 1, 2);

		Label passWordLabel = new Label("Password:");
		grid.add(passWordLabel, 0, 3);

		TextField passWordTextField = new TextField();
		passWordTextField.setText(passWord);
		grid.add(passWordTextField, 1, 3);

		Button btn = new Button("Login");
		HBox hbBtn = new HBox(0);
		hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
		hbBtn.getChildren().add(btn);
		grid.add(hbBtn, 1, 4);

		Scene scene = new Scene(grid, 320, 240);

		stage.setScene(scene);
		stage.show();

		// Send login msg when click login button
		btn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				Alert alert = new Alert(AlertType.ERROR);

				// User name should not be "Public"
				if (userNameTextField.getText().equalsIgnoreCase(ChatServerMaster.strPublic)) {
					alert.setContentText("Please check the name");
					alert.showAndWait();
					return;
				}

				ip = ipField.getText();
				userName = userNameTextField.getText();
				passWord = passWordTextField.getText();
				
				// Send username and password to the server
				if (checkLogin()) {
					// Login success, goto main window
					grid.getChildren().clear();
					chatScene();
				} else {
					// Login fail, pop warning message
					alert.setContentText("Login failed!");
					alert.showAndWait();
				}

			}
		});
	}

	// Update client list by the data from server
	public void getUserList(String strUsers) {
		String[] allUser = strUsers.split("@");
		
		String[][] Users = new String[allUser.length][3];  
		for (int i = 0; i < allUser.length; i++) {
			String[] currUser = allUser[i].split(",");
			Users[i][0] = currUser[0];
			Users[i][1] = currUser[1];
			Users[i][2] = currUser[2];
		}
		
		// Mark offline user
		for (int i = 0; i < this.userList.size(); ++i) {
			String currUserName = getUserName(this.userList.get(i));

			if (currUserName != null) {
				// Skip "Public"
				if (currUserName.equals(ChatServerMaster.strPublic))
					continue;

				// Check the status of user
				// Add IP after name if online
				// Add "offline" after name if offline
				for(int j=0;j<Users.length;j++) {
					if (currUserName.equals(Users[j][0])) {
						if (Users[j][1].equals("online")) {
							this.userList.set(i, Users[j][0] + " @ " + Users[j][2]);
						}else {
							this.userList.set(i, Users[j][0] + " @ offline");
						}
					}
				}
			}
		}

		// Add new user to the end of the list
		for(int i=0;i<Users.length;i++) {
			if (Users[i][0] != null && 
					!Users[i][0].equals(ChatServerMaster.strPublic) &&
					!Users[i][0].equals(userName)) {
				boolean bFind = false;
				for (int j = 0; j < this.userList.size(); ++j) {
					String strUser = getUserName(this.userList.get(j));
					if (strUser.equals(Users[i][0])) {
						bFind = true;
						break;
					}
				}
				if(bFind == false) {
					if (Users[i][1].equals("online")) {
						this.userList.add(Users[i][0] + " @ " + Users[i][2]);
					}else {
						this.userList.add(Users[i][0] + " @ offline");
					}
				}
			}
		}

		// Mark user with "*" when received new msg 
		for (int i = 0; i < this.userList.size(); ++i) {
			String strOrg = this.userList.get(i);
			Boolean bDispUnreadMsg = false;
			if (strOrg.substring(0,2).equals("* ")) {
				strOrg = strOrg.substring(2);
				bDispUnreadMsg = true;
			}
			
			String user = getUserName(strOrg);
			if (user != null) {
				if( userMsgNotify.get(user) != null && userMsgNotify.get(user)) {
					if (bDispUnreadMsg == false)
						this.userList.set(i, "* " + strOrg);
				} else {
					if (bDispUnreadMsg == true)
						this.userList.set(i, strOrg);
				}
			}
		}
	}

	// Push sent/received msg into screen
	public void storeMsg(String sender, String msg, boolean all) {

		String strMsg = String.format("[%s | %s] %s\n", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
				sender, msg);

		if (all)
			sender = ChatServerMaster.strPublic;

		// Add received msg into msg queue
		if (userMsg.get(sender) == null)
			userMsg.put(sender, strMsg);
		else
			userMsg.put(sender, userMsg.get(sender) + strMsg);
		
		// Add sender to the notify list
		userMsgNotify.put(sender, true);
	}

	// Parse received msg
	public void parse(ChatMsg cmd) {
		if (cmd.msgType.equals("USERS")) {
			// Server return user list
			getUserList(cmd.msgLoad); 
		} else if (cmd.msgType.equals("FROMPUB")) {
			// Server send a publish msg
			String sender = cmd.msgSend;
			storeMsg(sender, cmd.msgLoad, true);
		} else if (cmd.msgType.equals("FROM")) {
			// Server send a private message
			String sender = cmd.msgSend;
			storeMsg(sender, cmd.msgLoad, false);
		}
	}

	// Remove prefix and suffix of username 
	private String getUserName(String user) {
		if (user == null)
			return null;

		if (user.substring(0, 2).equals("* "))
			user = user.substring(2);
		
		String[] subString = user.split(" @ ");
		return subString[0];
	}

	// Check if the user is offline from the displayed name
	private boolean isOffline(String user) {
		if (user == null)
			return false;

		String[] subString = user.split(" @ ");
		if (subString[0].equals("Public")) {
			return false;
		}else if (subString.length > 1 && subString[1] != null && subString[1].subSequence(0, 7).equals("offline")) {
			return true;
		} else {
			return false;
		}
	}

	// Send msg to server
	private void sendMsg() {
		String currUser = getUserName(userListView.getSelectionModel().getSelectedItem());
		if (sendMsg.getText() != null && !Pattern.matches("\\n*", sendMsg.getText())) {
			// Build SEND msg
			ChatMsg msg = new ChatMsg("SEND", userName, currUser, sendMsg.getText());
			ChatMsg.sendMsg(msg, out);

			String Msg = String.format("[%s | %s] %s\n", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
					userName, sendMsg.getText());
			
			if (userMsg.get(currUser) != null)
				userMsg.put(currUser, userMsg.get(currUser) + Msg);
			else
				userMsg.put(currUser, Msg);
			
			// Push msg into msg queue
			listMsg.appendText(Msg);
			sendMsg.setText("");
		} else {
			sendMsg.setText("");
		}
	}

	// Main window
	private void chatScene() {
		// Store msg list for each user 
		userMsg = new HashMap<String, String>();
		// Store new message status for each user
		userMsgNotify = new HashMap<String, Boolean>();
		// Monitor public msg
		userList = FXCollections.observableArrayList(ChatServerMaster.strPublic);
		
		stage.setTitle("[User: " + userName + "]");

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		// User list
		userListView = new ListView<>(userList);
		userListView.setItems(userList);
		userListView.getSelectionModel().select(0); // select "Public" as default
		
		VBox userListBox = new VBox();
		VBox.setVgrow(userListView, Priority.ALWAYS);
		userListBox.getChildren().addAll(userListView);

		// Msg box and input box
		VBox sendBox = new VBox();
		listMsg = new TextArea();
		sendMsg = new TextArea();
		listMsg.setEditable(false);
		listMsg.setPrefRowCount(20);
		listMsg.setWrapText(true);
		sendMsg.setPrefRowCount(5);
		sendMsg.setWrapText(true);

		sendBox.getChildren().add(listMsg);
		sendBox.getChildren().add(sendMsg);

		// Send button
		Button btn = new Button("Send");
		HBox hbBtn = new HBox(0);
		hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
		hbBtn.getChildren().add(btn);
		sendBox.getChildren().add(hbBtn);

		// Add all element in to main scene
		grid.add(userListBox, 0, 0);
		grid.add(sendBox, 1, 0);
		Scene scene = new Scene(grid);
		stage.setScene(scene);

		// When selected new user, switch the content of msg area
		userListView.getSelectionModel().selectedItemProperty()
				.addListener((ObservableValue<? extends String> ov, String old_val, String new_val) -> {
					String old_user = getUserName(old_val);
					String new_user = getUserName(new_val);

					if (!old_user.equals(new_user)) {
						if (userMsg.get(new_user) != null) {
							listMsg.setText(userMsg.get(new_user));
							userMsgNotify.put(new_user, false);
							msgReachEnd = true;
						} else {
							listMsg.setText("");
						}
					}
				});
		
		// Handle send button
		btn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				sendMsg();
			}
		});

		// Handle enter key
		sendMsg.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent keyEvent) {
				if (keyEvent.getCode() == KeyCode.ENTER) {
					sendMsg();
					keyEvent.consume();
				}
			}
		});

		// Create new thread to refresh user list per second
		Task<Void> task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				while (out != null && in != null) {
					try {
						Thread.sleep(1000);
						
						// Ask server for user list
						ChatMsg msg = new ChatMsg("GETUSRS", "", "", "");
						ChatMsg.sendMsg(msg, out);

						ChatMsg cmd = (ChatMsg) in.readObject();
						if (cmd == null)
							break;

						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								// parse server's response
								parse(cmd);
								
								if (msgReachEnd) {
									listMsg.setScrollTop(Double.MAX_VALUE);
									msgReachEnd = false;
								}

								// Disable input area when receiver is offline 
								int curUsrId = userListView.getSelectionModel().getSelectedIndex();
								String curUsr = getUserName(userListView.getSelectionModel().getSelectedItem());

								if (isOffline(userListView.getSelectionModel().getSelectedItem())) {
									sendMsg.setDisable(true);
									btn.setDisable(true);
								} else {
									sendMsg.setDisable(false);
									btn.setDisable(false);
								}

								// Remove user from unread msg list
								if (userMsgNotify.get(curUsr) != null && userMsgNotify.get(curUsr)) {
									userMsgNotify.put(curUsr, false);
									userList.set(curUsrId, curUsr);

									listMsg.appendText(userMsg.get(curUsr).substring(listMsg.getText().length(),
											userMsg.get(curUsr).length()));
								}
							}
						});

					} catch (InterruptedException e) {

						e.printStackTrace();
					} catch (IOException e) {

						e.printStackTrace();
					}
				}
				return null;
			}
		};
		Thread th = new Thread(task);
		th.setDaemon(true); // Set thread to daemon mode
		th.start();
	}

	// Process login
	private boolean checkLogin() {
		Alert alert = new Alert(AlertType.ERROR);

		try {
			// Connect server and send the username and password
			s = new Socket(ip, ChatServerMaster.port);
			out = new ObjectOutputStream(s.getOutputStream());
			ChatMsg msg = new ChatMsg("LOGIN", userName, passWord, "");
			ChatMsg.sendMsg(msg, out);

			in = new ObjectInputStream(s.getInputStream());
			ChatMsg reply = (ChatMsg) in.readObject();
			
			// Check response
			if (reply.msgType.equals("OK"))
				return true;
			else
				return false;
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
			alert.setContentText(e.toString());
			alert.showAndWait();
		} catch (IOException e) {
			e.printStackTrace();
			alert.setContentText(e.toString());
			alert.showAndWait();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	// Entry point of chat client
	@Override
	public void start(Stage primaryStage) throws IOException {
		stage = primaryStage;
		stage.setTitle("Java Network Programming");
		stage.setResizable(false);
		loginScene();
	}

	// Quit thread
	@Override
	public void stop() {
		try {
			if (in != null && out != null && s != null) {
				ChatMsg msg = new ChatMsg("LOGOUT", userName, "", "");
				ChatMsg.sendMsg(msg, out);

				in.close();
				out.close();

				in = null;
				out = null;

				s.close();
			}
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
}
