package cs6380.algo;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cs6380.message.Message;
import cs6380.node.Node;

public class SynGHS {
	private Node node;
	// set used to check whether receive all new_leader ack message
	private Set<Integer> ack_set;
	// set used to check whether receive all test_reply message
	private Set<Integer> test_set;
	private Set<Message> buffered_messages;
	public Set<Integer> getAck_set() {
		return ack_set;
	}
	
	// buffer merge message
	public Set<Message> getBuffered_messages() {
		return buffered_messages;
	}

	public Set<Integer> getTest_set() {
		return test_set;
	}

	public SynGHS() {
		test_set = Collections.synchronizedSet(new HashSet<>());
		ack_set = Collections.synchronizedSet(new HashSet<>());
		buffered_messages = Collections.synchronizedSet(new HashSet<>());
	}
	
	public static void main(String[] args) {

	}
	
}
