package server;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import entities.Exam;
import entities.ExamResults;
import entities.Question;
import entities.User;
import enums.MessageType;
import ocsf.server.ConnectionToClient;


/**
 * This class represents the handling of all the messages that the server receives from clients.
 */
public class ServerMessageHandler 
{
	static private MySQLController sqlController = MySQLController.getInstance(); 
	
	static private Map<String, ArrayList<ConnectionToClient>> roleClientMap = new HashMap<>();
	
	//0 - exam_id 1 - time 2 - actualTime
	static private String[] onGoingExamTime = {"0", "0", "0"};
	
	//0 - finishedExam, 1 - unFinishedExam, 2 - totalStudents, 3 - time to add
	static private Integer[] counterArray = {0, 0, 0, 0};

	/**
	 * Finds out the type of the message and then initiates the appropriate method.
	 * @param msg
	 * @param client
	 */
	@SuppressWarnings({ "unchecked", "incomplete-switch" })
	public static void messageHandler(Object msg, ConnectionToClient client)
	{
		MessageType message = getType(msg);
		if (message == null)
			return;
		switch (message) 
		{
			case STRING:
				stringMessageHandler((String) msg, client);
				break;
				
			case STRING_ARRAY_LIST:
				stringArrayListMessageHandler((ArrayList<String>) msg, client);
				break;
				
			case QUESTION:
				questionMessageHandler((Question)msg, client);
				break;
				
			case EXAM:
				examMessageHandler((Exam)msg, client);
				break;
				
			case EXAM_RESULTS:
				examResultsMessageHandler((ExamResults)msg, client);
				break;
		}
	}

	/**
	 * Determines the type of the client message.
	 * @param msg
	 * @return the message type.
	 */
	public static MessageType getType(Object msg) 
	{
		if (msg instanceof String)
			return MessageType.STRING;
		if (msg instanceof Question)
			return MessageType.QUESTION;
		if (msg instanceof ExamResults)
			return MessageType.EXAM_RESULTS;
		if (msg instanceof Exam)
			return MessageType.EXAM;
		else if (msg instanceof ArrayList)
		{
			ArrayList<?> arrayList = (ArrayList<?>) msg;
			if (!arrayList.isEmpty())
			{
				Object firstElement = arrayList.get(0);
				if (firstElement instanceof String)
					return MessageType.STRING_ARRAY_LIST;
				else if (firstElement instanceof Question)
					return MessageType.QUESTION_ARRAY_LIST;
				else if (firstElement instanceof ExamResults)
					return MessageType.EXAM_RESULTS_ARRAY_LIST;
			}
		}
		return null;
	}

	/**
	 * Handles client messages of type Strings.
	 * @param message
	 * @param client
	 */
	public static void stringMessageHandler(String message, ConnectionToClient client) 
	{
		try 
		{
			switch (message) 
			{
				case "connected":
					EchoServer.updateclientsInfoList(client, "Connected");
					EchoServer.serverScreenController.clientConnected();
					client.sendToClient("Connected");
					break;
					
				case "disconnected":
					EchoServer.updateclientsInfoList(client, "Disconnected");
					client.sendToClient("Disonnected");
					break;
					
				case "get amount of questions":
					client.sendToClient(sqlController.getAmountOfQuestions());
					break;
					
				case "get amount of exams":
					client.sendToClient(sqlController.getAmountOfExams());
					break;
					
				case "load student exams":
					client.sendToClient(sqlController.loadStudentExams());
					break;
					
				case "count":
					counterArray[2]++;
					client.sendToClient("student entered exam");
					break;
				
				case "check for pending requests":
					String msg = "no pending requests";
					if (sqlController.checkForTimeChangeRequests())
						msg = "You have pending time change requests";
					client.sendToClient(msg);
					break;
			}
		} catch (IOException e) {}
	}

