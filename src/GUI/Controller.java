package GUI;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import common.TaskObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class Controller implements Initializable {
	static String _input;
	static UIMain _UI = new UIMain();
	ArrayList<TaskObject> taskList = _UI.getTaskList();
	HelpPopupController popupController = new HelpPopupController();
	
	@FXML
	private TextField userInput;
	@FXML 
	private static BorderPane layout;
	@FXML
	private TextFlow feedbackBox;
	@FXML
	private Text feedbackMessage;
	@FXML
	private TableView<TaskObject> taskTable;
	@FXML
	private TableColumn<TaskObject, String> indexColumn;
	@FXML
	private TableColumn<TaskObject, String> taskColumn;
	@FXML
	private TableColumn<TaskObject, String> statusColumn;
	@FXML
	private TableColumn<TaskObject, Integer> startDateColumn;
	@FXML
	private TableColumn<TaskObject, Integer> endDateColumn;
	@FXML
	private TableColumn<TaskObject, Integer> startTimeColumn;
	@FXML
	private TableColumn<TaskObject, Integer> endTimeColumn;
	
	@FXML
	public void handleEnterPressed(KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) {
    	System.out.println(userInput.getText());
    	_input = userInput.getText();
    	_UI.passInput(_input);
    	userInput.clear();
    	displayMessage();
    	display();
    	}
	}
	
	@FXML
	public void handleHelpPressed(KeyEvent event) throws IOException {
		if (event.getCode() == KeyCode.F1) {
			_UI.passInput("help");
			ArrayList<String> helpManualList = _UI.getHelpOutput();
			popupController.setHelpStage(helpManualList);
			System.out.println("help activated");
		}
	}
	
	private void displayMessage() {
		feedbackMessage.setText(_UI.getMessage());
		feedbackBox.getChildren().clear();
		feedbackBox.getChildren().add(feedbackMessage);
	}

	private void display() {
		ObservableList<TaskObject> groupData = FXCollections.observableArrayList(taskList);
		
		//populate the index column
		indexColumn.setCellFactory(col -> new TableCell<TaskObject, String>() {
		    @Override
			public void updateIndex(int index) {
		        super.updateIndex(index);
		        if (isEmpty() || index < 0) {
		            setText(null);
		        } else {
		            setText(Integer.toString(index+1));
		        }
		    }
		});
		
		//populate task and status column
		taskColumn.setCellValueFactory(new PropertyValueFactory<TaskObject, String>("Title"));
		statusColumn.setCellValueFactory(new PropertyValueFactory<TaskObject, String>("status"));
		endDateColumn.setCellValueFactory(new PropertyValueFactory<TaskObject, Integer>("endDate"));
		startDateColumn.setCellValueFactory(new PropertyValueFactory<TaskObject, Integer>("startDate"));
		startTimeColumn.setCellValueFactory(new PropertyValueFactory<TaskObject, Integer>("startTime"));
		endTimeColumn.setCellValueFactory(new PropertyValueFactory<TaskObject, Integer>("endTime"));
				
		taskTable.setItems(groupData);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		display();
	}	
} 