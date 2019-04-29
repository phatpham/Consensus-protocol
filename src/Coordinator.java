import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
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
					new Thread(new CoordinatorThread(cport, total, options, client, portsConnected)).start();
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
	
	private int pport;
	private int cport;
	private int expectedPorts;
	private volatile CopyOnWriteArrayList<Integer> portsConnected;
	
	Socket client;
	
	String[] options;
 	
	public CoordinatorThread(int cport, int total, String[] options, Socket c,  CopyOnWriteArrayList<Integer> portsConnected) {
		this.portsConnected = portsConnected;
		this.expectedPorts = total;
		this.cport = cport;
		this.options = options;
		this.client = c;
		this.portsConnected = portsConnected;
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
				
				
				// If not all participants join, abort?
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
		    	for (int i = 0; i < options.length ;i++) {
		    		message = message + " " + options[i];
		    	}
				out.println("VOTE_OPTIONS" + message);
				out.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			//Step 4
			try {
				
				//Step 1
				while(true) {
					String line;
					while(in.ready()) {
						line = in.readLine();
						String[] lineList = line.split(" ");
						if(lineList[0] == "OUTCOME") {
							System.out.println("YO IM HERE AND THE RESULT IS "+ lineList[1]);				
						} 
						
					}
					
					// If not all participants join, abort?
					
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

