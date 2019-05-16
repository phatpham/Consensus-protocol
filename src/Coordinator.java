import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Coordinator {
	
	public static void main(String[] args) {
		
		/*
		 * //Setting up int cport = Integer.parseInt(args[0]); int total =
		 * Integer.parseInt(args[1]); String[] options = new String[0];
		 * 
		 * 
		 * for (int x = 2; x < args.length ; x++) { options[x-2] = args[x]; }
		 */
		
		//Testing
		int cport = 9091;
		int total = 2;
		String[] options = new String[] {"A", "B"};
		CopyOnWriteArrayList<Integer> portsConnected = new CopyOnWriteArrayList<Integer>();
		
		//Generate new threads to handle each client
		try {
			ServerSocket socket = new ServerSocket(cport);
			for (;;) {
				try{
					Socket client = socket.accept();
					//new Thread(new CoordinatorThread(cport, total, options, client, portsConnected)).start();
				}catch(Exception e){
					System.out.println("error "+e);
				}
			}
		}catch(Exception e){
				System.out.println("error "+e);
			}
		}
		
}

class CoordinatorThread implements Runnable{
	
	private static volatile CopyOnWriteArrayList<String> messagesReceived;
	
	private int pport;
	private int cport;
	private static int expectedPorts;
	private static  CopyOnWriteArrayList<Integer> portsConnected = new CopyOnWriteArrayList<Integer>();
	private static ConcurrentHashMap<Integer, ArrayList<Integer>> outcome = new  ConcurrentHashMap<Integer, ArrayList<Integer>>();
	private String valuePort;
	Socket client;
	
	CopyOnWriteArrayList<String> options = new CopyOnWriteArrayList<String>();
 	
	public CoordinatorThread(int cport, int total, CopyOnWriteArrayList<String> options, Socket c) {
		this.expectedPorts = total;
		this.cport = cport;
		this.options = options;
		this.client = c;
	}
	
	
	
	@Override
	public void run() {
		PrintWriter out = null;
		BufferedReader in = null;
		try {
			out = new PrintWriter(client.getOutputStream());
			in = new BufferedReader( new InputStreamReader(client.getInputStream()));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
				
			//Step 1
			boolean notDone = true;
			while(notDone) {
				String line;

				while(in.ready()) {
					line = in.readLine();

					String[] lineList = line.split(" ");
					if(lineList[0].equals("JOIN")) {
						pport = Integer.parseInt(lineList[1]);
						portsConnected.add(Integer.parseInt(lineList[1]));	
					} 
					if (portsConnected.size() >= expectedPorts) {
						notDone = false;
						break;
					}
				}
				if (portsConnected.size() >= expectedPorts) {
					notDone = false;
				}
				
				
			}
	
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			//step 2
			try {
				String message = "";
		    	for (int i = 0; i < portsConnected.size() ;i++) {
		    		if(portsConnected.get(i) != pport)
		    			message = message + " " + String.valueOf(portsConnected.get(i));
		    	}
				out.println("DETAIL" + message);
				out.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			//step 3
			try {
				String message = "";
		    	for (int i = 0; i < options.size() ;i++) {
		    		message = message + " " + options.get(i);
		    	}
				out.println("VOTE_OPTIONS" + message);
				out.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		
			//Step 4
			try {
				
				while(true) {
					String line;
					if(in.ready()) {
						
						line = in.readLine();
						System.out.println(line);
						String[] lineList = line.split(" ");
						if(lineList[0].equals("OUTCOME")) {
							//Need change
							if (!outcome.containsKey(Integer.parseInt(lineList[2]))) {
								ArrayList<Integer> portsParticipated = new ArrayList<Integer>();
								for (int i = 2; i < lineList.length; i++) {
									portsParticipated.add(Integer.parseInt(lineList[i]));
								}
								outcome.put(Integer.parseInt(lineList[2]), portsParticipated);
								valuePort = lineList[1];
							} 
							
						} 
						
					}

					if (outcome.size() == expectedPorts) {
						System.out.println("YO IM HERE AND THE RESULT IS "+ outcome);		
						break;
					}
					
				}
	
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

