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
	int pport;
	int cport;
	static int timeout;
	int failureFlag;
	static ConcurrentHashMap<Integer, String> portVotes =new ConcurrentHashMap<Integer, String>();
	
	PrintWriter out = null;
	BufferedReader in = null;
	
	// List of other participants
	CopyOnWriteArrayList<Integer> others = new CopyOnWriteArrayList<Integer>();

	// List of vote options
	CopyOnWriteArrayList<String> options = new CopyOnWriteArrayList<String>();

	//Socket for connecting with the cordinator
	Socket csocket;

	//Socket that the participant is listening on
	Socket psocket;
	
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

		
		
		new Thread(new Runnable() {	
		@Override
		public void run() {
			try {
				//Start listening on port
				ServerSocket participantSocket = new ServerSocket(pport);
				
				//Connect to other participants
				for (int i = 0; i < others.size();i++) {
					Socket newSocket = new Socket("127.0.0.1", others.get(i)); 
				}
				
				while (true) {
					Socket newClient = participantSocket.accept();
					new Thread(new ListenPeerThread(out, pport, newClient, others, options)).start();		
					
				}
				
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		}).start();
		
		PrintWriter newOut = null;
		if (failureFlag == 0) {
			try {
				for (int i = 0; i < others.size();i++) {
					Socket newSocket = new Socket("127.0.0.1", others.get(i)); 
					newOut = new PrintWriter(newSocket.getOutputStream()); 
					newOut.println("VOTE " + others.get(i) + " A" ); 
					newOut.flush(); 
				}
			}
			catch (Exception e) { 
				e.printStackTrace(); 
			}
		} 
		else if (failureFlag == 1) {
			int randomNumber = (int) Math.floor(Math.random() * others.size()) ;
			if (randomNumber == others.size()) {
				randomNumber = (int) Math.floor(Math.random() * (others.size()-1));
			}
			try {
				for (int i = 0; i < others.size();i++) {
					Socket newSocket = new Socket("127.0.0.1", others.get(i)); 
					if (i < randomNumber) {
						newOut = new PrintWriter(newSocket.getOutputStream()); 
						newOut.println("VOTE " + others.get(i) + " A" ); 
						newOut.flush();
					} 
				}
			}
			catch (Exception e) { 
				e.printStackTrace(); 
			}
		} 
		/*
		while (true) {
			if (ListenPeerThread.currentRound == 2) {
				
			}
		}*/
		
		
	}
	
	
	
	static class ListenPeerThread implements Runnable {

		int pport;
		int numberOfRounds = 2;
		int currentRound = 0;
		
		// List of vote options
		CopyOnWriteArrayList<String> options;
		// List of other participants
		CopyOnWriteArrayList<Integer> others;

		Socket participantSocket;
		PrintWriter toServer;
		
		public ListenPeerThread(PrintWriter toServer, int pport, Socket socket, CopyOnWriteArrayList<Integer> others, CopyOnWriteArrayList<String> options) {
			this.toServer = toServer;
			this.pport = pport;
			this.participantSocket = socket;
			this.others = others;
			this.options = options;
		}
		
		public String generateDecision() {
			Random rand = new Random(); 
			int n = rand.nextInt(options.size()); 
			String randomVote = options.get(n);
			return randomVote;
		}
		
		
			@Override
		public void run() {

			//-----------------
			//Generate random votes 
			String randomVote = generateDecision();

			PrintWriter outToParticipant = null;
			BufferedReader inFromParticipant = null;
			
			try {
				inFromParticipant = new BufferedReader(new InputStreamReader(participantSocket.getInputStream()));;
				outToParticipant = new PrintWriter(participantSocket.getOutputStream());
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			
			HashMap<Integer, String> allReceivedPortVotes = new HashMap<Integer, String>();
			
		

			while(currentRound < numberOfRounds) {
				currentRound++;
				//Send
				if (currentRound == 1) {
					//Send vote to the client
					try {
						outToParticipant.println("VOTE " + pport + " " + randomVote); 
						outToParticipant.flush(); 
						
					}
					catch (Exception e) { 
						e.printStackTrace(); 
					}
					
				}
				else {
					
				}
				
				
				//Received 
				boolean checked = false;
				long start = System.currentTimeMillis();
				long end = start + timeout - 500;
				while (start < end) {
					if (currentRound == 1) {

						try {
							if(inFromParticipant.ready()) {
								String line = inFromParticipant.readLine();
								String[] voteList = line.split(" ");
								portVotes.put(Integer.parseInt(voteList[1]), voteList[2]); 
								start = System.currentTimeMillis();
								checked =true;
								start = end +1 ;
							}
							while (checked) {
								ConcurrentHashMap<String, Integer> counter1 = null;
								System.out.println(portVotes + " " + pport);
								if (portVotes.size() == others.size() + 1) {
									counter1 = new ConcurrentHashMap<String, Integer> (); 
									for (Entry<Integer, String> entry:portVotes.entrySet()) {
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
									  
									if (equal.size() == 1) { 
										try {
											toServer.println("OUTCOME " + equal.get(0) + " " +message); 
											toServer.flush();
											toServer.close();
	
										} catch (Exception e) {
											e.printStackTrace();
										}
	
									} else if (equal.size() > 1) { 
										toServer.println("OUTCOME null " + message); 
									}  
									checked = false;
								} 

							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						
					}
					//Else if nth (n>1) rounds
					else {
						try {
							if(inFromParticipant.ready()) {
								String line = inFromParticipant.readLine();
								String[] voteList = line.split(" ");
								portVotes.put(Integer.parseInt(voteList[1]), voteList[2]); 
								start = System.currentTimeMillis();
								checked =true;
								start = end +1 ;
							}	
						} catch (Exception e) {
							e.printStackTrace();
						}
							
					}
					
					start = System.currentTimeMillis();
				}
				
				currentRound++;
				
			}
			
			
			try {
				participantSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
		}
	}
}
	


