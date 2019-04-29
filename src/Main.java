import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {

	public static void main(String[] args) {
		int cport = 9091;
		int total = 2;
		String[] options = new String[] {"A", "B"};
		int pport = 12337;
		int[] others = new int[20];
		String[] options2 = new String[20];
		CopyOnWriteArrayList<Integer> portsConnected = new CopyOnWriteArrayList<Integer>();
		
		//Generate new threads to handle each client
		try {
			ServerSocket serverSocket = new ServerSocket(cport);
			
			//ServerSocket socket = new ServerSocket(pport);
			//Socket client = socket.accept(); 
			Thread listener = new Thread(new ListenThread(cport, pport, others, options2));
			//Thread peerListener = new Thread(new ListenPeerThread(pport, client, others, options2));
			//peerListener.start();
			listener.start();
			Socket serverClient1 = serverSocket.accept();
			new Thread(new CoordinatorThread(cport, total, options, serverClient1, portsConnected)).start();

						
			//ServerSocket socket2 = new ServerSocket(pport+1);
			//Socket client2 = socket2.accept();
			Thread listener2 = new Thread(new ListenThread(cport, pport+1, others, options2));
			//Thread peerListener2 = new Thread(new ListenPeerThread(pport, client2, others, options2));
			//peerListener2.start();
			listener2.start();
			
			Socket serverClient2 = serverSocket.accept();
			new Thread(new CoordinatorThread(cport, total, options, serverClient2, portsConnected)).start();
			
			
		}catch(Exception e){
				System.out.println("error "+e);
			}
		}
}

