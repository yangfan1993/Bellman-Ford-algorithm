import java.util.*;
import java.net.*;
import java.io.*;
import java.text.*;
import java.nio.*;

class routeTable {
    public distanceVec self;
    public Hashtable<String, distanceVec> neighbors = new Hashtable<String,distanceVec>();
    // the distanceVec received from the neighbors
    public Hashtable<String, Double> nearby = new Hashtable<String, Double>();
    // the still linkup neighors and their link cost
    public Hashtable<String, Double> offline = new Hashtable<String, Double>();
    // the linkdwon neighbors and their link cost
    public Hashtable<String, Double> dead = new Hashtable<String, Double>();
    // the close(dead) neighbors
    public static double INF = 65535.0;

    public routeTable(distanceVec local) {
        // to initalize a new routeTable
        self = local;
        nearby = new Hashtable<String, Double>(local.linkWeight);
    }

    public routeTable(routeTable oldRT) {
        // copy a new routeTable from the old one
        self = new distanceVec(oldRT.self);
        neighbors = new Hashtable<String, distanceVec>(oldRT.neighbors);
        nearby = new Hashtable<String, Double>(oldRT.nearby);
        offline = new Hashtable<String, Double>(oldRT.offline);
    }

    public boolean compareRT(routeTable newRT) {
        // compare whehter two routeTable are the same, only concentrate on whether the distanceVec
        // and nearby have been changed, it is sufficient
        if (self.linkWeight.size() != newRT.self.linkWeight.size()) {
            return false;
        }
        Enumeration e = self.linkWeight.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (!newRT.self.linkWeight.containsKey(key)) {
                return false;
            } else if (!self.linkWeight.get(key).equals(newRT.self.linkWeight.get(key))) {
                return false;
            } else if (!self.nextHop.get(key).equals(newRT.self.nextHop.get(key))) {
                return false;
            }
        }
        if (nearby.size() != newRT.nearby.size()) {
            return false;
        }
        e = nearby.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (!newRT.nearby.containsKey(key)) {
                return false;
            } else if (!nearby.get(key).equals(newRT.nearby.get(key))) {
                return false;
            }
        }
        if (neighbors.size() != newRT.neighbors.size()) {
            return false;
        }
        /*if (offline.size() != newRT.offline.size()) {
            return false;
        }
        e1 = offline.keys();
        e2 = newRT.offline.keys();
        while (e1.hasMoreElements()) {
            Object key1 = e1.nextElement();
            Object key2 = e2.nextElement();
            if (offline.get(key1) != newRT.offline.get(key2)) {
                return false;
            }
        }*/
        return true;
    }

    public void linkup(String dest) {
        // the linkup function
        if (!offline.containsKey(dest)) {
            System.out.println("The link has never existed.");
        } else {
            double dist = offline.get(dest);
            nearby.put(dest, dist);
            // add it to the nearby and remove from offline
            offline.remove(dest);
            if (self.linkWeight.containsKey(dest)) {
                // if they have indirect link before the linkup
                if (nearby.get(dest) > self.linkWeight.get(dest)) {
                } else {
                    self.linkWeight.put(dest, nearby.get(dest));
                    self.nextHop.put(dest, dest);
                }
            } else {
                // if they do not have indirect link before linkup
                self.linkWeight.put(dest, nearby.get(dest));
                self.nextHop.put(dest, dest);
            }
        }
    }

    public void linkdown(String dest) {
        // the linkdown function
        if (!nearby.containsKey(dest)) {
            System.out.println("No link exists between these two nodes.");
        } else {
            double dist = nearby.get(dest);
            nearby.remove(dest);
            // remove it from the nearby and add to the offline
            offline.put(dest, dist);
            neighbors.remove(dest);
            if (!self.nextHop.get(dest).equals(dest)) {
                // if the direct link is not the optimal one
            } else {
                // if the direct link is the optimal one
                self.linkWeight.remove(dest);
                self.nextHop.remove(dest);
                double shortest = INF;
                Enumeration e = neighbors.keys();
                String shortcut = new String("");
                // set as infinity first and find the new optimal path to the linkdown node, it may exist
                while(e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    distanceVec neighborVec = (distanceVec) neighbors.get(key);
                    if (neighborVec.linkWeight.containsKey(dest) && (!neighborVec.nextHop.get(dest).equals(self.name))) {
                        if (!self.nextHop.get(neighborVec.name).equals(dest)) {
                            // the criterion here is just like the posion reverse, find from both the neighborVec
                            if (neighborVec.linkWeight.get(dest) + self.linkWeight.get(neighborVec.name) < shortest) {
                                shortcut = neighborVec.name;
                                shortest = neighborVec.linkWeight.get(dest) + self.linkWeight.get(neighborVec.name);
                                self.linkWeight.put(dest, shortest);
                                self.nextHop.put(dest, shortcut);
                            }
                        } else {
                            if (neighborVec.linkWeight.get(dest) + nearby.get(neighborVec.name) < shortest) {
                                // also find from the nearby, because the value in nearby and neighborVec may not be the same
                                // they are the same only if the direct link is the optimal link
                                shortcut = neighborVec.name;
                                shortest = neighborVec.linkWeight.get(dest) + nearby.get(neighborVec.name);
                                self.linkWeight.put(dest, shortest);
                                self.nextHop.put(dest, shortcut);
                            }
                        }
                    }
                }
                if (!self.linkWeight.containsKey(dest)) {
                    // if we could not find it, or Poison reverse not allow us, just remove the nodes 
                    // that need to go through the linkdown node it
                    e = self.nextHop.keys();
                    while(e.hasMoreElements()) {
                        String key = (String) e.nextElement();
                        if (self.nextHop.get(key).equals(dest)) {
                            self.linkWeight.remove(key);
                            self.nextHop.remove(key);
                        }
                    }
                } else {
                    // if we could find it, then the other nodes that need to go through that also need 
                    // to be upadated
                    e = self.nextHop.keys();
                    while(e.hasMoreElements()) {
                        String key = (String) e.nextElement();
                        if (self.nextHop.get(key).equals(dest)) {
                            self.linkWeight.put(key, shortest + self.linkWeight.get(key));
                            self.nextHop.put(key, shortcut);
                        }
                    }
                }
            }
        }
    }

    public void linkdestory(String dest) {
        dead.put(dest, nearby.get(dest));
        nearby.remove(dest);
        // remove all the things about it
        self.linkWeight.remove(dest);
        self.nextHop.remove(dest);
        neighbors.remove(dest);

        Enumeration e = self.linkWeight.keys();
        while(e.hasMoreElements()) {
            // remove all the nodes need to go through that node, only if it is in the nearby of the current node
            String key = (String) e.nextElement();
            if (self.nextHop.get(key).equals(dest)) {
                if (!nearby.containsKey(key)) {
                    self.nextHop.remove(key);
                    self.linkWeight.remove(key);
                } else {
                    self.linkWeight.put(key, nearby.get(key));
                    self.nextHop.put(key, key);
                }
            }
        }
    }


    public void updateTable(distanceVec newVec, Double linkWeight) {
        nearby.put(newVec.name, linkWeight);
        if (offline.containsKey(newVec.name)) {
            offline.remove(newVec.name);
        }
    	neighbors.put(newVec.name, newVec);
        // update the weight and hop table
        Enumeration e = newVec.linkWeight.keys();
        if (!self.linkWeight.containsKey(newVec.name)) {
            // here is to update the link cost between the source and destination node
            self.linkWeight.put(newVec.name, newVec.linkWeight.get(self.name));
                self.nextHop.put(newVec.name, newVec.name);
        } else {
            if (self.linkWeight.get(newVec.name) > newVec.linkWeight.get(self.name)) {
                self.linkWeight.put(newVec.name, newVec.linkWeight.get(self.name));
                self.nextHop.put(newVec.name, newVec.name);
            }
        }
        while(e.hasMoreElements()) {
        	String key = (String) e.nextElement();
            // here is the core of the distributed BF algorithm as illustrated in the book
            if (!dead.containsKey(key)) {
        	    if (key.equals(self.name)) {
                    continue;
        	    } else if (!self.linkWeight.containsKey(key)) {
        		    self.nextHop.put(key, newVec.name);
        		    self.linkWeight.put(key, self.linkWeight.get(newVec.name) + newVec.linkWeight.get(key));
        	    } else {
                    if (self.linkWeight.get(newVec.name) + newVec.linkWeight.get(key) < self.linkWeight.get(key)) {
                	    self.nextHop.put(key, newVec.name);
                        self.linkWeight.put(key, self.linkWeight.get(newVec.name) + newVec.linkWeight.get(key));
                    }
        	    }
            }
        }
        
        e = self.linkWeight.keys();
        while(e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (!dead.containsKey(key)) {
                if (self.nextHop.get(key).equals(newVec.name)) {
                    if (key.equals(newVec.name)) {
                        continue;
                    }
                    if (! newVec.linkWeight.containsKey(key)) {
                        // here consider some consequences of Posion reverse, under this situation, 
                        // it means that some link in the have been linkdowned.
                        self.linkWeight.remove(key);
                        self.nextHop.remove(key);
                        // this need the node to acquire new link because of the situation
                        double shortest = INF;
                        if (nearby.containsKey(key)) {
                            shortest = nearby.get(key);
                            self.linkWeight.put(key, nearby.get(key));
                            self.nextHop.put(key, key);
                        }
                        Enumeration f = neighbors.keys();
                        while(f.hasMoreElements()) {
                            String keyf = (String) f.nextElement();
                            distanceVec neighborVec = (distanceVec) neighbors.get(keyf);
                            if (neighborVec.linkWeight.containsKey(key) && (!neighborVec.nextHop.get(key).equals(self.name)) &&
                                (neighborVec.linkWeight.get(key) + self.linkWeight.get(neighborVec.name) < shortest)) {
                                shortest = neighborVec.linkWeight.get(key) + self.linkWeight.get(neighborVec.name);
                                self.linkWeight.put(key, shortest);
                                self.nextHop.put(key, neighborVec.name);
                            }
                        }
                    } else if (self.linkWeight.get(key) < newVec.linkWeight.get(key) + self.linkWeight.get(newVec.name)) {
                        // another situation that means that some link in the have been linkdowned.
                        double shortest = newVec.linkWeight.get(key) + self.linkWeight.get(newVec.name);
                        Enumeration f = neighbors.keys();
                        String shortcut = new String(newVec.name);
                        // this need the node to acquire new link because of the situation
                        while(f.hasMoreElements()) {
                            String keyf = (String) f.nextElement();
                            distanceVec neighborVec = (distanceVec) neighbors.get(keyf);
                            if (neighborVec.linkWeight.containsKey(key) && (!neighborVec.nextHop.get(key).equals(self.name))
                                && (neighborVec.linkWeight.get(key) + self.linkWeight.get(neighborVec.name) < shortest)) {
                                shortcut = neighborVec.name;
                                shortest = neighborVec.linkWeight.get(key) + self.linkWeight.get(neighborVec.name);
                            }
                        }
                        self.linkWeight.put(key, shortest);
                        self.nextHop.put(key, shortcut);
                    }
                }
            }
        }
    }

}
