package cs6380.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cs6380.algo.SynGHS;
import cs6380.message.Message;
import cs6380.message.MsgType;
import cs6380.util.NodeLookup;
import cs6380.util.NodeUtil;

public class Node {
	private ServerSocket myServer;
	private final int id;
	private int port;
	private String ip;
	private int message_req_time = 0;
	// store oos to all neighbors
	private Map<Integer, ObjectOutputStream> outgoingMap;
	private Map<Integer, ObjectInputStream> ingoingMap;
	private NodeLookup lookup;
	private Map<Integer, Integer> outgoing_edges;
	private Map<Integer, Integer> tree_edges;
	// other edges == edges pointing to nodes in same component while not in tree
	private Map<Integer, Integer> other_edges;
	private static final String configAddr = "config2.txt";
	private int root;
	private int round;
	private Integer parent;
	private SynGHS algo;
	// minimal edge length
	private int min = Integer.MAX_VALUE;
	// edge corresponding to min
	private int[] mwoe = new int[] { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };
	// oos to send message to itself
	private ObjectOutputStream self_sender;
	// ois used to only receive message from itself
	private ObjectInputStream self_receiver;
	// check whether receive all test_reply from neighbors, only if this is true can
	// we start to process merge message
	private boolean receive_all_test = false;

	public Node(String id) {
		this.id = Integer.parseInt(id);
		this.root = this.id;
		lookup = new NodeLookup();
		outgoing_edges = Collections
				.synchronizedMap(NodeUtil.readConfig(configAddr, lookup.getId_to_addr(), lookup.getId_to_index(), id));
		tree_edges = Collections.synchronizedMap(new HashMap<>());
		other_edges = Collections.synchronizedMap(new HashMap<>());
		port = Integer.parseInt(lookup.getPort(this.id));
		ip = lookup.getIP(this.id);
		ingoingMap = Collections.synchronizedMap(new HashMap<>());
		outgoingMap = Collections.synchronizedMap(new HashMap<>());
		round = 0;
	}

	public int getRound() {
		return round;
	}

	public void setRound(int round) {
		this.round = round;
	}

	public Map<Integer, ObjectOutputStream> getOutgoingMap() {
		return outgoingMap;
	}

	public Map<Integer, ObjectInputStream> getIngoingMap() {
		return ingoingMap;
	}

