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
	
	public ListenThread(int cport, int pport) {
		this.pport = pport;
		this.cport = cport;

		
		try {
			csocket = new Socket("127.0.0.1", cport);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {

	
		try {
			in = new BufferedReader(new InputStreamReader(csocket.getInputStream()));;
			out = new PrintWriter(csocket.getOutputStream());
			out.println("JOIN " + pport);
			out.flush();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
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
		//Step 4
		try {
			ServerSocket participantSocket = new ServerSocket(pport);
			new Thread(new Runnable() {

				@Override
				public void run() {
					Socket newClient;
					try {
						newClient = participantSocket.accept();
						new Thread(new ListenPeerThread(out, pport, newClient, others, options)).start();;					
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
			}).start();;
			
			//-----------------
			//Generate random votes 
			Random rand = new Random(); 
			int n = rand.nextInt(options.size()); 
			String randomVote = "A";
			
			//Send votes
			try {
				PrintWriter newOut;
				for (int i = 0; i < others.size();i++) {
					Socket newSocket = new Socket("127.0.0.1", others.get(i)); 
					newOut = new PrintWriter(newSocket.getOutputStream()); 
					newOut.println("VOTE " + others.get(i) + " " + randomVote); 
					newOut.flush(); 
					newOut.close();
				}
			}
			catch (Exception e) { 
				e.printStackTrace(); 
			}

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		/*
		 * //Wait for votes while(true) { try { this.wait(); } catch
		 * (InterruptedException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } }
		 */
		
	}
	
	
	class ListenPeerThread implements Runnable {

		int pport;
		
		// List of vote options
		CopyOnWriteArrayList<String> options;
		// List of other participants
		CopyOnWriteArrayList<Integer> others;
		
		Socket psocket;
		PrintWriter toServer;
		
		public ListenPeerThread(PrintWriter toServer, int pport, Socket socket, CopyOnWriteArrayList<Integer> others, CopyOnWriteArrayList<String> options) {
			this.toServer = toServer;
			this.pport = pport;
			this.psocket = socket;
			this.others = others;
			this.options = options;
		}
		
			@Override
		public void run() {

			PrintWriter out = null;
			BufferedReader in = null;
			try {
				in = new BufferedReader(new InputStreamReader(psocket.getInputStream()));;
				out = new PrintWriter(psocket.getOutputStream());
			} catch (IOException e2) {
				e2.printStackTrace();
			}
				
			
			//Proceed to step 4 if all info is received
			int counter = 0; 
			boolean cont = true; 
			HashMap<Integer, String> portVotes =new HashMap<Integer, String>();
				  
			while (cont) {
			
				if (counter == 0) { 
				  
					
					  
					//Receive votes 
					try { 
						String line; 
						while(in.ready()) {
							line = in.readLine();
							String[] voteList = line.split(" ");
							portVotes.put(Integer.parseInt(voteList[1]), voteList[2]); 
						} 
					} 
						catch (IOException e) { 
						e.printStackTrace(); 
					}
					  
					//If no failure occurs 
					ConcurrentHashMap<String, Integer> counter1 = null;
					if (portVotes.size() == others.size()) {
						counter1 = new ConcurrentHashMap<String, Integer> (); 
						for (Entry<Integer, String> entry:portVotes.entrySet()) {
							   String value = entry.getValue();
							   Integer count = counter1.get(value);
							   if (count == null)
								   counter1.put(value, new Integer(1));
							   else
								   counter1.put(value, new Integer(count+1));
							}
					
						//Find option with the highest vote Iterator it2 
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
						System.out.println(equal);  
						//Send message to coordinator 
						String message = String.valueOf(pport); 
						for (int i = 0; i < others.size() ;i++) { 
							message = message + " " + others.get(i); 
						}
						  
						if (equal.size() == 1) { 
							System.out.println("OUTCOME " + equal.get(0) + " " +message);
							try {
								toServer.println("OUTCOME " + equal.get(0) + " " +message); 
								toServer.flush();
								toServer.close();
								cont = false;

							} catch (Exception e) {
								e.printStackTrace();
							}

						} else if (equal.size() > 1) { 
							out.println("OUTCOME null " + message); 
						}  
					
						  
					} else {
					  
					}
				}
			}   
		}
	}
}
	


