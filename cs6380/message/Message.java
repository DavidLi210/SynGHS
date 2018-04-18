package cs6380.message;

import java.io.Serializable;

public class Message implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7117404854166434002L;
	private int sender;
	private int receiver;
	private String type;
	private int round;
	private int[] edge = new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
	private Boolean sameComp;
	private int root_id;
	
	
	public int getRoot_id() {
		return root_id;
	}

	public void setRoot_id(int root_id) {
		this.root_id = root_id;
	}

	public Boolean getSameComp() {
		return sameComp;
	}

	public void setSameComp(Boolean sameComp) {
		this.sameComp = sameComp;
	}

	public int getRound() {
		return round;
	}

	public void setRound(int round) {
		this.round = round;
	}

	public int getSender() {
		return sender;
	}

	public void setSender(int sender) {
		this.sender = sender;
	}

	public int[] getEdge() {
		return edge;
	}
	// format of edge: [smaller nodeid, larger node id, edge weight]
	public void setEdge(int id1, int id2, int weight) {
		if (id1 < id2) {
			edge[0] = id1;
			edge[1] = id2;
			edge[2] = weight;
		} else {
			edge[0] = id2;
			edge[1] = id1;
			edge[2] = weight;
		}
	}

	public int getReceiver() {
		return receiver;
	}

	public void setReceiver(int receiver) {
		this.receiver = receiver;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Message() {
	}

	public Message(int counter, String type, int from, int to) {
		this.round = counter;
		this.type = type;
		this.sender = from;
		this.receiver = to;
	}

	@Override
	public String toString() {
		String str = edge == null ? ", null" : ", edge=" + edge[0] + "." +edge[1] + "." + edge[2];
		return "Message [sender=" + sender + ", receiver=" + receiver + ", type=" + type + ", round=" + round
				+ ", sameComp=" + sameComp + str + ", root_id=" + root_id + "]";
	}

}
