
import javafx.application.Application;
import javafx.application.Platform;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
	String ip = "", userName = "", passWord = "";

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

	// Update client listbox
	public void getUserList(String usrsList) {
		String[] allUser = usrsList.split(",");
		ObservableList<String> oList = FXCollections.observableArrayList(allUser);

		// Mark offline user
		for (int i = 0; i < this.userList.size(); ++i) {
			// Skip Public
			if (this.userList.get(i).equals(ChatServerMaster.strPublic))
				continue;
			
			String currUserName = getUserName(this.userList.get(i));
			
			if (currUserName != null) {
				// Check if current user is in the active list
				if (!oList.contains(currUserName))
					this.userList.set(i, currUserName + " (Offline)");
				else
					this.userList.set(i, currUserName);
			}
		}

		// Add new user 
		for (String user : allUser) {
			if (user != null && 
					!this.userList.contains(user) && 
					!this.userList.contains(user + " (!)") &&
					!this.userList.contains(user + " (Offline)")) {
				this.userList.add(user);
			}
		}

		// Mark user with unread msg
		for (int i = 0; i < this.userList.size(); ++i) {
			String user = getUserName(this.userList.get(i));
			if (user != null && userMsgNotify.get(user) != null && userMsgNotify.get(user)) {
				this.userList.set(i, user + " (!)");
			}
		}

	}

	// Push sent/received msg into screen
	public void storeMsg(String sender, String msg, boolean all) {

		String strMsg = String.format("[%s | %s] %s\n", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
				sender, msg);

		if (all)
			sender = ChatServerMaster.strPublic;

		if (userMsg.get(sender) == null)
			userMsg.put(sender, strMsg);
		else
			userMsg.put(sender, userMsg.get(sender) + strMsg);
		
		// Notify user with unread msg
		userMsgNotify.put(sender, true);
	}

	public void parse(ChatMsg cmd) {
		if (cmd.msgType.equals("USERS")) {
			getUserList(cmd.msgLoad); // Get user list 
		} else if (cmd.msgType.equals("FROMPUB")) {
			String sender = cmd.msgSend;
			storeMsg(sender, cmd.msgLoad, true); // Get public msg
		} else if (cmd.msgType.equals("FROM")) {
			String sender = cmd.msgSend;
			storeMsg(sender, cmd.msgLoad, false); // Get private msg
		}
	}

	// Remove suffix of username 
	private String getUserName(String user) {
		if (user == null)
			return null;

		Pattern p = Pattern.compile("([\\w\\d]+)( \\((\\*|Offline)\\))?");
		Matcher m = p.matcher(user);
		m.find();
		return m.group(1);
	}

	// Check if the user is offline
	private boolean isOffline(String user) {
		if (user == null)
			return false;

		Pattern p = Pattern.compile("([\\w\\d]+)( \\((\\*|Offline)\\))?");
		Matcher m = p.matcher(user);
		m.find();
		return m.group(3) != null && m.group(3).equals("Offline");
	}

	// Send msg
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
		userMsg = new HashMap<String, String>();
		userMsgNotify = new HashMap<String, Boolean>();
		// Monitor public msg
		userList = FXCollections.observableArrayList(ChatServerMaster.strPublic);
		
		stage.setTitle("[User: " + userName + "]");

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		userListView = new ListView<>(userList);
		userListView.setItems(userList);
		userListView.getSelectionModel().select(0);

		VBox userListBox = new VBox();
		VBox.setVgrow(userListView, Priority.ALWAYS);
		userListBox.getChildren().addAll(userListView);

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

		Button btn = new Button("Send");
		HBox hbBtn = new HBox(0);
		hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
		hbBtn.getChildren().add(btn);
		sendBox.getChildren().add(hbBtn);

		grid.add(userListBox, 0, 0);
		grid.add(sendBox, 1, 0);
		Scene scene = new Scene(grid);
		stage.setScene(scene);

		// When selected new user, switch the content of msg area
		userListView.getSelectionModel().selectedItemProperty()
				.addListener((ObservableValue<? extends String> ov, String old_val, String new_val) -> {
					String oldV = getUserName(old_val);
					String newV = getUserName(new_val);

					if (!oldV.equals(newV)) {
						if (userMsg.get(newV) != null) {
							listMsg.setText(userMsg.get(newV));
							userMsgNotify.put(newV, false);
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
				}
			}
		});

		// Create new thread to refresh user list
		Task<Void> task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				while (out != null && in != null) {
					try {
						Thread.sleep(300);

						ChatMsg msg = new ChatMsg("GETUSRS", "", "", "");
						ChatMsg.sendMsg(msg, out);

						ChatMsg cmd = (ChatMsg) in.readObject();
						if (cmd == null)
							break;

						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								parse(cmd);
								if (msgReachEnd) {
									listMsg.setScrollTop(Double.MAX_VALUE);
									msgReachEnd = false;
								}

								int curUsrId = userListView.getSelectionModel().getSelectedIndex();
								String curUsr = getUserName(userListView.getSelectionModel().getSelectedItem());

								if (isOffline(userListView.getSelectionModel().getSelectedItem())) {
									sendMsg.setDisable(true);
									btn.setDisable(true);
								} else {
									sendMsg.setDisable(false);
									btn.setDisable(false);
								}

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
		th.setDaemon(true);
		th.start();
	}

	// Process login
	private boolean checkLogin() {
		Alert alert = new Alert(AlertType.ERROR);

		try {
			s = new Socket(ip, ChatServerMaster.port);
			out = new ObjectOutputStream(s.getOutputStream());
			ChatMsg msg = new ChatMsg("LOGIN", userName, passWord, "");
			ChatMsg.sendMsg(msg, out);

			in = new ObjectInputStream(s.getInputStream());
			ChatMsg reply = (ChatMsg) in.readObject();

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
