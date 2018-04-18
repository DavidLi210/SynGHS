package cs6380.node;

import java.io.IOException;
import java.io.ObjectInputStream;

import cs6380.message.Message;

public class NodeListener implements Runnable {
	private Node node;
	private ObjectInputStream ois;
	public NodeListener(Node node, ObjectInputStream ois) {
		this.node = node;
		this.ois = ois;
	}
	
	@Override
	public void run() {
		Message message = null;
		try {
			while((message = (Message) ois.readObject()) != null) {
				node.process_message(message);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {

	}

}
