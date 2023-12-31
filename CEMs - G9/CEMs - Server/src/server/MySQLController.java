package server;

import java.io.File;
import java.io.FileInputStream;
import java.sql.*;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import entities.Course;
import entities.Distribution;
import entities.Exam;
import entities.ExamFile;
import entities.ExamProfessorReport;
import entities.ExamResults;
import entities.ExamStat;
import entities.ExamStatistics;
import entities.ExamTemplate;
import entities.ExamTimeChange;
import entities.HeadOfDepartment;
import entities.Professor;
import entities.Question;
import entities.Student;
import entities.StudentExam;
import entities.User;

/**
 * A class that handles all the queries and the connection to the database.
 */
public class MySQLController 
{
	
	static Connection conn;
	
	private static volatile MySQLController INSTANCE;

	/**
	 * private constructor for Singleton DB
	 */
	private MySQLController() {}
	
	/**
	 * @return instance of MYSQLController
	 */
	public static MySQLController getInstance() 
	{
		if (INSTANCE == null)
			synchronized (MySQLController.class)
			{
				if (INSTANCE == null)
					INSTANCE = new MySQLController();
			}
		return INSTANCE;
	}
	
	/**
	 * Establishing a connection to a MySQL database
	 * @param username of database
	 * @param password of database
	 * @return
	 */
	public  boolean connectToDB(String URL, String username, String password) 
	{
		try 
		{
            Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
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
	public ArrayList<Question> loadProfessorQuestions(String id)
	{
		ArrayList<Question> qArr = new ArrayList<Question>();
		qArr.add(new Question("professor questions"));
	    try
		{
	    	//loading all the questions from the table
	    	PreparedStatement ps = conn.prepareStatement("SELECT q.*, s.subject_name " +
									                     "FROM question q " +
									                     "JOIN subject s ON q.subject_id = s.subject_id " +
									                     "WHERE professor_id = ?");
		    ps.setString(1, id);
		    ResultSet rs = ps.executeQuery();
			while (rs.next()) 
			{
				String s = rs.getString("subject_id") + " - " + rs.getString("subject_name");
				Question q = new Question(rs.getString("question_number"), rs.getString("id"),
				s, rs.getString("question_text"), rs.getString("professor_full_name"),
				rs.getString("professor_id"), rs.getString("correct_answer"), 
				new String[]{rs.getString("answer1"), rs.getString("answer2"), rs.getString("answer3"), rs.getString("answer4")});
				qArr.add(q);
			}
		} 
		catch (SQLException e) {e.printStackTrace();}
		return qArr;
	}
	
	/**
	 * @param id
	 * @return all questions in data base the professor from the 
	 * appropriate department 
	 */
	public ArrayList<Question> loadHeadOfDepartmentQuestions(String id){
		
		
		ArrayList<Question> qArr = new ArrayList<Question>();
		qArr.add(new Question("professor questions"));
	    try
		{
	    	//loading all the questions from the table
	    	PreparedStatement ps = conn.prepareStatement("SELECT q.*,s.subject_name FROM  question as q, subject as s WHERE professor_id IN"
	    			+ " (SELECT p.professor_id FROM professor_department as p\r\n"
	    			+ "WHERE p.head_of_department_id=?) AND q.subject_id=s.subject_id;");
		    ps.setString(1, id);
		    ResultSet rs = ps.executeQuery();
			while (rs.next()) 
			{
				String s = rs.getString("subject_id") + " - " + rs.getString("subject_name");
				Question q = new Question(rs.getString("question_number"), rs.getString("id"),
				s, rs.getString("question_text"), rs.getString("professor_full_name"),
				rs.getString("professor_id"), rs.getString("correct_answer"), 
				new String[]{rs.getString("answer1"), rs.getString("answer2"), rs.getString("answer3"), rs.getString("answer4")});
				qArr.add(q);
			}
		} 
		catch (SQLException e) {e.printStackTrace();}
		return qArr;
	}
		
	/**
	 * This method loads all the exams that the student can take.
	 * @return an ArrayList of Exam objects
	 */
	public ArrayList<Exam>loadStudentExams()
	{
		ArrayList<Exam> eArr = new ArrayList<>();
		eArr.add(new Exam ("student exams"));
		try {
	    	//loading all the  exams from the exam table
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM exam WHERE isActive <> -1 ORDER BY isActive DESC ");
		    ResultSet rs = ps.executeQuery();
			while (rs.next()) 
			{
				Exam e = new Exam(rs.getString("exam_number"), rs.getString("subject_id") , rs.getString("course_id"), rs.getString("exam_id"),
				rs.getInt("num_questions"), rs.getInt("time"), rs.getString("examinees_notes"), rs.getString("professor_notes"),
				rs.getString("professor_full_name"), rs.getString("professor_id"), rs.getString("password"), rs.getString("exam_name"));
				e.setIsActive(rs.getString("isActive"));
				e.setType(rs.getString("type"));
				eArr.add(e);
			}
		} 
		catch (SQLException e) {e.printStackTrace();}
		return eArr;
	}
	
	/**
	 * @param id
	 * @param role
	 * @return the exams of the professor which student already took and have grades.
	 */
	public ArrayList<?> loadProfessorExams_toReport(String id, String role)
	{
		ResultSet rs;
		PreparedStatement ps;
		ArrayList<ExamTemplate> arr = new ArrayList<>();
		try {
			if (role.equals("professor"))
			{
				ps = conn.prepareStatement("SELECT DISTINCT e.exam_name, e.exam_id, e.course_id " +
										   "FROM exam AS e, student_exam AS se " +
										   "WHERE e.professor_id = ? " +
										   "AND se.exam_id = e.exam_id " +
										   "AND e.isActive = -1;");
			}
			//In case it is a head of department - get all exams.
			else
			{
				ps = conn.prepareStatement("SELECT DISTINCT e.exam_name, e.exam_id, e.professor_full_name, e.isActive " +
										   "FROM exam AS e " +
										   "WHERE e.professor_id IN (SELECT p.professor_id " +
										   "FROM professor_department AS p " +
										   "WHERE p.head_of_department_id = ?)");
			}
			ps.setString(1, id);
			rs = ps.executeQuery();
			while(rs.next()) 
			{
				ExamTemplate e = new ExamTemplate(rs.getString(1),rs.getString(2),rs.getString(3));
				//So the head can't access exams that were not activated.
				if (role.equals("head"))
					e.setIsActive(rs.getString(4));
				arr.add(e);
			}	
		}catch(SQLException e) {e.printStackTrace();}
		return arr;
	}
	
	/**
	 * @param exam
	 * @return statistics on exam of professor average max min histogram fails ... 
	 */
	public ExamProfessorReport  professorExamStat(String exam) 
	{
		ResultSet rs;
		ExamProfessorReport examRep;
		try 
		{
			Statement st = conn.createStatement();
			rs=st.executeQuery("SELECT round(AVG(grade),2) as average ,max(grade) as max ,min(grade) AS min,\r\n" +
							   "round((SELECT count(*) FROM student_exam WHERE exam_id=\""+exam+"\" AND grade<55)/\r\n" +
							   "(SELECT count(*) FROM student_exam WHERE exam_id=\""+exam+"\")*100,2) as fails\r\n" +
					           "FROM student_exam WHERE exam_id=\""+exam+"\";");
			rs.next();
			examRep=new ExamProfessorReport(exam,"", rs.getDouble(1), rs.getInt(2), rs.getInt(3), rs.getDouble(4));
			examRep.setMedian(medianExam(exam));
			examRep.setDistribution(ExamHistogramResult(exam));
			examRep.setStat(examStatistics(exam));
			return examRep;
		}catch(SQLException e) {e.printStackTrace();}
		return null ;
	}
	
	/**
	 * @param exam
	 * @return exam statistics for exam with the given ID
	 */
	private ExamStat examStatistics(String exam)
	{

		ResultSet rs;
		ExamStat stat;
		try {
			Statement st = conn.createStatement();
			rs = st.executeQuery("SELECT * FROM exam_stats WHERE exam_id=\""+exam+"\"");
			rs.next();
			stat=new ExamStat(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7));
			return stat;
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param exam
	 * @return returnning the median - the grades coming sorted already
	 */
	private double medianExam(String exam) {
		ArrayList<Number> grades=new ArrayList<>();
		ResultSet rs;
		
		try {
			
			Statement st = conn.createStatement();
			rs =st.executeQuery("SELECT grade FROM student_exam\r\n"
					+ "WHERE exam_id=\""+exam+"\"\r\n"
					+ "order by grade;");
			while(rs.next()) {
				grades.add(rs.getInt(1));
			}
			return calMeddian(grades);
		}catch(SQLException e) {
			e.printStackTrace();
		}
		
		return 0;
		
	}
	
	/**
	 * the array is sorted so the median does not need any calculation
	 * 
	 * @param array
	 * @return the median of all the grades - they came ordered from the database
	 */
	private double calMeddian(ArrayList<Number> array)
	{
		if (array.size() % 2 == 0)
		{
			return ((array.get((int) ((array.size() / 2) - 1)).doubleValue()
					+ array.get((int) (array.size() / 2)).doubleValue()) / 2);
		} 
		else
			return array.get((int) (array.size() / 2)).doubleValue();

	}
	
	/**
	 * @param exam
	 * @return the amount of student that receive the grade shown in the query
	 */
	private Distribution ExamHistogramResult(String exam){
		ResultSet rs;
		Distribution dist=new Distribution();
		
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT \r\n"
					+ "    CASE \r\n"
					+ "        WHEN grade >= 0 AND grade <= 55 THEN '0-54'\r\n"
					+ "        WHEN grade > 55 AND grade <= 75 THEN '55-65'\r\n"
					+ "        WHEN grade > 10 AND grade <= 20 THEN '66-75'\r\n"
					+ "        WHEN grade > 75 AND grade <= 85 THEN '76-85'\r\n"
					+ "        WHEN grade > 85 AND grade <= 95 THEN '86-95'\r\n"
					+ "        WHEN grade > 95 AND grade <= 100 THEN '96-100'\r\n"
					+ "        \r\n"
					+ "    END AS grade_range,\r\n"
					+ "    COUNT(*) AS student_count\r\n"
					+ "FROM \r\n"
					+ "    student_exam as exam\r\n"
					+ "WHERE \r\n"
					+ "    exam.exam_id =\""+exam+"\"\r\n"
					+ "GROUP BY \r\n"
					+ "    grade_range "
					+ "order by grade_range;");
			
			rs=ps.executeQuery();
			
			while(rs.next()) 
				dist.addToRange(rs.getString(1), rs.getInt(2));
		}catch(SQLException e) {e.printStackTrace();}
		return dist;
	}
	
	/**
	 * This method is responsible for updating question text and question number records in the database. 
	 * @param arr represents the updated question data.
	 */
	public void editQuestionInDb(ArrayList<String> arr) 
	{
	    try 
	    {
	        PreparedStatement ps = conn.prepareStatement("UPDATE question SET question_text = ?, correct_answer = ?, "
	        									       + "answer1 = ?, answer2 = ?, answer3 = ?, answer4 = ? WHERE id = ?");
	        ps.setString(1, arr.get(1));
	        ps.setString(2, arr.get(2));
	        ps.setString(3, arr.get(3));
	        ps.setString(4, arr.get(4));
	        ps.setString(5, arr.get(5));
	        ps.setString(6, arr.get(6));
	        ps.setString(7, arr.get(0));
	        ps.executeUpdate();
	    } 
	    catch (SQLException e) {e.printStackTrace();}
	}
	
	/**
	 * This method is responsible for adding a question to the database. 
	 * @param question represents the updated question data.
	 */
	public void addQuestionToDB(Question question) 
	{
	    try 
	    {
	    	PreparedStatement ps = conn.prepareStatement( "INSERT INTO question (question_number, id, " +
										    			  "subject_id, question_text, professor_full_name, professor_id, " +
										    			  "correct_answer, answer1, answer2, answer3, answer4) " +
										    			  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" );
	       ps.setString(1, question.getQuestionNumber());
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
	@SuppressWarnings("rawtypes")
	public  User verifyLogin(ArrayList<String> loginInfo)
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
							newUser = new Student(user_id, first_name, last_name, email, username, password, "student");
							break;
						case "professor":
							newUser = new Professor(user_id, first_name, last_name, email, username, password, "professor");
							break;
						case "head":
							newUser = new HeadOfDepartment(user_id,first_name,last_name,email,user_id,password,"head");
					}
				}
				else 
				{
					return new User("logged", null, null, null, null, null, null);
				}
			}
		}
		catch (SQLException e){e.printStackTrace();}
		return newUser;
	}
	
	/**
	 * This method sets a certain user's logged status to 0.
	 * @param id
	 */
	public  void logout(String id)
	{
		try
		{
			PreparedStatement ps = conn.prepareStatement("UPDATE users SET isLogged = 0 WHERE user_id = ?");
			ps.setString(1, id);
			ps.executeUpdate();
		}
		catch(SQLException e){}
	}
	
	/**
	 * @return amount of exams in data base.
	 */
	public ArrayList<String> getAmountOfQuestions()
	{
		ArrayList<String> answer = new ArrayList<String>();
		answer.add("amount of questions");
		try 
		{
			ResultSet rs = conn.createStatement().executeQuery("SELECT MAX(question_number) FROM question");
			while (rs.next())
			{
				String maxQuestion = rs.getString("MAX(question_number)");
				answer.add(maxQuestion);
			}
				
		}
		catch(SQLException e){e.printStackTrace();}
		return answer;
	}
	
	/**
	 * this method adds the courses that were assigned to a question and adds it to 
	 * question_course table in DB.
	 * @param courses
	 */
	public  void addQuestionCourses(ArrayList<String> courses)
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
	
	/**
	 * this method sets isLogged to ALL users to 0 when the server disconnects.
	 */
	public  void logoutAllUsers()
	{
		try 
		{
			Statement st = conn.createStatement();
			st.executeUpdate("UPDATE users SET isLogged = 0");
		}
		catch(SQLException e){e.printStackTrace();}
	}

	/**
	 * this method deleted a question with the given ID.
	 * @param string
	 */
	public boolean deleteQuestionFromDb(String string) 
	{
		try 
		{
			PreparedStatement ps = conn.prepareStatement("DELETE FROM question WHERE id = ?");
			ps.setString(1, string);
			ps.executeUpdate();
			return true;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			return false;
			}
	}
	
	
	/**
	 * @param id
	 * @return map with subject that are assigned to the professor as keys 
	 * and the values are the courses of each subject.
	 */
	public Map<String, ArrayList<String>> getProfessorSubjectsAndCourses(String id) 
	{
	    Map<String, ArrayList<String>> resultMap = new LinkedHashMap<>();
	    try 
	    {
	        PreparedStatement ps = conn.prepareStatement( "SELECT subject.subject_id, subject.subject_name, course.course_id, course.course_name " +
	        											  "FROM professor_subject " +
	        											  "JOIN subject ON professor_subject.subject_id = subject.subject_id " +
	        											  "JOIN course ON subject.subject_id = course.course_subject_id " +
	        											  "WHERE professor_subject.professor_id = ?");
	        ps.setString(1, id);
	        ResultSet rs = ps.executeQuery();
	        while (rs.next())
	        {
	            String subject_id = rs.getString("subject_id");
	            String subject_name = rs.getString("subject_name");
	            String course_id = rs.getString("course_id");
	            String course_name = rs.getString("course_name");
	            String subject = subject_id + " - " + subject_name;
	            if (resultMap.containsKey(subject)) 
	                resultMap.get(subject).add(course_id + " - " + course_name);
	             else 
	            {
	                ArrayList<String> courses = new ArrayList<>();
	                courses.add(course_id + " - " + course_name);
	                resultMap.put(subject, courses);
	            }
	        }
	    } catch (SQLException e) { e.printStackTrace();}
	    return resultMap;
	}
	
	/**
	 * @param id
	 * @return the courses that are assigned to a question with the given ID
	 */
	public ArrayList<Question> loadCourseQuestions (String id)
	{
		ArrayList<Question> answer = new ArrayList<>();
		answer.add(new Question("course questions"));
		try 
		{
			PreparedStatement ps = conn.prepareStatement("SELECT qc.question_id, q.question_text, " +
														 "q.correct_answer, q.answer1, q.answer2, q.answer3, q.answer4 " +
														 "FROM question_course qc " +
														 "JOIN question q ON qc.question_id = q.id " +
														 "WHERE qc.course_id = ?");
			ps.setString(1, id);
		    ResultSet rs = ps.executeQuery();
		    while (rs.next())
		    {
		    	Question q = new Question(null, rs.getString(1), null, rs.getString(2), null, null, rs.getString(3), 
		    							  new String[]{rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)}); 
		    	q.setCourse(id);
		        answer.add(q);
			}
		} catch (SQLException e) { e.printStackTrace();}
		return answer;
	}
	
	/**@author czmat
	 * @param role
	 * @return the function receives  or a student or a professor request and load all the 
	 * Appropriate users
	 */
	public ArrayList<?> getAllStudents(String role){
		try {
			ArrayList<User> arrayS=new ArrayList<>();
			Statement st = conn.createStatement();
			ResultSet rs =st.executeQuery("SELECT * FROM users WHERE role=\""+role+"\"");
			while(rs.next()) {
				arrayS.add(new User (rs.getString(1), rs.getString(2), 
						rs.getString(3), rs.getString(4), rs.getString(5),rs.getString(6), role));//can changed to be a specific student or user
			}
			return arrayS;
			
		}catch(Exception e) {
			e.printStackTrace();
					}
		return null;
	}

	
	/**
	 * @param id
	 * @return the function receives professor request and load all the 
	 * Appropriate professors belongs to the department 
	 */
	public ArrayList<?> getAllProffesors(String id){
		try {
			ArrayList<User> arrayS=new ArrayList<>();
			Statement st = conn.createStatement();
			ResultSet rs =st.executeQuery("SELECT u.* from users as u , professor_department as dp\r\n"
					+ "WHERE u.user_id=dp.professor_id AND dp.head_of_department_id=\""+id+"\";");
			while(rs.next()) {
				arrayS.add(new User (rs.getString(1), rs.getString(2), 
						rs.getString(3), rs.getString(4), rs.getString(5),rs.getString(6),"professor"));//can changed to be a specific student or user
			}
			return arrayS;
			
		}catch(Exception e) {
			e.printStackTrace();
					}
		return null;
	}

	
	
	
	/**
	 * the function return all the courses in the system 
	 * @return
	 */
	public ArrayList<Course> getAllCourses(String id){
		ArrayList<Course> arrayC=new ArrayList<>();
		Statement st;
		try {
			st = conn.createStatement();
		ResultSet rs =st.executeQuery("SELECT c.* FROM course as c, subject as sb\r\n"
				+ "WHERE c.course_subject_id=sb.subject_id AND sb.head_department_id=\""+ id +"\"");
		while(rs.next()) {
			arrayC.add(new Course (rs.getString(1), rs.getString(2), 
					rs.getString(3)));
		}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return arrayC;		
	}
	
	/**
	 * @param arr
	 * @return all the exam of the student and his grades orderd by grade
	 */
	public ArrayList<StudentExam> getAllStudentExams(ArrayList<String> arr){
		ArrayList<StudentExam> array=new ArrayList<>();
		ResultSet rs;		
		try {
			Statement st = conn.createStatement();
			rs =st.executeQuery("SELECT se.exam_id,se.grade,ex.exam_name FROM student_exam as se,exam as ex WHERE"
					+ " ex.exam_id=se.exam_id AND se.student_id=\""+arr.get(2)+"\" order by grade");
			while(rs.next()) {
				array.add(new StudentExam(rs.getString(1),rs.getInt(2),rs.getString(3)));
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if(array.isEmpty()) {
			array.add(new StudentExam("empty", 0, "empty"));
			return array;
		}
		double ave=AverageofStudent(arr.get(2));
		
		array.add(new StudentExam(String.valueOf(ave),0,""));
		
		return array;		
	}
	
	/**
	 * @param id
	 * @return the average of all the exams of this id of professor
	 */
	private double AverageofStudent(String id) {
		double ave=0;
		ResultSet rs;
		try {
		Statement st = conn.createStatement();
		rs =st.executeQuery("SELECT round(AVG(grade),2) AS average\r\n"
				+ "FROM student_exam WHERE student_id=\""+id+"\"");
		while(rs.next()) {
			ave=rs.getDouble(1);
		}
		return ave;
		
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	/**
	 * @param arr
	 * @return all the exam of the professor and his grades ordered by grade average
	 * or the course grades 
	 */
	public ArrayList<?> getAllprofessor_courseExams(ArrayList<String> arr){
		ArrayList<ExamStatistics> array=new ArrayList<>();
		ExamStatistics exam;
		ResultSet rs;		
		try {
			
			Statement st = conn.createStatement();
			rs =st.executeQuery("SELECT\r\n"
					+ "   e.exam_id,ex.exam_name,\r\n"
					+ "  round( AVG(e.grade),2) AS average_grade,\r\n"
					+ "  MAX(grade) AS max_grade,\r\n"
					+ "  MIN(grade) AS min_grade,\r\n"
					+ " round((SELECT COUNT(*) FROM student_exam WHERE student_exam.exam_id = e.exam_id AND grade < 55) / \r\n"
					+ "  (SELECT COUNT(*) FROM student_exam WHERE student_exam.exam_id = e.exam_id) * 100 ,2) AS fails\r\n"
					+ "FROM\r\n"
					+ "  student_exam AS e , exam as ex\r\n"
					+ "  WHERE e.exam_id=ex.exam_id AND "+arr.get(3)+"=\""+arr.get(2)+"\""
					+ "GROUP BY\r\n"
					+ "  ex.exam_id , ex.exam_name   order by average_grade;"
					);
			while(rs.next()) {
				 exam= new ExamStatistics(rs.getString(1), rs.getString(2), rs.getDouble(3),rs.getInt(4), rs.getInt(5), rs.getDouble(6));
				array.add(exam);
			}
			
			if(array.isEmpty()) {
				array.add(new ExamStatistics("empty","", 0,0,0,0));
				return array;
			}
			
			array.add(new ExamStatistics("average of all", null, AverageofExam(arr.get(2),arr.get(3)), 0, 0, 0));
		} catch (SQLException e) {e.printStackTrace();}
		return array;	
		
		
	}
	
	/**
	 * @param id
	 * @return the average of all the exams of this id of professor
	 */
	private double AverageofExam(String id,String path) {
		double ave=0;
		ResultSet rs;
		try {
		Statement st = conn.createStatement();
		rs =st.executeQuery("SELECT round(avg(se.grade),2) FROM student_exam as se, "
				+ "exam as ex WHERE se.exam_id=ex.exam_id AND "
				+ path +"=\""+id+"\"");
		while(rs.next()) {
			ave=rs.getDouble(1);
		}
		return ave;
		
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	/**
	 * @return amount of exams in data base.
	 */
	public ArrayList<String> getAmountOfExams() 
	{
		ArrayList<String> answer = new ArrayList<String>();
		answer.add("amount of exams");
		try 
		{
			//By getting the max number, we get the latest exam number so we can increment it when creating a new one.
			ResultSet rs = conn.createStatement().executeQuery("SELECT MAX(exam_number) FROM exam");
			while (rs.next())
			{
				String maxExam = rs.getString("MAX(exam_number)");
				answer.add(maxExam);
			}
				
		}
		catch(SQLException e){e.printStackTrace();}
		return answer;
	}
	
	/**
	 * this method adds the question that were assigned to an exam and adds it to 
	 * exam_question table in DB.
	 * @param questionInExams
	 */
	public void addExamQuestions(ArrayList<String> questionInExams)
	{
		try 
		{
			PreparedStatement ps = conn.prepareStatement("INSERT INTO exam_question (exam_id, question_id, score) VALUES (?, ?, ?)");
			for (String s : questionInExams)
			{
				String[] splitted = s.split("\\s+");
				ps.setString(1, splitted[0]);
				ps.setString(2, splitted[1]);
				ps.setString(3, splitted[2]);
				ps.executeUpdate();
			}
		}
		catch(SQLException e){e.printStackTrace();}
	}

	/**
	 * Adds a new exam to the DB.
	 * @param exam
	 */
	public void addExamToDB(Exam exam) 
	{
		 try 
		    {
		    	PreparedStatement ps = conn.prepareStatement( "INSERT INTO exam (exam_number, subject_id, course_id, exam_id, " +
											    			  "num_questions, time, examinees_notes, professor_notes, professor_full_name, " +
											    			  "professor_id, password, isActive, type, exam_name) " +
											    			  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" );
		       ps.setString(1, exam.getExam_number());
		       ps.setString(2, exam.getSubject_id());
		       ps.setString(3, exam.getCourse_id());
		       ps.setString(4, exam.getExam_id());
		       ps.setInt(5, exam.getNum_questions());
		       ps.setInt(6, exam.getTime());
		       ps.setString(7, exam.getExaminees_notes());
		       ps.setString(8, exam.getProfessor_notes());
		       ps.setString(9, exam.getProfessor_full_name());
		       ps.setString(10, exam.getProfessor_id());
		       ps.setString(11, exam.getPassword());
		       ps.setString(12, exam.getIsActive());
		       ps.setString(13, exam.getType());
		       ps.setString(14, exam.getExam_name());
		       ps.executeUpdate();
		    } 
		    catch (SQLException e){e.printStackTrace();}
	}
	
	/** 
	 * This method loads courses and their names.
	 * @return an ArrayList of String objects
	 */
	public ArrayList<String> getStudentCourses(){
        ArrayList<String> courses = new ArrayList<>();
        courses.add("student courses");
        try 
	    {
	        PreparedStatement ps = conn.prepareStatement("SELECT course.course_id, course.course_name FROM course ");
	        ResultSet rs = ps.executeQuery();
	        while (rs.next())
	        {
	            String course_id = rs.getString("course_id");
	            String course_name = rs.getString("course_name");
	            courses.add(course_id + " - " + course_name);
	        }
	    } catch (SQLException e) { e.printStackTrace();}
        return courses;
	}

	/**
	 * This method, loadProfessorExams, retrieves a list of exams from a database table
	 * @return an ArrayList of exam objects
	 */
	public ArrayList<Exam> loadProfessorExams(String id) 
	{
		ArrayList<Exam> eArr = new ArrayList<>();
		eArr.add(new Exam("professor exams"));
	    try
		{
	    	//loading all the exams from the table
	    	PreparedStatement ps = conn.prepareStatement("SELECT * FROM exam WHERE professor_id = ? AND isActive <> -1 ");
		    ps.setString(1, id);
		    ResultSet rs = ps.executeQuery();
			while (rs.next()) 
			{
				Exam e = new Exam(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
						          rs.getInt(5), rs.getInt(6), rs.getString(7), rs.getString(8),
						          rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(14));
				e.setIsActive(rs.getString(12));
				e.setType(rs.getString(13));
				eArr.add(e);
			}
		} 
		catch (SQLException e) {e.printStackTrace();}
		return eArr;
	}
		
	/**
	 * This method activates or deactivates an exam with the given id
	 * @param id
	 * @param isActive 
	 */
	public void updateExamStatus(String id, boolean isActive) 
	{
		try 
		{
			PreparedStatement ps = conn.prepareStatement("UPDATE exam SET isActive = ? WHERE exam_id = ?");
			ps.setInt(1, isActive ? 1 : -1);
			ps.setString(2, id);
			ps.executeUpdate();
		} catch (SQLException e) {e.printStackTrace();}
	}
	
	/**
	 * Creates an examFile with the specified exam ID for opening the exam.
	 * @param examID the ID of the exam file to open
	 * @return the ExamFile object
	 */
	public ExamFile openExamFile(String examID) {
		ExamFile examFile = new ExamFile(examID);
		Blob blob = null;
		ResultSet rs = null;
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT manual_exam_file FROM manual_exam WHERE idmanual_exam = ?");
			ps.setString(1, examID);
			rs = ps.executeQuery();
			while (rs.next()) {
				blob = rs.getBlob(1);
			}
			examFile.initArray((int) blob.length());
			examFile.setSize((int) blob.length());
			examFile.setMybytearray(blob.getBytes(1, examFile.getSize()));
			} catch (Exception e) {
				e.printStackTrace();
			}
			return examFile;
	}
	
	/**
	 * Uploads a docx file to the DB
	 * @param examID
	 * @param userID
	 * @param examPath
	 */
	public void uploadExamFile(String examID, String userID, String examPath) 
	{
		//ExamFile examFile = new ExamFile(examID);
		try {
		// Create a PreparedStatement for the SQL statement
        PreparedStatement ps = conn.prepareStatement("UPDATE student_manual_exam SET uploaded_file  = ? "
        		+ " WHERE exam_id = ? AND student_id = ?");
        // Read the file
        File file = new File(examPath);
        FileInputStream inputStream = new FileInputStream(file);
        // Set the BLOB parameter
        ps.setBinaryStream(1, inputStream, (int) file.length());
        ps.setString(2, examID);
        ps.setString(3, userID);
		ps.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method loads the questions of specified exam and returns an ArrayList of Question objects.
	 * @param examID the ID of the exam to retrieve questions for
	 * @return an ArrayList of Question objects representing the exam questions
	 */
	public ArrayList<Question> examQuestion(String examID){
		ArrayList<Question> qArr = new ArrayList<>();
		qArr.add(new Question("exam questions"));
		try 
		{
			PreparedStatement ps = conn.prepareStatement("SELECT q.*, eq.score FROM question q JOIN exam_question eq "
														+ "ON q.id = eq.question_id WHERE eq.exam_id = ?");
			ps.setString(1, examID);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Question q = new Question(rs.getString("question_number"), rs.getString("id"), rs.getString("subject_id"),
				rs.getString("question_text"), rs.getString("professor_full_name"),
				rs.getString("professor_id"), rs.getString("correct_answer"), 
				new String[]{rs.getString("answer1"), rs.getString("answer2"), rs.getString("answer3"), rs.getString("answer4")});
				q.setScore(rs.getString("score"));
				qArr.add(q);
			}
		} catch (SQLException e) {e.printStackTrace();}
		return qArr;
	}

	/**
	 * Uploads the computerized exam that was also checked by the system to the DB.
	 * @param arrayList
	 */
	public void uploadFinishedExam(ArrayList<String> arrayList)
	{
		try 
	    {
	    	PreparedStatement ps = conn.prepareStatement( "INSERT INTO student_exam (exam_id, student_id, " +
										    			  "grade, correct_answers, wrong_answer, isConfirmed) " +
										    			  "VALUES (?, ?, ?, ?, ?, ?)" );
	       ps.setString(1, arrayList.get(0));
	       ps.setString(2, arrayList.get(1));
	       ps.setInt(3, Integer.parseInt(arrayList.get(2)));
	       ps.setString(4, arrayList.get(3));
	       ps.setString(5, arrayList.get(4));
	       ps.setString(6, arrayList.get(5));
	       ps.executeUpdate();
	    } 
	    catch (SQLException e){e.printStackTrace();}
	}

	/**
	 * sets exam stats (date, time, etc..) in the DB.
	 * @param examStats
	 */
	public void addExamStats(ArrayList<String> examStats) 
	{
		try 
	    {
	    	PreparedStatement ps = conn.prepareStatement( "INSERT INTO exam_stats (exam_id, date, time, actual_time, " +
										    			  "students_total, students_completed, students_uncompleted) " +
										    			  "VALUES (?, ?, ?, ?, ?, ?, ?)");
	       ps.setString(1, examStats.get(0));
	       ps.setString(2, examStats.get(1));
	       ps.setInt(3, Integer.parseInt(examStats.get(2)));
	       ps.setInt(4, Integer.parseInt(examStats.get(3)));
	       ps.setInt(5, Integer.parseInt(examStats.get(4)));
	       ps.setInt(6, Integer.parseInt(examStats.get(5)));
	       ps.setInt(7, Integer.parseInt(examStats.get(6)));
	       ps.executeUpdate();
	    } 
	    catch (SQLException e){e.printStackTrace();}
	}

	/**
	 * @param string
	 * @return arraylist that contains an exam that needs to be checked.
	 */
	public ArrayList<ExamResults> getProfessorPendingExams(String string)
	{
		ArrayList<ExamResults> examResultsArr = new ArrayList<>();
		examResultsArr.add(new ExamResults("pending exams"));
		try 
		{
			PreparedStatement ps = conn.prepareStatement("SELECT e.exam_name, se.exam_id, se.student_id, se.grade, se.wrong_answer " +
														 "FROM exam e " +
														 "JOIN student_exam se ON e.exam_id = se.exam_id " +
														 "WHERE e.professor_id = ? " +
	                									 "AND se.isConfirmed = 0");
			ps.setString(1, string);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				ExamResults e = new ExamResults(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4));
				e.setWrong_answers(rs.getString(5));
				examResultsArr.add(e);
			}
		} catch (SQLException e) {e.printStackTrace();}
		return examResultsArr;
	}

	/**
	 * This method is responsible for updating a non approved exam after the professor approved it.
	 * @param examResult
	 */
	public void approveExamResult(ExamResults examResult) 
	{
		 try 
		    {
		        PreparedStatement ps = conn.prepareStatement("UPDATE student_exam SET grade = ?, comment = ?, isConfirmed = ? " +
		        									         "WHERE exam_id = ? AND student_id = ?");
		        ps.setInt(1, examResult.getGrade());
		        ps.setString(2, examResult.getComment());
		        ps.setString(3, examResult.getIsConfirmed());
		        ps.setString(4, examResult.getExam_id());
		        ps.setString(5, examResult.getStudent_id());
		        ps.executeUpdate();
		    }
		    catch (SQLException e) {e.printStackTrace();}
	}
	
	/**
	 * Sends an 'email' to a student.
	 * @param id
	 * @return
	 */
	public ArrayList<String> sendEmailToUser(String id)
	{
		ArrayList<String> emailToSend = new ArrayList<>();
		emailToSend.add("send email to user");
		try 
		{
			PreparedStatement ps = conn.prepareStatement("SELECT email FROM users WHERE user_id = ? ");
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				emailToSend.add(rs.getString(1));
			}
		} catch (SQLException e) {e.printStackTrace();}
		return emailToSend;
	}
	
	/**
	 * Uploads the request to add time to the DB.
	 * @param request
	 */
	public void requestTimeChange(ArrayList<String> request)
	{
		try 
	    {
	    	PreparedStatement ps = conn.prepareStatement( "INSERT INTO exam_time_request (exam_id, exam_time_request, " +
										    			  "isApproved, professor_id, professor_name, reason, exam_name) " +
										    			  "VALUES (?, ?, ?, ?, ?, ?, ?)" );
	       ps.setString(1, request.get(0));
	       ps.setInt(2, Integer.parseInt(request.get(1)));
	       ps.setString(3, request.get(2));
	       ps.setString(4, request.get(3));
	       ps.setString(5, request.get(4));
	       ps.setString(6, request.get(5));
	       ps.setString(7, request.get(6));
	       ps.executeUpdate();
	    } 
	    catch (SQLException e){e.printStackTrace();}
	}
	
	/**
	 * @return true or false depending on whether there's any request for the HOF.
	 */
	public boolean checkForTimeChangeRequests()
	{
		try 
		{
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT isApproved FROM exam_time_request WHERE isApproved <> -1");
			while (rs.next())
			{
				String isApprovedValue = rs.getString(1);
				if (isApprovedValue.equals("0"))
					return true;
			}
		} catch (SQLException e) {e.printStackTrace();}
		return false;
	}

	/**
	 * Loads from the DB all the relevant requests.
	 * @param id
	 * @return arraylist full of requests for time change
	 */
	public ArrayList<ExamTimeChange> loadPendingRequests(String id) 
	{
		ArrayList<ExamTimeChange> pendingRequestsArr = new ArrayList<>();
		try 
		{
			PreparedStatement ps = conn.prepareStatement("SELECT etr.* " +
									                     "FROM exam_time_request etr " +
									                     "WHERE etr.isApproved = 0 AND etr.professor_id IN (" +
									                     "SELECT pd.professor_id " +
									                     "FROM professor_department pd " +
									                     "WHERE pd.head_of_department_id = ?)");
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				ExamTimeChange e = new ExamTimeChange(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
													  rs.getString(6), rs.getString(7));
				pendingRequestsArr.add(e);
			}
		} catch (SQLException e) {e.printStackTrace();}
		return pendingRequestsArr;
	}
	
	/**
	 * This method approves or disapproves a time request with the given ID.
	 * @param id
	 * @param isApproved 
	 */
	public void updateApprovalStatus(String id, String isApproved) 
	{
		try 
		{
			PreparedStatement ps = conn.prepareStatement("UPDATE exam_time_request SET isApproved = ? WHERE exam_id = ?");
			ps.setString(1, isApproved);
			ps.setString(2, id);
			ps.executeUpdate();
		} catch (SQLException e) {e.printStackTrace();}
	}

	/**
	 * @param id
	 * @return  id of specific head of department
	 */
	public ArrayList<String> getHeadofDepId(String id) 
	{
		ArrayList<String> hofId = new ArrayList<>();
		hofId.add("head of dep id to set");
		try 
		{
			PreparedStatement ps = conn.prepareStatement("SELECT head_of_department_id FROM professor_department WHERE professor_id = ?");
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				hofId.add(rs.getString(1));
		} catch (SQLException e) {e.printStackTrace();}
		return hofId;
	}	
	/**
	 * This method add a new row to student_manual_exam table
	 * @param examID
	 * @param StudentID
	 */
	public void addStudentManualExam(String examID, String StudentID) 
	{
		try 
	    {
			PreparedStatement ps = conn.prepareStatement("INSERT INTO student_manual_exam (exam_id, student_id) VALUES (?, ?)");
			ps.setString(1, examID);
			ps.setString(2, StudentID);
			ps.executeUpdate();
	    } 
	    catch (SQLException e){e.printStackTrace();}
	}
	

	/**
	 * This method loads student's grades.
	 * @param StudentID
	 * @return
	 */
	public ArrayList<ExamResults> setStudentExamGrade(String StudentID) 
	{
		ArrayList<ExamResults> studentExamArr = new ArrayList<>();
		studentExamArr.add(new ExamResults("student grades"));
		try
	    {
			PreparedStatement ps = conn.prepareStatement("SELECT exam.exam_name, exam. exam_id, student_exam.grade, student_exam.comment " +
													     "FROM student_exam " +
													     "JOIN exam ON student_exam.exam_id = exam.exam_id " +
													     "WHERE student_exam.isConfirmed = 1 AND student_id = ?");
			ps.setString(1, StudentID);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				ExamResults e = new ExamResults(rs.getString(1), rs.getString(2), StudentID, rs.getInt(3));
				e.setComment(rs.getString((4)));
				studentExamArr.add(e);
			}	   
		} 
	    catch (SQLException e){e.printStackTrace();}
		return studentExamArr;
	}

	/**
	 * This method imports all the users manually into the DB.
	 */
	public void importExternalUsers()
	{
		try
		{
			Statement s = conn.createStatement();
			//This query inserts users into users table from an external DB table, and makes sure to not try to include existing users.
			s.executeUpdate("INSERT INTO users (user_id, first_name, last_name, email, username, password, role, isLogged) " +
					       "SELECT se.* " +
					       "FROM simulation_external_users AS se " +
					       "LEFT JOIN users AS u ON se.user_id = u.user_id " +
					       "WHERE u.user_id IS NULL;");
		}
		catch (SQLException e) {e.printStackTrace();}
	}
}
