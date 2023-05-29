package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import entities.Professor;
import entities.Question;
import entities.Student;
import entities.User;

public class MySQLConnection 
{
	static Connection conn;
	static MySQLConnection mySqlController=null;
	
	
	
	/**
	 * private constructor for Singleton DP
	 */
	private MySQLConnection() {	
	}
	
	/**
	 * @return
	 */
	public static MySQLConnection getInstance() {
		if(mySqlController==null)
			mySqlController=new MySQLConnection();
		return mySqlController;
	}
	
	/**
	 * Establishing a connection to a MySQL database
	 * @param username of database
	 * @param password of database
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public  boolean connectToDB(String URL, String username, String password) 
	{
		try 
		{
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            System.out.println("Driver definition succeed");
        } catch (Exception ex) {
        	/* handle the error*/
        	 System.out.println("Driver definition failed");
        	 return false;
        	 }
        
        try 
        {
            conn = DriverManager.getConnection(URL, username, password);
            System.out.println("SQL connection succeed");
            return true;

     	} 
        catch (SQLException ex) 
     	{/* handle any errors*/
        
         	System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return false;
        }
   	}
	
	/**
	 * This method, loadQuestions, retrieves a list of questions from a database table
	 * @return an ArrayList of Question objects
	 */
	public static ArrayList<Question> loadProfessorQuestions(String id)
	{
		ArrayList<Question> qArr = new ArrayList<Question>();
	    try
		{
	    	//loading all the questions from the table
	    	PreparedStatement ps = conn.prepareStatement("SELECT q.*, qc.course_id, c.course_name "
										    		   + "FROM question q "
										    		   + "JOIN question_course qc ON q.id = qc.question_id "
										    		   + "JOIN course c ON qc.course_id = c.course_id "
										    		   + "WHERE q.professor_id = ?");
		    ps.setString(1, id);
		    ResultSet rs = ps.executeQuery();
			while (rs.next()) 
			{
				Question q = new Question(rs.getInt("question_number"), rs.getString("id"),
				rs.getString("subject_id"), rs.getString("question_text"), rs.getString("professor_full_name"),
				rs.getString("professor_id"), rs.getString("correct_answer"), 
				new String[]{rs.getString("answer1"), rs.getString("answer2"), rs.getString("answer3"), rs.getString("answer4")});
				q.setCourse(rs.getString("course_id") + " - " + rs.getString("course_name"));
				qArr.add(q);
			}
		} 
		catch (SQLException e) {e.printStackTrace();}
		return qArr;
	}
	
	/**
	 * This method is responsible for updating question text and question number records in the database. 
	 * @param arr represents the updated question data.
	 */
	public static void saveQuestionToDB(ArrayList<Question> arr) 
	{
	    try 
	    {
	        PreparedStatement ps = conn.prepareStatement("UPDATE question SET question_text = ?, question_number = ? WHERE id = ?");
	        for (Question question : arr) 
	        {
	            ps.setString(1, question.questionText);
	            ps.setInt(2, question.questionNumber);
	            ps.setString(3, question.getId());
	            ps.executeUpdate();
	        }
	    } 
	    catch (SQLException e)
	    {
	        e.printStackTrace();
	    }
	}
	
	/**
	 * This method is responsible for adding a question to the database. 
	 * @param question represents the updated question data.
	 */
	public static void addQuestionToDB(Question question) 
	{
	    try 
	    {
	    	PreparedStatement ps = conn.prepareStatement( "INSERT INTO question (question_number, id, "
										    			+ "subject_id, question_text, professor_full_name, professor_id, "
										    			+ "correct_answer, answer1, answer2, answer3, answer4) "
										    			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
	       ps.setInt(1, question.getQuestionNumber());
	       ps.setString(2, question.getId());
	       ps.setString(3, question.getSubject());
	       ps.setString(4, question.getQuestionText());
	       ps.setString(5, question.getAuthor());
	       ps.setString(6, question.getProfessorId());
	       ps.setString(7, question.getCorrectAnswer());
	       ps.setString(8, question.getAnswers()[0]);
	       ps.setString(9, question.getAnswers()[1]);
	       ps.setString(10, question.getAnswers()[2]);
	       ps.setString(11, question.getAnswers()[3]);
	       ps.executeUpdate();
	    } 
	    catch (SQLException e){e.printStackTrace();}
	}
	
	/**
	 * Verifies user info for login.
	 * @param loginInfo
	 * @return user object or null.
	 */
	public static User verifyLogin(ArrayList<String> loginInfo)
	{
		String username = loginInfo.get(1);
		String password = loginInfo.get(2);
		User newUser = null;
		//lOading all the questions from the table
		try
		{
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
			stmt.setString(1, username);
			stmt.setString(2, password);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) 
			{
				String user_id = rs.getString("user_id");
				String first_name = rs.getString("first_name");
				String last_name = rs.getString("last_name");
				String email = rs.getString("email");
				String role = rs.getString("role");
				int isLogged = rs.getInt("isLogged");
				if (isLogged == 0)
				{
					PreparedStatement ps = conn.prepareStatement("UPDATE users SET isLogged = 1 WHERE user_id = ?");
					ps.setString(1, user_id);
					ps.executeUpdate();
					switch(role)
					{
						case "student":
							newUser = new Student(user_id, first_name, last_name, email, username, password, null);
							break;
						case "professor":
							newUser = new Professor(user_id, first_name, last_name, email, username, password, null);
							break;
					}
				}
				else 
				{
					return new User("logged", null, null, null, null, null, null);
				}
			}
		}
		catch (SQLException e){}
		return newUser;
	}
	
	public static void logout(String id)
	{
		try
		{
			PreparedStatement ps = conn.prepareStatement("UPDATE users SET isLogged = 0 WHERE user_id = ?");
			ps.setString(1, id);
			ps.executeUpdate();
		}
		catch(SQLException e){}
	}
	
	public static ArrayList<String> getProfessorSubjects(String id)
	{
		ArrayList<String> answer = new ArrayList<String>();
		answer.add("professor subjects");
		try
		{
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM professor_subject WHERE professor_id = ?");
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) 
			{
				String subject_id = rs.getString("subject_id");
				PreparedStatement tempPs = conn.prepareStatement("SELECT * FROM subject WHERE subject_id = ?");
				tempPs.setString(1, subject_id);
				ResultSet tempRs = tempPs.executeQuery();
				while (tempRs.next())
				{
					String subject_name = tempRs.getString("subject_name");
					answer.add(subject_id + " - " + subject_name);
				}
			}
		}
		catch(SQLException e){e.printStackTrace();}
		return answer;
	}
	
	public static ArrayList<String> getSubjectCourses(String id)
	{
		ArrayList<String> answer = new ArrayList<String>();
		answer.add("subject courses");
		try
		{
			PreparedStatement ps = conn.prepareStatement("SELECT course_id, course_name FROM course WHERE course_subject_id = ?");
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) 
			{
				String course_id = rs.getString("course_id");
				String course_name = rs.getString("course_name");
				answer.add(course_id + " - " + course_name);
			}
		}
		catch(SQLException e){e.printStackTrace();}
		return answer;
	}
	
	public static ArrayList<String> getAmountOfQuestions()
	{
		ArrayList<String> answer = new ArrayList<String>();
		answer.add("amount of questions");
		try 
		{
			ResultSet rs = conn.createStatement().executeQuery("SELECT MAX(question_number) FROM question");
			while (rs.next())
			{
				Integer maxQuestion = rs.getInt("MAX(question_number)");
				answer.add(maxQuestion.toString());
			}
				
		}
		catch(SQLException e){e.printStackTrace();}
		return answer;
	}
	
	public static void addQuestionCourses(ArrayList<String> courses)
	{
		String question_id = courses.get(0);
		courses.remove(0);
		try 
		{
			PreparedStatement ps = conn.prepareStatement("INSERT INTO question_course (question_id, course_id) VALUES (?, ?)");
			for (String s : courses)
			{
				ps.setString(1, question_id);
				String[] tempStr = s.split("\\s+");
				ps.setString(2, tempStr[0]);
				ps.executeUpdate();
			}
		}
		catch(SQLException e){e.printStackTrace();}
	}
}
