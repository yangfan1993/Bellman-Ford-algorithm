import java.util.*;
import java.net.*;
import java.io.*;
import java.text.*;
import java.nio.*;

public class bfclient {
	public static distanceVec localDV;
	public static int maxClients = 10;
    // here the max client set to 10, but assignment requirement is only 5
	public static routeTable localRT;
    BroadCast broadcast;
    // the broadcast thread
    Listen listen;
    // the listen thread

	public bfclient(String[] args) {
		int localPort;
		String localAdd = new String("");
	    int timeoutTime = 1;
	    ArrayList<String> addList = new ArrayList<String>();
	    ArrayList<Double> weightList = new ArrayList<Double>();
        // the two arraylist to store the original datum
	    BufferedReader user_in;

		if (args.length >=5 && args.length %3 == 2) {
			localPort = Integer.parseInt(args[0]);
			timeoutTime = Integer.parseInt(args[1]);
			try {
				localAdd = InetAddress.getLocalHost().getHostAddress().toString() + ":" + String.valueOf(localPort);
                for (int i=0; i<(args.length-2)/3; i++) {
                	addList.add(args[i*3+2] + ":" + args[i*3+3]);
                	weightList.add(Double.parseDouble(args[i*3+4]));
                    // read the invoke datum of the command line
                }
			} catch (UnknownHostException e) {
				System.out.println(e.getLocalizedMessage());
				System.exit(0);
			} catch (IOException e) {
				System.out.println(e.getLocalizedMessage());
				System.exit(0);
			}
		} else {
			System.out.println("Parameter Wrongness! Please refer to the Readme file.");
			System.exit(0);
		}

        localDV = new distanceVec(localAdd, addList, weightList);
        // construct the new distanceVec
        localRT = new routeTable(localDV);
        // construct the new routeTable
        broadcast = new BroadCast(timeoutTime);
        listen = new Listen(localAdd, timeoutTime);
        // run the two threads
        
        System.out.println("Welcome to the bfclient!");
        System.out.println("Available Command: SHOWRT, LINKUP, LINKDOWN, CLOSE");
        try {
            user_in = new BufferedReader(new InputStreamReader(System.in, "UTF8"));
		    while (true) {
			    String readline = user_in.readLine();
                // receive the command of the user
			    String[] tokens = readline.split(" ");
			    if (tokens.length == 0) {
				    continue;
			    }
			    if (tokens[0].equals("SHOWRT")) {
                    // handle the SHOWRT command
				    if (tokens.length != 1) {
                        System.out.println("Unknown Command!");
				    } else {
                        localDV.showRT();
				    }
			    } else if (tokens[0].equals("LINKDOWN")) {
                    // handle the linkdown command
                    if (tokens.length != 2) {
                	    System.out.println("Unknown Command!");
                    } else {
                        // here to generate the linkdown message to the linkdown neighbor
                        byte[] datum = new byte[48];
                        byte[] temp = tokens[1].getBytes();
                        System.arraycopy(temp, 0, datum, 0, temp.length);
                        temp = localDV.name.getBytes();
                        System.arraycopy(temp, 0, datum, 28, temp.length);
                        datum[20] = 1;
                        // this marks that it is a linkdown message

                        int checksum = 0;
                        for (int i=0; i<datum.length; i=i+4) {
                            checksum += datum[i+3] + 16*datum[i+2];
                            checksum += 256*datum[i+1] + 4096*datum[i];
                        }
                        ByteBuffer buffer = ByteBuffer.allocate(4);
                        byte[] check = buffer.putInt(checksum).array();
                        System.arraycopy(check, 0, datum, 24, 4);
                        String[] destAdd = tokens[1].split(":");
                        InetAddress dest_IP = InetAddress.getByName(destAdd[0]);
                        int destPort = Integer.parseInt(destAdd[1]);
                        DatagramPacket packet = new DatagramPacket(datum, datum.length, dest_IP, destPort);
                        DatagramSocket socket = new DatagramSocket();
                        System.out.println("send linkdown message successfully");
                        socket.send(packet);
                        // send the linkdown message to one specified neighbor
                	    localRT.linkdown(tokens[1]);
                        // change the routeTable
                        socket.close();
                        broadcast.BC2Neighbors();
                        // because the routeTable and distanceVec is changed, it have to be broadcasted.
                    }
			    } else if (tokens[0].equals("LINKUP")) {
                    // handle the linkup command 
                    if (tokens.length != 2) {
                        System.out.println("Unknown Command!");
                    } else {
                        localRT.linkup(tokens[1]);
                        // here do not need linkup message, once the neighbor receive the normal update message
                        // it know it need to be linkup
                        broadcast.BC2Neighbors();
                        // because the routeTable and distanceVec is changed, it have to be broadcasted.
                    }
			    } else if (tokens[0].equals("CLOSE")) {
                    // handle the close command
                    if (tokens.length != 1) {
                	    System.out.println("Unknown Command!");
                    } else {
                	    byte[] datum = new byte[48];
                        byte[] temp = localDV.name.getBytes();
                        System.arraycopy(temp, 0, datum, 28, temp.length);
                        datum[20] = 2;
                        // this marks that it is a exit message (here called as linkdestroy)

                        DatagramSocket socket = new DatagramSocket();
                        Enumeration e = localRT.nearby.keys();
                        while (e.hasMoreElements()) {
                            // to inform all its neighbors that it is going to be closed
                            String key = (String) e.nextElement();
                            temp = key.getBytes();
                            System.arraycopy(temp, 0, datum, 0, temp.length);
                            ByteBuffer buffer = ByteBuffer.allocate(8);
                            int checksum = 0;
                            for (int i=0; i<datum.length; i=i+4) {
                                if (i == 24) {
                                    continue;
                                }
                                checksum += datum[i+3] + 16*datum[i+2];
                                checksum += 256*datum[i+1] + 4096*datum[i];
                            }
                            buffer = ByteBuffer.allocate(4);
                            byte[] check = buffer.putInt(checksum).array();
                            System.arraycopy(check, 0, datum, 24, 4);
                            String[] destAdd = key.split(":");
                            InetAddress dest_IP = InetAddress.getByName(destAdd[0]);
                            int destPort = Integer.parseInt(destAdd[1]);
                            DatagramPacket packet = new DatagramPacket(datum, datum.length, dest_IP, destPort);
                            socket.send(packet);
                        }
                        socket.close();
                        // if it is closed, then exit it.
                        System.exit(0);
                    }
			    } else {
				    System.out.println("Unknown Command!");
			    }
		    }
	    } catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(0);
		}
	}

	public static void main(String[] args) {
		new bfclient(args);
        // main thread
	}

	class Listen extends Thread {
		DatagramSocket socket;
		String localAdd;
        int timeout;

        public Listen(String add, int timeoutTime) {
        	localAdd = add;
            timeout = timeoutTime;
            // start the listening thread
        	start();
        }

		public void run() {
			try {
				int localPort = Integer.parseInt(localAdd.split(":")[1]);
				DatagramSocket socket = new DatagramSocket(localPort);
                Hashtable<String, Timer> timers = new Hashtable<String, Timer>();
                // to timers for all the neighbors
                Enumeration e = localRT.nearby.keys();
                while(e.hasMoreElements()) {
                    // first initialize all the countdown timers for the neighbors 
                    final String key = (String) e.nextElement();
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        public void run() {
                            System.out.println(key + " is now offline.");
                            localRT.linkdestory(key);
                            // if countdown, then regard it as dead
                        }
                    }, 1000 * 60 * timeout * 3);
                    timers.put(key, timer);
                }
                while (true) {
            	    Thread.sleep(100);
                    // to make the listening thread not receive message so frequently
            	    byte[] segment = new byte[maxClients * 48 + 8];
            	    DatagramPacket packet = new DatagramPacket(segment, segment.length);
            	    socket.receive(packet);
                    System.out.println("Receive packet!");
            	    segment = packet.getData();
            	    byte[] temp = new byte[20];
            	    System.arraycopy(segment, 0, temp, 0, 20);
                    // check whether sent to the right destination
            	    String destAdd = new String(temp).trim();
            	    if (!destAdd.equals(localAdd)) {
            	    	continue;
            	    }
            	    int checksum = 0;
            	    temp = new byte[4];
            	    System.arraycopy(segment, 24, temp, 0, 4);
            	    int verify = ByteBuffer.wrap(temp).getInt();
            	    for (int i=0; i<segment.length; i=i+4) {
                        if (i == 24) {
                            continue;
                        } else {
            	    	    checksum += segment[i+3] + 16*segment[i+2];
                            checksum += 256*segment[i+1] + 4096*segment[i];
                        }
            	    }
            	    if (verify != checksum) {
            	    	continue;
            	    }
                    // check the checksum
            	    temp = new byte[2];
            	    System.arraycopy(segment, 22, temp, 0, 2);
            	    int length = ByteBuffer.wrap(temp).getShort();
                    routeTable preRT = new routeTable(localRT);
                    // store the current routetable before updated
                    if (length == 0) {
                        if (segment[20] == 1) {
                            // if it is a linkdown message
                            temp = new byte[20];
                            System.arraycopy(segment, 28, temp, 0, 20);
                            String linkdownAdd = new String(temp).trim();
                            System.out.println("receive linkdown message!");
                            localRT.linkdown(linkdownAdd);
                        } else if (segment[20] == 2) {
                            // if it is a close message
                            temp = new byte[20];
                            System.arraycopy(segment, 28, temp, 0, 20);
                            String linkdesAdd = new String(temp).trim();
                            System.out.println("receive exit message!");
                            localRT.linkdestory(linkdesAdd);
                        } else {
                            continue;
                        }
                    } else {
                        // if it is a normal message
                        byte[] newseg = new byte[length*48+56];
            	        System.arraycopy(segment, 0, newseg, 0, length*48+56);
                        temp = new byte[8];
                        System.arraycopy(newseg, newseg.length-8, temp, 0, 8);
                        Double link = ByteBuffer.wrap(temp).getDouble();
                        distanceVec newDV = new distanceVec(newseg);
                        // retrive the distanceVec sent from the neighbor from the message
                        if (timers.containsKey(newDV.name)) {
                            Timer timer = timers.get(newDV.name);
                            timer.cancel();
                        }
                        final String key = new String(newDV.name);
                        // update the timer, reset the time to 3*timeout
                        Timer newtimer = new Timer();
                        newtimer.schedule(new TimerTask() {
                            public void run() {
                                if (localRT.nearby.containsKey(key)) {
                                    System.out.println(key + " is now offline.");
                                    localRT.linkdestory(key);
                                    broadcast.BC2Neighbors();
                                }
                            }
                        }, 1000 * 10 * timeout * 3);
                        // if not receive message from the nearby for three timeout, then regard it as dead
                        timers.put(key, newtimer);
                        localRT.updateTable(newDV, link);
                        // update the routeTable
                    }
                    if (!localRT.compareRT(preRT)) {
                        // if the routeTable is changed, then broadcast to all its neighbors
                        broadcast.BC2Neighbors();
                    }
                }
            } catch (IOException e) {
				System.out.println(e.getLocalizedMessage());
				System.exit(0);
			} catch (InterruptedException e) {
				System.out.println(e.getLocalizedMessage());
				System.exit(0);
			}
		}

	}

	class BroadCast extends Thread {
        int timeout;

		public BroadCast(int timeoutTime) {
            timeout = timeoutTime;
            // start the broadcast thread
            start();
		}

		public void run() {
			BC2Neighbors();
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    BC2Neighbors();
                }
            }, 1000 * 60 * timeout, 1000 * 60 * timeout);
            // set the timeout, to broadcast periodically.
            while (true) {
                /*
                if (!localDVec.compareDV(localDV)) {
                    System.out.println("Different!");
                    BC2Neighbors();
                    task = new Task();
                    timer.schedule(task, 1000 * 10 * timeout, 1000 * 10 * timeout);
                }*/
            }
		}

		public void BC2Neighbors() {
            // the broadcast of ordinary update message
            try {
                System.out.println("BroadCasting!");
                DatagramSocket socket = new DatagramSocket();
                byte[] datum = localDV.generatePacket();
                // generate the common part of the message
                Enumeration e = localRT.nearby.keys();
                // generate the specified part of the message, depending on which neighbor it is sent
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    byte[] temp = key.getBytes();
                    System.arraycopy(temp, 0, datum, 0, temp.length);
                    ByteBuffer buffer = ByteBuffer.allocate(8);
                    byte[] origin = buffer.putDouble(localRT.nearby.get(key)).array();
                    System.arraycopy(origin, 0, datum, datum.length-8, 8);
                    int checksum = 0;
                    for (int i=0; i<datum.length; i=i+4) {
                        if (i == 24) {
                            continue;
                        }
                        checksum += datum[i+3] + 16*datum[i+2];
                        checksum += 256*datum[i+1] + 4096*datum[i];
                    }
                    buffer = ByteBuffer.allocate(4);
                    byte[] check = buffer.putInt(checksum).array();
                    System.arraycopy(check, 0, datum, 24, 4);
                    String[] destAdd = key.split(":");
                    InetAddress dest_IP = InetAddress.getByName(destAdd[0]);
                    int destPort = Integer.parseInt(destAdd[1]);
                    DatagramPacket packet = new DatagramPacket(datum, datum.length, dest_IP, destPort);
                    socket.send(packet);
                    // send the packet to the neighbors
                }
                socket.close();
			} catch (SocketException e) {
				System.out.println(e.getLocalizedMessage());
				System.exit(1);
			} catch (IOException e) {
				System.out.println(e.getLocalizedMessage());
				System.exit(1);
			}
		}

	}

}
