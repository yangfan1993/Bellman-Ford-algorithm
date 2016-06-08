import java.util.*;
import java.net.*;
import java.io.*;
import java.text.*;
import java.nio.*;

public class distanceVec {
    public String name;
    public Hashtable<String, Double> linkWeight = new Hashtable<String, Double>();
    public Hashtable<String, String> nextHop = new Hashtable<String, String>();
    // the two hashtable, one for link cost, another for nexthop

    public distanceVec(String localAdd, ArrayList<String> addList, ArrayList<Double> weightList) {
        // initialize the orignial distanceVec
        name = localAdd;
        for (int i=0; i<addList.size(); i++) {
            linkWeight.put(addList.get(i), weightList.get(i));
            nextHop.put(addList.get(i), addList.get(i));
        }
    }

    public distanceVec(distanceVec oldVec) {
        // copy a new distanceVec from the original one
        name = oldVec.name;
        linkWeight = new Hashtable<String, Double>(oldVec.linkWeight);
        nextHop = new Hashtable<String, String>(oldVec.nextHop);
    }

    public distanceVec(byte[] datum) {
        // retrieve the distanceVec from the received message
        byte[] temp = new byte[20];
        System.arraycopy(datum, 28, temp, 0, 20);
        name = new String(temp).trim();
        int size = (datum.length-56)/48;
        for (int i=1; i<=size; i++) {
            temp = new byte[20];
            System.arraycopy(datum, 48*i, temp, 0, 20);
            String dest = new String(temp).trim();
            byte[] temp2 = new byte[8];
            System.arraycopy(datum, 48*i+20, temp2, 0, 8);
            double weight = ByteBuffer.wrap(temp2).getDouble();
            temp = new byte[20];
            System.arraycopy(datum, 48*i+28, temp, 0, 20);
            String hop = new String(temp).trim();
            linkWeight.put(dest, weight);
            nextHop.put(dest, hop);
        }
    }

    public void showRT() {
        // the showRT function, to show the distanceVec of that bfclient
        Date date = new Date();
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("<" + date + "> Distance vector list is:");
        Enumeration e = linkWeight.keys();
        while (e.hasMoreElements()) {
        	String key = (String) e.nextElement();
        	System.out.println("Destination = " + key + ", Cost = " + linkWeight.get(key) + 
        		", Link = (" + nextHop.get(key) +")");
        }
    }

    public byte[] generatePacket() {
        // change the distanceVec to the byte, later sent as message
        byte[] datum = new byte[56 + 48*linkWeight.size()];
        ByteBuffer buffer = ByteBuffer.allocate(2);
        byte[] length = buffer.putShort((short)linkWeight.size()).array();
        System.arraycopy(length, 0, datum, 22, 2);
        byte[] temp = name.getBytes();
        System.arraycopy(temp, 0, datum, 28, temp.length);
        Enumeration e = linkWeight.keys();
        for (int i=1; i<=linkWeight.size(); i++) {
            String key = (String) e.nextElement();
            temp = key.getBytes();
            System.arraycopy(temp, 0, datum, 48*i, temp.length);
            double weight = linkWeight.get(key);
            buffer = ByteBuffer.allocate(8);
            byte[] temp2 = buffer.putDouble(weight).array();
            System.arraycopy(temp2, 0, datum, 48*i+20, 8);
            String hop = nextHop.get(key);
            temp = hop.getBytes();
            System.arraycopy(temp, 0, datum, 48*i+28, temp.length);
        }
        return datum;
    }

    public boolean compareDV(distanceVec newVec) {
        // to compare whether two distanceVec are the same, but actually not utilized in upper layer
        // we only use the compareRT function, it is more reliable
        if (linkWeight.size() != newVec.linkWeight.size()) {
            return false;
        }
        Enumeration e1 = linkWeight.keys();
        Enumeration e2 = newVec.linkWeight.keys();
        while (e1.hasMoreElements()) {
            Object key1 = e1.nextElement();
            Object key2 = e2.nextElement();
            if (!nextHop.get(key1).equals(newVec.nextHop.get(key2))) {
                return false;
            }
            if (linkWeight.get(key1) != newVec.linkWeight.get(key2)) {
                return false;
            }
        }
        return true;
    }

}
