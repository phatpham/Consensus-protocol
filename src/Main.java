import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {

	public static void main(String[] args) {
		int cport = 9091;
		int total = 3;
		CopyOnWriteArrayList<String> options = new CopyOnWriteArrayList<String>();
		options.add("A");
		options.add("B");
		int pport = 12337;
		int timeout = 2000;
		int failureFlag = 0;
		
		CopyOnWriteArrayList<Integer> portsConnected = new CopyOnWriteArrayList<Integer>();
		
		//Generate new threads to handle each client
		try {
			ServerSocket serverSocket = new ServerSocket(cport);
			
			Thread listener = new Thread(new ListenThread(cport, pport, timeout, failureFlag));
			listener.start();
			Socket serverClient1 = serverSocket.accept();
			new Thread(new CoordinatorThread(cport, total, options, serverClient1)).start();
			Thread.currentThread().sleep(1000);
			
			
			
			Thread listener2 = new Thread(new ListenThread(cport, pport+1, timeout, 0));
			listener2.start();
			Socket serverClient2 = serverSocket.accept();
			new Thread(new CoordinatorThread(cport, total, options, serverClient2)).start();
			Thread.currentThread().sleep(1000);

			Thread listener3 = new Thread(new ListenThread(cport, pport+2, timeout, 1));
			listener3.start();
			Socket serverClient3 = serverSocket.accept();
			new Thread(new CoordinatorThread(cport, total, options, serverClient3)).start();

			
		}catch(Exception e){
				System.out.println("error "+e);
			}
		}
}

