import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {

	public static void main(String[] args) {
		int cport = 9091;
		int total = 2;
		CopyOnWriteArrayList<String> options = new CopyOnWriteArrayList<String>();
		options.add("A");
		options.add("B");
		int pport = 12337;
		
		CopyOnWriteArrayList<Integer> portsConnected = new CopyOnWriteArrayList<Integer>();
		
		//Generate new threads to handle each client
		try {
			ServerSocket serverSocket = new ServerSocket(cport);
			
			Thread listener = new Thread(new ListenThread(cport, pport));
			listener.start();
			Socket serverClient1 = serverSocket.accept();
			new Thread(new CoordinatorThread(cport, total, options, serverClient1, portsConnected)).start();

			
			
			
			Thread listener2 = new Thread(new ListenThread(cport, pport+1));
			listener2.start();
			Socket serverClient2 = serverSocket.accept();
			new Thread(new CoordinatorThread(cport, total, options, serverClient2, portsConnected)).start();
			
			
		}catch(Exception e){
				System.out.println("error "+e);
			}
		}
}

