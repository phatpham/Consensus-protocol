import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

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
		int[] others = new int[20];
		String[] options = new String[20];
		
		
		try {
			ServerSocket socket = new ServerSocket(pport);
			Thread listener = new Thread(new ListenThread(cport, pport, others, options));
			listener.start();
			
			while (true) { 
				try{ 
					Socket client = socket.accept(); 
					new Thread(new ListenPeerThread(pport, client, others, options)).start(); 
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

	// List of other participants
	int[] others;

	// List of vote options
	String[] options;

	Socket csocket;

	public ListenThread(int cport, int pport, int[] others, String[] options) {
		this.pport = pport;
		this.cport = cport;
		this.others = others;
		this.options = options;
				
		try {
			csocket = new Socket("127.0.0.1", cport);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {

		PrintWriter out = null;
		BufferedReader in = null;
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
					System.out.println(Arrays.toString(lineList));
					if (lineList[0].equals("DETAIL")) {
						for (int i = 1; i < lineList.length; i++) {
							others[i - 1] = Integer.parseInt(lineList[i]);
						}
						
					} else if (lineList[0].equals("VOTE_OPTIONS")) {
						for (int i = 1; i < lineList.length; i++) {
							options[i - 1] = lineList[i];
						}
						notDone = false;
						break;
					}
					
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}


		/*
		 * //Wait for votes while(true) { try { this.wait(); } catch
		 * (InterruptedException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } }
		 */
		
	}
}
class ListenPeerThread implements Runnable {

	int pport;

	// List of other participants
	int[] others;
		// List of vote options
	String[] options;
	Socket psocket;
	
	public ListenPeerThread(int pport, Socket socket, int[] others, String[] options) {
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
			
		
		while (true) { //Proceed to step 4 if all info is received
			System.out.println("sss");
			int counter = 0; 
			boolean cont = true; 
			HashMap<Integer, String> portVotes =new HashMap<Integer, String>();
			  
			//Generate random votes 
			Random rand = new Random(); 
			int n = rand.nextInt(options.length); 
			String randomVote = options[n];
			
			Socket newSocket; 
			PrintWriter newOut;

			while (cont) {
			
				if (counter == 0) { 
				  
					//Send votes to every other participants 
					for(int i = 0; i < others.length;i++) { 
						try { 
							newSocket = new Socket("127.0.0.1", others[i]); 
							newOut = new PrintWriter(newSocket.getOutputStream()); 
							newOut.println("VOTE " + others[i] + " " + randomVote); 
							newOut.flush(); 
							newOut.close();
						}
						catch (Exception e) { // TODO Auto-generated catch block 
							 e.printStackTrace(); 
						}
					  
					}
					  
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
					  
					//If no failure occurs if (portVotes.size() == others.length) {
					ConcurrentHashMap<Integer, String> counter1 = new ConcurrentHashMap<Integer, String> (); 
					Iterator it = portVotes.entrySet().iterator(); 
					while (it.hasNext()) { 
						Map.Entry pair = (Map.Entry) it.next(); 
						counter1.put((Integer) (pair.getValue()) + 1, (String) pair.getKey()); 
						it.remove(); 
					}
					  
					//Find option with the highest vote Iterator it2 
					int max = 0; 
					String[] equal = new String[0];
					Iterator it2 = counter1.entrySet().iterator(); 
					while (it2.hasNext()) {
						Map.Entry pair = (Map.Entry)it.next(); 
						if (max < (int) pair.getKey()) { 
							max = (int) pair.getKey(); 
							equal = new String[0];
							equal[equal.length] = (String) pair.getValue(); 
						} else if (max == (int) pair.getKey()) { 
							equal[equal.length] = (String) pair.getValue(); 
						}
						it.remove(); 
					}
					  
					//Send message to coordinator 
					String message = String.valueOf(pport); 
					for (int i = 0; i < others.length ;i++) { 
						message = message + " " + others[i]; 
					}
					  
					if (equal.length == 1) { 
						out.println("OUTCOME" + counter1.get(max) + " " +message); 
					} else if (equal.length > 1) { 
						out.println("OUTCOME null " + message); 
					}  
				
				  
				} else {
				  
				} 
			}   
		}
	}
}