	private void init() {
		algo = new SynGHS();

		for (int neighbor : outgoing_edges.keySet()) {
			new Thread(new Runnable() {
				// try to connect to neighbors and store oos in hashmap
				@Override
				public void run() {
					final int nid = neighbor;
					String nip = lookup.getIP(nid);
					int nport = Integer.parseInt(lookup.getPort(nid));
					System.out.println(nip + " " + nport);
					boolean success = false;
					while (!success) {
						try {
							Socket socket = new Socket();
							socket.connect(new InetSocketAddress(nip, nport), 2 * 1000);
							ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
							Message message = new Message();
							message.setReceiver(nid);
							message.setRound(round);
							message.setSender(id);
							message.setType(MsgType.LOGIN);
							oos.writeObject(message);
							outgoingMap.put(nid, oos);
							success = true;
						} catch (UnknownHostException e) {
							e.printStackTrace();
						} catch (IOException e) {
							System.out.println("Please Wait For Other Nodes To Be Started");
							success = false;
							try {
								Thread.sleep(500);
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
						}
					}
				}
			}).start();
		}

		new Thread(new Runnable() {
			// start a oos to connect to itself
			@Override
			public void run() {
				boolean success = false;
				while (!success) {
					try {
						Socket socket = new Socket();
						socket.connect(new InetSocketAddress("127.0.0.1", port), 2 * 1000);
						self_sender = new ObjectOutputStream(socket.getOutputStream());
						Message m = generate_message(id, MsgType.ACK, round, null, null);
						self_sender.writeObject(m);
						success = true;
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						System.out.println("Please Wait Your Own Server To Be Started");
						success = false;
						try {
							Thread.sleep(500);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		}).start();

		try {
			myServer = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// wait until setup an ois to receive message from itself
		while (self_receiver == null) {
			try {
				Socket socket = myServer.accept();
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				Message login = (Message) ois.readObject();
				if (login.getType().equals(MsgType.ACK)) {
					self_receiver = ois;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		// wait until receive all connection from neighbors
		while (ingoingMap.size() < outgoing_edges.size()) {
			try {
				Socket socket = myServer.accept();
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				Message login = (Message) ois.readObject();
				int sender = login.getSender();
				ingoingMap.put(sender, ois);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		while (ingoingMap.size() < outgoing_edges.size() || outgoingMap.size() < outgoing_edges.size()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// start to listen and ready to receive messages
		listenToSelf();
		listenToNeighbors();
		System.out.println(id + " sets up correctly");
	}

	private void listenToSelf() {
		new Thread(new NodeListener(this, self_receiver)).start();
	}

	private void listenToNeighbors() {
		for (int neighbor : ingoingMap.keySet()) {
			new Thread(new NodeListener(this, ingoingMap.get(neighbor))).start();
		}
	}

	/**
	 * broadcast message on tree with given arguments
	 * 
	 * @param type
	 *            of message
	 * @param edge
	 *            that will be sent in message
	 * @param except
	 *            send to all neighbors on tree except this
	 * @param root_id
	 *            root_id contained in this message
	 */
	public synchronized void broadcast_on_tree(String type, int[] edge, Integer except, Integer root_id) {
		for (int neighbor : tree_edges.keySet()) {
			if (except == null || (except != null && neighbor != except)) {
				Message m = generate_message(neighbor, type, round, edge, root_id);
				private_message(m);
			}
		}
	}

	/**
	 * broadcast message on all nontree edges with given arguments
	 * 
	 * @param type
	 *            type of message
	 * @param edge
	 *            that will be sent in message
	 * @param root_id
	 *            root_id root_id contained in this message
	 */
	public synchronized void broadcast_on_nontree(String type, int[] edge, Integer root_id) {
		for (int neighbor : outgoing_edges.keySet()) {
			Message m = generate_message(neighbor, type, round, edge, root_id);
			private_message(m);
		}

		for (int neighbor : other_edges.keySet()) {
			Message m = generate_message(neighbor, type, round, edge, root_id);
			private_message(m);
		}
	}

	public synchronized void process_message(Message message) {
		message_req_time++;
		System.out.println("message received " + message + " at " + message_req_time);
		String realClazz = message.getClass().getSimpleName();
		if (realClazz.equals(Message.class.getSimpleName())) {
			// SEARCH MESSAGE
			if (message.getType().equals(MsgType.SEARCH)) {
				receive_all_test = false;
				// on receiving a search, node setup it's parent pointer unless it's root node
				if (root == id) {
					// System.out.println("Tree Structure : " + tree_edges);
					parent = null;
					System.out.println("parent becomes null at " + message_req_time);
				} else {
					parent = message.getSender();
				}
				// in this case, this node is the leaf node without any neighbors except parent, so we just pass
				if (tree_edges.size() == 1 && outgoing_edges.size() == 0 && other_edges.size() == 0 && parent != null
						&& id != root) {
					Message ack = generate_message(parent, MsgType.TEST_REPLY, round, null, null);
					private_message(ack);
				} else {
					// broadcast search message to other nodes on tree except who sent this message
					// to this node
					broadcast_on_tree(MsgType.SEARCH, null, message.getSender(), null);
					// broadcast test on non tree edges
					broadcast_on_nontree(MsgType.TEST, null, root);
				}
			} else if (message.getType().equals(MsgType.ADD_MWOE)) {
				int[] edge = message.getEdge();
				// leaf node which has the mwoe
				if (edge[0] == id || id == edge[1]) {
					int other_id = edge[0] == id ? edge[1] : edge[0];
					if (outgoing_edges.containsKey(other_id)) {
						int other_weight = outgoing_edges.remove(other_id);
						tree_edges.put(other_id, other_weight);
						// it means this is a common mwoe between 2 nodes
						Message merge = generate_message(other_id, MsgType.MERGE, round, edge, null);
						private_message(merge);
					} else if (tree_edges.containsKey(other_id)) {

						Message merge = generate_message(other_id, MsgType.MERGE, round, edge, null);
						private_message(merge);
						int max = Math.max(edge[0], edge[1]);
						if (max == id) {
							round++;
							root = id;
							// wait 10 seconds to allow other nodes have completed merge operations
							int sleep = 20;
							while (sleep > 0) {
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								if (sleep == 20) {
									System.out.println("NewLeader in " + sleep + " seconds");
								}
								if (sleep == 15) {
									System.out.println("NewLeader in " + sleep + " seconds");
								}

								if (sleep == 10) {
									System.out.println("NewLeader in " + sleep + " seconds");
								}
								if (sleep == 5) {
									System.out.println("NewLeader in " + sleep + " seconds");
								}
								sleep--;
							}

							broadcast_on_tree(MsgType.NEW_LEADER, null, null, root);
							parent = null;
							System.out.println("parent becomes null at " + message_req_time);
						}
					}
				} else {
					// replay add_mwoe to other nodes in tree
					broadcast_on_tree(MsgType.ADD_MWOE, edge, message.getSender(), null);
				}
			} else if (message.getType().equals(MsgType.MERGE)) {
				algo.getBuffered_messages().add(message);
				// only if this node has replied to its parent, then we can process buffered
				// merge message
				if (receive_all_test) {
					process_buffered_merge();
				}
			} else if (message.getType().equals(MsgType.NEW_LEADER)) {
				round = message.getRound();
				parent = message.getSender();
				root = message.getRoot_id();
				if (tree_edges.size() == 1) {
					System.out.println(id + " is Leaf Node, my root is " + root);
					// leaf node send ack to it's parent
					Message ack = generate_message(parent, MsgType.ACK, round, null, null);
					private_message(ack);
				} else if (tree_edges.size() > 1) {
					// broadcast new leader message to other nodes on tree
					System.out.println(id + " is Normal Node, my root is " + root);
					broadcast_on_tree(MsgType.NEW_LEADER, null, parent, message.getRoot_id());
				}
			} else if (message.getType().equals(MsgType.ACK)) {
				algo.getAck_set().add(message.getSender());
				Set<Integer> child = new HashSet<>(tree_edges.keySet());
				child.remove(parent);
				// receive all ack from neighbors in tree exceot parent
				if (algo.getAck_set().equals(child)) {
					algo.getAck_set().clear();
					if (parent != null && id != root) {
						Message ack = generate_message(parent, MsgType.ACK, round, null, null);
						private_message(ack);

						// root node will start next round by broadcasting search message
					} else if (root == id) {
						System.err.println(round + " now formally starts");
						// ????? How to start next round
						int sleep = 15;
						while (sleep > 0) {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							if (sleep == 20) {
								System.out.println("Search in " + sleep + " seconds");
							}
							if (sleep == 15) {
								System.out.println("Search in " + sleep + " seconds");
							}

							if (sleep == 10) {
								System.out.println("Search in " + sleep + " seconds");
							}
							if (sleep == 5) {
								System.out.println("Search in " + sleep + " seconds");
							}
							sleep--;
						}

						// start next round
						Message m = generate_message(id, MsgType.SEARCH, round, null, null);
						// start by sending search at itself
						send_to_self(m);
					}
					parent = null;
					System.out.println("parent becomes null at " + message_req_time);
				}
			} else if (message.getType().equals(MsgType.TEST)) {
				Integer comp_id = message.getRoot_id();
				Message tst_reply = null;
				// belonging to 2 different component
				if (comp_id != root) {
					int[] test_reply = new int[3];
					test_reply[0] = message.getReceiver();
					test_reply[1] = message.getSender();
					/*
					 * System.out.println("=========="); System.out.println("outgoing edges " +
					 * outgoing_edges); System.out.println("other edges " + other_edges);
					 * System.out.println("tree edges " + tree_edges);
					 * System.out.println("==========");
					 */
					Integer sender_weight = outgoing_edges.get(message.getSender());
					test_reply[2] = sender_weight == null ? tree_edges.get(message.getSender()) : sender_weight;
					tst_reply = generate_message(message.getSender(), MsgType.TEST_REPLY, round, test_reply, null);
					tst_reply.setSameComp(false);
					// same component
				} else {
					tst_reply = generate_message(message.getSender(), MsgType.TEST_REPLY, round, null, null);
					tst_reply.setSameComp(true);
				}

				private_message(tst_reply);
			} else if (message.getType().equals(MsgType.TEST_REPLY)) {
				int sender = message.getSender();
				algo.getTest_set().add(sender);
				// get test_reply from node at different component or from subtree, then compare
				// min mwoe
				if (message.getSameComp() == null || message.getSameComp() == false) {
					// format of edge: [smaller nodeid, larger node id, edge weight]
					// update mwoe
					int[] m = message.getEdge();
					if (min > m[2]) {
						min = m[2];
						mwoe = m;
					} else if (min == m[2]) {
						if (mwoe[0] > m[0]) {
							min = m[2];
							mwoe = m;
						} else if (mwoe[0] == m[0]) {
							if (mwoe[1] > m[1]) {
								min = m[2];
								mwoe = m;
							}
						}
					}
				}

				Set<Integer> except_parent = new HashSet<>(other_edges.keySet());
				except_parent.addAll(outgoing_edges.keySet());
				except_parent.addAll(tree_edges.keySet());
				except_parent.remove(parent);
				// nontree.remove(parent);
				System.out.println("parent: " + parent);
				System.out.println("real set " + algo.getTest_set());
				System.out.println("expected set " + except_parent);
				// if receive all test_reply except parent
				if (algo.getTest_set().equals(except_parent)) {
					receive_all_test = true;
					algo.getTest_set().clear();
					boolean res = (mwoe == null || mwoe.length == 0);
					System.err.println("The MWOR IS " + (res ? "null" : mwoe[0] + " ," + mwoe[1] + " ," + mwoe[2]));
					// root will start add_mwoe if min is not Integer.MAX_VALUE or broadcast
					// terminate since no more outgoing edge
					if (parent == null && id == root) {
						if (min == Integer.MAX_VALUE) {
							terminate(null);
						} else {
							// send ADD_MWOE to itself
							Message m = generate_message(id, MsgType.ADD_MWOE, round, mwoe, null);
							send_to_self(m);
						}
					} else {
						/*
						 * if (mwoe == null) { Message c_Message = generate_message(parent,
						 * MsgType.TEST_REPLY, round, new int[] { Integer.MAX_VALUE, Integer.MAX_VALUE,
						 * Integer.MAX_VALUE }, null); private_message(c_Message); } else { }
						 */
						Message cur_mwoe = generate_message(parent, MsgType.TEST_REPLY, round, mwoe, null);
						private_message(cur_mwoe);
					}
					parent = null;
					System.out.println("parent becomes null at " + message_req_time);
					// this node sent test_reply to it's parent then we can process merge message
					process_buffered_merge();
					// reset mwoe and min for next round
					mwoe = new int[] { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };
					min = Integer.MAX_VALUE;
				}
			} else if (MsgType.TERMINATE.equals(message.getType())) {
				terminate(message.getSender());
			}
		}
	}

	/**
	 * broadcast terminate message on tree except given number
	 * 
	 * @param except
	 *            node not to send
	 */
	private synchronized void terminate(Integer except) {
		broadcast_on_tree(MsgType.TERMINATE, null, except, null);
		System.out.println("To terminate node in " + 5 + " seconds");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.err.println("The final MST in this node is ");
		System.err.println(tree_edges);
	}

	/**
	 * process buffered merge message
	 */
	private synchronized void process_buffered_merge() {
		for (Message buffered_merge : algo.getBuffered_messages()) {
			int[] edge = buffered_merge.getEdge();
			if (edge[0] == id || id == edge[1]) {
				int other_id = edge[0] == id ? edge[1] : edge[0];
				// 1st time receive merge message only update edge status
				if (outgoing_edges.containsKey(other_id)) {
					int other_weight = outgoing_edges.remove(other_id);
					tree_edges.put(other_id, other_weight);
					// in case below it's a common mwoe bewteen 2 nodes
				} else if (tree_edges.containsKey(other_id)) {
					int max = Math.max(edge[0], edge[1]);
					if (max == id) {
						round++;
						root = id;
						int[] newleader = new int[3];
						newleader[0] = root;
						parent = null;
						System.out.println("parent becomes null at " + message_req_time);
						broadcast_on_tree(MsgType.NEW_LEADER, newleader, null, root);
					}
				}
			} else {
				System.err.println("There is sth wrong with merge message");
			}
		}
		algo.getBuffered_messages().clear();
	}

	/**
	 * generate a message with given parameters
	 * 
	 * @param receiver
	 *            node to send
	 * @param type
	 *            of message
	 * @param round
	 *            current round of this node
	 * @param edge
	 *            mwoe to be send
	 * @param root_id
	 *            root id of this message
	 * @return
	 */
	private synchronized Message generate_message(int receiver, String type, Integer round, int[] edge,
			Integer root_id) {
		Message message = new Message();
		message.setReceiver(receiver);
		message.setSender(id);
		message.setType(type);
		if (round != null) {
			message.setRound(round);
		}

		if (type.equals(MsgType.NEW_LEADER) || type.equals(MsgType.TEST)) {
			assert root_id != null;
			message.setRoot_id(root_id);
		}

		if (type.equals(MsgType.ADD_MWOE) || type.equals(MsgType.TEST_REPLY) || type.equals(MsgType.MERGE)) {
			if (edge != null) {
				message.setEdge(edge[0], edge[1], edge[2]);
			} else {
				assert type.equals(MsgType.TEST_REPLY);
			}
		}

		return message;
	}

	/**
	 * send a message to neighbor
	 * 
	 * @param message
	 */
	public synchronized void private_message(Message message) {
		message_req_time++;
		System.err.println(id + " Send Message " + message + " at " + message_req_time);
		ObjectOutputStream oos = outgoingMap.get(message.getReceiver());
		try {
			oos.writeObject(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * sent a message to node itself
	 * 
	 * @param message
	 */
	public synchronized void send_to_self(Message message) {
		System.err.println(id + "Send Message " + message + " at " + message_req_time);
		try {
			self_sender.writeObject(message);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private synchronized void start_built_MST() {
		if (id == root) {
			parent = null;
			// broadcast_on_tree(MsgType.SEARCH, null, null);
			Message m = generate_message(id, MsgType.SEARCH, round, null, null);
			send_to_self(m);
		}
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Please input correct format as: nodeid");
			System.exit(1);
		}

		Node node = new Node(args[0]);
		node.init();

		node.start_built_MST();
	}
}
