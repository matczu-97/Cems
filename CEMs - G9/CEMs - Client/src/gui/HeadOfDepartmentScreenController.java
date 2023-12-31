package gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import client.ClientMessageHandler;
import client.ClientUI;
import control.UserController;
import entities.HeadOfDepartment;
import entities.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.text.Text;

/**
 * Class that represents the main screen of the head of department.
 */
public class HeadOfDepartmentScreenController implements Initializable
{
	private String path="/gui/HeadOfDepartmentScreen.fxml";
	
	private static StatisticsChooseScreenController 
	
	statisticsScreen = new StatisticsChooseScreenController();
	
	static HeadOfDepartment u ;
	
    @FXML
    private Text Alert;
    
    @FXML
    private Text idTXT;

    @FXML
    private Text nameTXT;

    @FXML
    private Text welcomeText;
    
    @FXML
    private Button checkResBtn;

    @FXML
    private Button createExamBtn;

    @FXML
    private Button exitBtn;

    @FXML
    private Button logoutBtn;

    @FXML
    private Button manageQstBtn;

    /**
  	 * Initializes the JavaFX controller during application startup.
  	 * @param user
  	 */
	public void start(User user) 
	{
		u = (HeadOfDepartment) user;
		Platform.runLater(()-> ScreenUtils.createNewStage(path).show());
	}
    
	/**
	 * Initializes the GUI with the given logic.
	 * @param location
	 * @param resources
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) 
	{
		ClientMessageHandler.setHeadOfDepScreenController(this);
		welcomeText.setText("Welcome back " + u.getFirst_name());
		nameTXT.setText(u.get_fullName());
		idTXT.setText(u.getUser_id());
		ClientUI.chat.accept("check for pending requests");
	}
	
    /**
     * Exits the GUI window.
	 * disconnects from the server and exits from the GUI. 
     * @param event
     */
    @FXML
    void exit(ActionEvent event) 
    {
    	UserController.userExit(u);
    }

    /**
     * Logs out from the current user + sets the correct flag in the DB.
     * @param event
     */
    @FXML
    void logout(ActionEvent event)
    {
    	u.resetArrays();
    	UserController.close(event);
		ScreenUtils.createNewStage("/gui/LoginScreen.fxml").show();
		UserController.logoutUser(u);
	}

    /**
     * Method to let the head access all the exams in the DB.
     * @param event
     */
    @FXML
    void accessAllExams(ActionEvent event)
    {
    	UserController.hide(event);
    	try 
    	{
			ExamReportController.start(u);
		} catch (Exception e) {}
    }
    
    /**
     * Method to let the head access all the questions in the DB.
     * @param event
     */
    @FXML
    void accessAllQuestions(ActionEvent event)
    {
        {
        	UserController.hide(event);
        	try 
        	{
    			QuestionBankScreenController.start(u);
    		} catch (Exception e) {}
        }
        
    }
    
    /** navigate to a screen where the user can chose which statistics he wants
     * @param event
     */
    @FXML
    void openStatistics(ActionEvent event) 
    {
    	UserController.hide(event);
    	statisticsScreen.start(u);
    }
    
    /**
     * Starts time pending requests screen
     * @param event
     */
    @FXML 
    void grantApproval(ActionEvent event)
    {
    	UserController.hide(event);
    	try 
    	{
			TimePendingRequestsController.start(u);
		} catch (Exception e) {}
    }
    
    /**
     * Setter.
     * @param msg
     */
	public void setUserArr(ArrayList<?> msg)
	{
			u.setArrUser(msg);
	}
	
	/**
	 * @return hof id
	 */
	public String getId()
	{
		return u.getUser_id();
	}
}