	/**
	 * Handles client messages that are an array list with String elements.
	 * @param arrayList
	 * @param client
	 */
	public static void stringArrayListMessageHandler(ArrayList<String> arrayList, ConnectionToClient client) 
	{
		String messageType = arrayList.get(0);
		try 
		{
			switch (messageType) 
			{
				case "login": 		
					User user = sqlController.verifyLogin(arrayList);
					if (user == null) 
						client.sendToClient("incorrect login");
				    else if (user.getUser_id().equals("logged"))
						client.sendToClient("already logged");
					//Adds the client with his user-role to the roleClientMap.
				    else
				    {
				    	ArrayList<ConnectionToClient> clients = roleClientMap.get(user.getRole());
				    	if (clients == null) 
							clients = new ArrayList<>();
				    	clients.add(client);
				    	roleClientMap.put(user.getRole(), clients);
						client.sendToClient(user);
				    }
					break;
					
				case "logout":
					sqlController.logout(arrayList.get(1));
					client.sendToClient("logged out");
					logout(client);
					break;
					
				case "load professor questions":
					client.sendToClient(sqlController.loadProfessorQuestions(arrayList.get(1)));
					break;
					
				case "load all department questions":
					client.sendToClient(sqlController.loadHeadOfDepartmentQuestions(arrayList.get(1)));
					break;
				
				case "load professor exams":
					client.sendToClient(sqlController.loadProfessorExams(arrayList.get(1)));
					break;
					
				case "load exam questions":
					client.sendToClient(sqlController.examQuestion(arrayList.get(1)));
					break;
					
				case "update question courses":
					arrayList.remove(0);
					sqlController.addQuestionCourses(arrayList);
					client.sendToClient("updated question courses");
					break;
				
				case "update exam questions":
					arrayList.remove(0);
					sqlController.addExamQuestions(arrayList);
					client.sendToClient("added exam and questions");
					break;
				
				case "update question":
					arrayList.remove(0);
					sqlController.editQuestionInDb(arrayList);
					client.sendToClient("updated question");
					break;
					
				case "delete question":
					if(sqlController.deleteQuestionFromDb(arrayList.get(1)))
						client.sendToClient("deleted question");
					else
						client.sendToClient("question in use");
					break;
					
				case "load teaching map":
					client.sendToClient(sqlController.getProfessorSubjectsAndCourses(arrayList.get(1)));
					break;
				
				case "load course questions":
					client.sendToClient(sqlController.loadCourseQuestions(arrayList.get(1)));
					break;
					
				case "load student courses":
					client.sendToClient(sqlController.getStudentCourses());
					break;
				
				//Info about indexes is next to the declaration.
				case "load exam file":
					counterArray[2]++;
					client.sendToClient(sqlController.openExamFile(arrayList.get(1)));
					break;
					
				case "upload exam":
					sqlController.uploadExamFile(arrayList.get(1), arrayList.get(2), arrayList.get(3));
					client.sendToClient("exam uploaded");
					break;
					
				case "for head of department report":
						if(arrayList.get(1).equals("student"))
							client.sendToClient(sqlController.getAllStudentExams(arrayList));
						else if(arrayList.get(1).equals("professor")) {
							arrayList.add("ex.professor_id");
							client.sendToClient(sqlController.getAllprofessor_courseExams(arrayList));
						}
						else {	
							arrayList.add("ex.course_id");
							client.sendToClient(sqlController.getAllprofessor_courseExams(arrayList));
						}
					break;
					
				//Info about indexes is next to the declaration.
				case "finished exam":
					updateStats(0, 2, arrayList, 7, 8);
					arrayList.remove(0);
					sqlController.uploadFinishedExam(arrayList);
					handleFinishedExam(client);
					client.sendToClient("exam submitted");
					break;
					
				case "unfinished exam":
					updateStats(1, 2, arrayList, 7, 8);
					arrayList.remove(0);
					sqlController.uploadFinishedExam(arrayList);
					handleFinishedExam(client);
					client.sendToClient("exam submitted");
					break;
					
				//Info about indexes is next to the declaration.
				case "finished manual exam":
					updateStats(0, 2, arrayList, 2, 3);
					handleFinishedExam(client);
					client.sendToClient("exam submitted");
					break;
					
				case "unfinished manual exam":
					updateStats(1, 2, arrayList, 2, 3);
					handleFinishedExam(client);
					client.sendToClient("exam submitted");
					break;
					
				//Adds new row in student_manual_exam table.
				case "add student manual exam":
					sqlController.addStudentManualExam(arrayList.get(1), arrayList.get(2));
					client.sendToClient("");
					break;
					
				case "activate":
					sqlController.updateExamStatus(arrayList.get(1), true);
					client.sendToClient("selected exam is now active");
					break;
					
				case "deactivate":
					emptyCounters();
					sqlController.updateExamStatus(arrayList.get(1), false);
					ArrayList<String> answer = new ArrayList<>();
					answer.add("selected exam is now inactive");
					//exam ID
					answer.add(arrayList.get(1));
					//Specific message for the professor.
					client.sendToClient(answer.get(0));
					if (roleClientMap.get("student") != null)
					{
						//A message for each client of role "student".
						for (ConnectionToClient c : roleClientMap.get("student"))
							c.sendToClient(answer);
					}
					break;
				
				case "load pending exams":
					client.sendToClient(sqlController.getProfessorPendingExams(arrayList.get(1)));
					break;
					
				case "Get all students":			
					client.sendToClient(sqlController.getAllStudents("student"));
					break;
					
				case "Get all professors":			
					client.sendToClient(sqlController.getAllProffesors(arrayList.get(1)));
					break;
					
				case "Get all courses":			
					client.sendToClient(sqlController.getAllCourses(arrayList.get(1)));
					break;
					
				case "send all exams to professor":
					client.sendToClient(sqlController.loadProfessorExams_toReport(arrayList.get(1), arrayList.remove(2)));
					break;
					
				case "generate full exam report":
					try 
					{
						client.sendToClient(sqlController.professorExamStat(arrayList.get(1)));
					}catch(Exception e) {e.printStackTrace();}
					break;
					
				case "request to add time":
					arrayList.remove(0);
					sqlController.requestTimeChange(arrayList);
					client.sendToClient("request sent");
					ArrayList<String> messageToHead = new ArrayList<>();
					messageToHead.add("send immediate approval to head of department");
					messageToHead.add(arrayList.get(7));
					if (roleClientMap.get("head") != null)
					{
						//A message for each client of role "head".
						for (ConnectionToClient c : roleClientMap.get("head"))
							c.sendToClient(messageToHead);
					}
					break;
					
				case "load pending requests":
					client.sendToClient(sqlController.loadPendingRequests(arrayList.get(1)));
					break;
					
				case "request answered":
					ArrayList<String> message = sqlController.sendEmailToUser(arrayList.get(3));
					sqlController.updateApprovalStatus(arrayList.get(2), arrayList.get(1));
					if (arrayList.get(1).equals("-1"))
					{
						message.add("Request disapproved - email sent to: ");
						client.sendToClient(message);
					}
					else 
					{
						message.add("Request approved - email sent to: ");
						client.sendToClient(message);
						ArrayList<String> newTime = new ArrayList<>();
						newTime.add("exam time is now changed");
						//exam id
						newTime.add(arrayList.get(2));
						//new time
						newTime.add(arrayList.get(4));
						if (roleClientMap.get("student") != null)
						{
							//A message for each client of role "student".
							for (ConnectionToClient c : roleClientMap.get("student"))
								c.sendToClient(newTime);
						}
					}
					break;
					
				case "get head of dep id":
					client.sendToClient(sqlController.getHeadofDepId(arrayList.get(1)));
					break;
					
				case "load student grades":
					client.sendToClient(sqlController.setStudentExamGrade(arrayList.get(1)));
					break;
			}
		} catch (IOException e) {}
	}
	
