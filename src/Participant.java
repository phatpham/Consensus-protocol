import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Participant {


	public static void main(String[] args) {

		/*
		 * try { Thread listener = new Thread(new ListenThread(socket));
		 * listener.start();
		 * 
		 * } catch (IOException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */
		int pport = 12335;
		int cport = 9091;
		CopyOnWriteArrayList<Integer> others = new CopyOnWriteArrayList<Integer>();
		CopyOnWriteArrayList<String> options = new CopyOnWriteArrayList<String>();
		
		
		try {
			ServerSocket socket = new ServerSocket(pport);
			////Thread listener = new Thread(new ListenThread(cport, pport, others, options));
			//listener.start();
			
			while (true) { 
				try{ 
					Socket client = socket.accept(); 
					//new Thread(new ListenPeerThread(pport, client, others, options)).start(); 
				}catch(Exception e){ 
					System.out.println("error "+e); 
				} 
			}


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

class ListenThread implements Runnable {
	
	//Port that the client is listening on, and the server's port
	private int pport;
	private int cport;
	private  int timeout;
	private int failureFlag;
	private long start;
	private long end;
	
	//Keep a list of failed participants
	private static CopyOnWriteArrayList<Integer> failedParticipants = new CopyOnWriteArrayList<Integer>();
	
	private  ConcurrentHashMap<Integer, String> portVotes1 =new ConcurrentHashMap<Integer, String>();
	
	//All votes received
	private  ConcurrentHashMap<Integer, String> portVotes =new ConcurrentHashMap<Integer, String>();
	
	//All votes received last round
	private  ConcurrentHashMap<Integer, String> lastRoundVotes =new ConcurrentHashMap<Integer, String>();


	PrintWriter out = null;
	BufferedReader in = null;
	
	// List of other participants
	private CopyOnWriteArrayList<Integer> others = new CopyOnWriteArrayList<Integer>();

	// List of vote options
	private CopyOnWriteArrayList<String> options = new CopyOnWriteArrayList<String>();

	//Socket for connecting with the cordinator
	Socket csocket;

	//Socket that the participant is listening on
	Socket psocket;
	
	private static int x = 0;

	boolean firstRoundDone = false;

	public ListenThread(int cport, int pport, int timeout, int failureFlag) {
		this.pport = pport;
		this.cport = cport;
		this.timeout = timeout;
		this.failureFlag = failureFlag;
		
		try {
			csocket = new Socket("127.0.0.1", cport);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//Find other participants and populate possible options
	public void receiveBasicDetails(BufferedReader in) {
		boolean notDone = true;
		while (notDone) {
			// Read incoming message and update variables based on message received
			String line;
			try {
				while (in.ready()) {
					line = in.readLine();
					// If the line is DETAIL [ports]
					String[] lineList = line.split(" ");

					if (lineList[0].equals("DETAIL")) {
						for (int i = 1; i < lineList.length; i++) {
							others.add(Integer.parseInt(lineList[i]));
						}
						
					} else if (lineList[0].equals("VOTE_OPTIONS")) {
						for (int i = 1; i < lineList.length; i++) {
							options.add(lineList[i]);
						}
						notDone = false;
						break;
					}
					
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
	

	public void run() {
		
		try {
			
			//Set up read/print to server
			in = new BufferedReader(new InputStreamReader(csocket.getInputStream()));;
			out = new PrintWriter(csocket.getOutputStream());
			out.println("JOIN " + pport);
			out.flush();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		//Receive details from coordinator
		receiveBasicDetails(in);
		
		//Talk to other clients
		Socket newClient = null;
		
		Random rand = new Random(); 
		String randomVote = options.get(rand.nextInt(options.size()));
		
		new Thread(new Runnable() {	
		@Override
		public void run() {
			
			
			
			try {
				//Start listening on port
				ServerSocket participantSocket = new ServerSocket(pport);
				
				//Connect to other participants
				PrintWriter newOut = null;
				
				
				//Send the first round of votes (number of message based on failure flag)
				if (failureFlag == 0) {
					try {
						for (int i = 0; i < others.size();i++) {
							Socket newSocket = new Socket("127.0.0.1", others.get(i)); 
							newOut = new PrintWriter(newSocket.getOutputStream()); 
							newOut.println("VOTE " + pport + " " + randomVote ); 
							newOut.flush(); 
						}
					}
					catch (IOException e) { 
						System.out.println("socket is closed"); 
					}
				} 
				else if (failureFlag == 1) {
					/*
					int randomNumber = (int) Math.floor(Math.random() * others.size()) ;
					if (randomNumber == others.size()) {
						randomNumber = (int) Math.floor(Math.random() * (others.size()-1));
					}*/
					try {
						for (int i = 0; i < others.size()-1;i++) {
							Socket newSocket = new Socket("127.0.0.1", others.get(i)); 
							System.out.println("Vote sent to " + others.get(i));
							newOut = new PrintWriter(newSocket.getOutputStream()); 
							newOut.println("VOTE " + pport + " A" ); 
							newOut.flush();
							csocket.close(); 
						}
					}
					catch (IOException e) { 
						e.printStackTrace(); 
					}
				} 
				
				while (true) {

					Socket newClient = participantSocket.accept();
					new Thread(new ListenPeerThread(newClient, others, options)).start();		
					
				}
				
				
			} catch (IOException e) {
				System.out.println("socket is closed"); 
			}
			
		}

		}).start();
		
		
	
		
		
		//Send the votes receive from last round

		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true && failureFlag == 0) {
					if (x==(others.size()*(others.size()+1))) {
						try {
							for (int i = 0; i < others.size();i++) {
								if (!failedParticipants.contains(others.get(i))) {
									Socket newSocket = new Socket("127.0.0.1", others.get(i)); 
									PrintWriter newOut = new PrintWriter(newSocket.getOutputStream()); 
									String secondRoundMessage = "";
									for (Entry<Integer, String> pair: lastRoundVotes.entrySet()) {
										secondRoundMessage = secondRoundMessage + " " + String.valueOf(pair.getKey()) + " " + pair.getValue() ;
									}
									if (!secondRoundMessage.isEmpty()) {
										System.out.println("VOTE" + secondRoundMessage + " " + pport);
										newOut.println("VOTE" + secondRoundMessage); 
										newOut.flush(); 
									}
									lastRoundVotes.clear();
									
								}
							}
						}
						catch (Exception e) { 
							e.printStackTrace(); 
						}
						break;
					}
				}
				
			}

		}).start(); 
		
		if (failureFlag != 2) {
			while (true && failureFlag == 0) {
				ConcurrentHashMap<String, Integer> counter1 = null;
				if (portVotes.size() >= others.size()) {
					counter1 = new ConcurrentHashMap<String, Integer> (); 
					ConcurrentHashMap<Integer, String> allVotes = new ConcurrentHashMap<Integer, String>();
					allVotes.put(pport, randomVote);
					for (Entry<Integer, String> entry:portVotes.entrySet()) {
						if (!allVotes.containsKey(entry.getKey())) {
							allVotes.put(entry.getKey(), entry.getValue());
						}
					}
					for (Entry<Integer, String> entry:allVotes.entrySet()) {
					   String value = entry.getValue();
					   Integer count = counter1.get(value);
					   if (count == null)
						   counter1.put(value, new Integer(1));
					   else
						   counter1.put(value, new Integer(count+1));
					}
						
					//Find option with the highest vote
					int max = 0; 
					ArrayList<String> equal = new ArrayList<String>();
					for (Entry<String, Integer> pair: counter1.entrySet()) {
						if (max < (int) pair.getValue()) { 
							max = (int) pair.getValue(); 
							equal = new ArrayList<String>();
								equal.add( (String) pair.getKey()); 
						} else if (max == (int) pair.getValue()) { 
							equal.add( (String) pair.getKey()); 
						}
					}
					//Send message to coordinator 
					String message = String.valueOf(pport); 
					for (int i = 0; i < others.size() ;i++) { 
						message = message + " " + others.get(i); 
					}
					System.out.println(equal);
					if (equal.size() == 1) { 
						try {
							out.println("OUTCOME " + equal.get(0) + " " +message); 
							out.flush();
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (equal.size() > 1) { 
						out.println("OUTCOME null " + message); 
						out.flush();
					} 
					break;
				} 
			}
		} else {
			try {
				csocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	private class ListenPeerThread implements Runnable {
		
		// List of vote options
		CopyOnWriteArrayList<String> options;
		// List of other participants
		CopyOnWriteArrayList<Integer> others;

		Socket participantSocket;
		
		public ListenPeerThread(Socket socket, CopyOnWriteArrayList<Integer> others, CopyOnWriteArrayList<String> options) {
			this.participantSocket = socket;
			this.others = others;
			this.options = options;
		}
		
		
		
		
			@Override
		public void run() {

			//-----------------
			//Generate random votes 
			BufferedReader inFromParticipant = null;
			PrintWriter outToParticipant = null;
			try {
				inFromParticipant = new BufferedReader(new InputStreamReader(participantSocket.getInputStream()));;
				outToParticipant = new PrintWriter(participantSocket.getOutputStream());
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			start = System.currentTimeMillis();
			end = start + timeout;
			//Receive message
			while(start < end) {
				try {
					if(inFromParticipant.ready()) {
						String line = inFromParticipant.readLine();
						String[] voteList = line.split(" ");
						for (int i = 1; i < voteList.length; i = i+2) {
							if (!portVotes.contains(Integer.parseInt(voteList[i]))) {
								portVotes.put(Integer.parseInt(voteList[i]), voteList[i+1]); 
								lastRoundVotes.put(Integer.parseInt(voteList[i]), voteList[i+1]);
							}
						} 
						synchronized (this) {
							x++;
						}
						start = System.currentTimeMillis();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				start = System.currentTimeMillis();
			} 
			synchronized (this) {
				if (!(portVotes.size() == others.size())) {				
	;				//Finding the failed participant 
					for (Integer port: others) {
						if (!portVotes.containsKey(port) ) {
							if (failedParticipants.isEmpty()) {
								failedParticipants.add(port);
								System.out.println(failedParticipants);
								synchronized (this) {
									x++;
								}
							}
						}
					} 
				}	
			}

			firstRoundDone = true;
		
			
			
			
			try {
				participantSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
		}
	}
}
	