	/**
	 * Handles client messages of type Question.
	 * @param question
	 * @param client 
	 */
	public static void questionMessageHandler(Question question, ConnectionToClient client) 
	{
		try 
		{
			sqlController.addQuestionToDB(question);
			client.sendToClient("question added");
		} catch (IOException e) {}
	}
	
	/**
	 * Handles messages of type Exam.
	 * @param exam
	 * @param client
	 */
	private static void examMessageHandler(Exam exam, ConnectionToClient client) 
	{
		try 
		{
			sqlController.addExamToDB(exam);
			client.sendToClient("exam added");
		} catch (IOException e) {}
	}
	
	/**
	 * Handles messages of type ExamResults
	 * @param examResult
	 * @param client
	 */
	private static void examResultsMessageHandler(ExamResults examResult, ConnectionToClient client) 
	{
		try 
		{
			sqlController.approveExamResult(examResult);
			ArrayList<String> message = sqlController.sendEmailToUser(examResult.getStudent_id());
			message.add("Exam approved - email sent to: ");
			client.sendToClient(message);
		} catch (IOException e) {}
	}
	
	/**
	 * Removes the client from clientRoleList after the client logs out
	 * so a message won't be sent to a "logged out" client when handling
	 * time changes or exam shut-downs.
	 * @param client
	 */
	public static void logout(ConnectionToClient client) 
	{
	    for (String key : roleClientMap.keySet()) 
	    {
	        ArrayList<ConnectionToClient> clients = roleClientMap.get(key);
	        if (clients != null) 
	        	if (clients.contains(client))
					roleClientMap.get(key).remove(client);
	    }
	}
	
	/**
	 * Resets the map AND the counters when the server disconnects. 
	 */
	public static void clearServerVars()
	{
		roleClientMap = new HashMap<>();
		emptyCounters();
	}
	
	/**
	 * Resets the counters when the server disconnects. 
	 */
	public static void emptyCounters()
	{
		Arrays.fill(onGoingExamTime, "0");
		Arrays.fill(counterArray, 0);
	}
	
	/**
	 * Updates all the information about the exam
	 * @param statusExam
	 * @param studentCounter
	 * @param arrayList
	 * @param time
	 * @param actualTime
	 */
	public static void updateStats(int statusExam, int studentCounter, 
			ArrayList<String> arrayList, int time, int actualTime)
	{
		try
		{
			counterArray[statusExam]++;
			counterArray[studentCounter]--;
			onGoingExamTime[0] = arrayList.get(1);
			onGoingExamTime[1] = arrayList.get(time);
			onGoingExamTime[2] = String.valueOf(Math.max(
					Integer.parseInt(onGoingExamTime[2]), Integer.parseInt(arrayList.get(actualTime)))); 
		}
		catch(Exception e) {e.printStackTrace();}
	}
	
	/**
	 * Checking if the exam is finished, if it was - updating its statistics in the DB.
	 * @param client
	 */
	public static void handleFinishedExam(ConnectionToClient client)
	{
		//Only if the exam is finished (all student left), the queries are activated.
		if (counterArray[2] == 0)
		{
			sqlController.updateExamStatus(onGoingExamTime[0], false);
			ArrayList<String> examStats = new ArrayList<>();
			examStats.add(onGoingExamTime[0]);
			LocalDate currentDate = LocalDate.now();
		    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		    String dateString = currentDate.format(formatter);
		    examStats.add(dateString);
		    //Exam time
		    examStats.add(String.valueOf(onGoingExamTime[1]));
		    //Actual time
		    examStats.add(String.valueOf(onGoingExamTime[2]));
		    //Total amount of students
		    examStats.add(String.valueOf(counterArray[0] + counterArray[1]));
		    //Amount of students that finished the exam
		    examStats.add(String.valueOf(counterArray[0]));
		    //Amount of students that didn't finish the exam
		    examStats.add(String.valueOf(counterArray[1]));
		    sqlController.addExamStats(examStats);
		    emptyCounters();
		}
	}
}
