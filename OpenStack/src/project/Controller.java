package project;

import java.net.URL;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


import lib.Debug;
import lib.SubnetAddress;
import object.Network;
import object.Port;

/**
 * Controller Instance
 *
 *
 * Note that in this version, IPAddress of a range 192.168.(some number).0/24 is assigned
 * for each subnet. IT IS HARCODED NOW!!!
 * @author TAISHI
 *
 */

//TODO make sure randomly generated Id is positive by taking a mod
public class Controller {
	// global OpenStack configuration
	public HashMap<String, String> configMap;


	// maps tenantID to Hash Map of Network (mapping network ID to Network)
	private HashMap<Long, HashMap<Long,Network>> tenantMap;
	//private DNS dnsServer;

	// maps portNums to Port instance
	public HashMap<Integer, Port> portMap;

	// list of used subnet addresses (in the form of 192.168.(some number).0)
	private ArrayList<Inet4Address> usedSubnetAddrList;

	// list of used bridge names
	private ArrayList<String> usedBridgeNameList;

	// Miscellaneous
	public Random randomGen;

	// where we store scripts
    private String scriptDirectory;

    // ip and NIC info for host tables
    public Inet4Address hostIP;
    public String hostNIC;

	public Controller(HashMap<String,String> configMap) throws Exception {
		this.tenantMap = new HashMap<Long, HashMap<Long,Network>>();
		this.randomGen = new Random();
		//this.dnsServer = new DNS();
		this.configMap = configMap;

		//port map init
		this.portMap = new HashMap<>();
		try {
			int ctrlPort = Integer.parseInt(configMap.get("CtrlPort"));
			this.registerNewPort(ctrlPort);
		} catch (Exception e) {
			throw new Exception("Couldn't parse CtrlPort");
		}

		this.usedSubnetAddrList = new ArrayList<>();
		this.usedBridgeNameList = new ArrayList<>();


		// 192.168.0.0 - 192.168.15.0 is illegal (reserved for host machine)
		for (int i = 0; i < 16; i++) {
			byte[] rawAddr = new byte[]{(byte) 192,(byte) 168,(byte) i, (byte) 0};
			Inet4Address addr;
			try {
				addr = (Inet4Address) Inet4Address.getByAddress(rawAddr);
			} catch (UnknownHostException e) {
				// shouldn't happen
				continue;
			}
			usedSubnetAddrList.add(addr);
		}

		this.scriptDirectory = this.configMap.get("LocScript");
		this.hostIP = getHostIp(); // blocking
		this.hostNIC= "eth0"; // CONFIGURE TO SET NETWORK INTERFACE CARD
		setupRouting();
	}



	/**
	 * Assigns available private address space for a subset.
	 * Initialize SubnetAddress instance and returns it
	 * Should be called when a new subnet is created to assign
	 * the range of IP address
	 *
	 * For the sake of simplicity, we only use 192.168.(available).0/24
	 * where available is in the range of 16-255
	 * @param subnetID
	 * @return SubnetAddress - initialized subnet address object on success, o.w. null
	 */
	public SubnetAddress getAvailableSubnetAddr(long subnetID) {
		// maintain the range of private IP address that is already used by some other networks
		// assign a unique bridge name for the subnet
		// return available range of IP address as SubnetAddress instance

		byte[] rawSubnetAddr = new byte[4];
		rawSubnetAddr[0] = (byte) 192;
		rawSubnetAddr[1] = (byte) 168;
		rawSubnetAddr[2] = (byte) 15;
		rawSubnetAddr[3] = (byte) 0;

		int mask = 24;

		for (int i = 16; i < 256; i++) {
			rawSubnetAddr[2] = (byte) i;
			Inet4Address subnetAddr;

			try {
				subnetAddr = (Inet4Address) Inet4Address.getByAddress(rawSubnetAddr);
			} catch (UnknownHostException e) {
				// shouldn't happen
				continue;
			}

			if (!isSubnetAddrAlreadyRegistered(subnetAddr)) {
				String bridgeName = assignNewSubnetBridgeName();
				if (bridgeName == null) {
					return null;
				}

				SubnetAddress sa;
				try {
					sa = new SubnetAddress(this, subnetAddr, mask, subnetID, bridgeName);
				} catch (Exception e) {
					Debug.redDebug("Error in creating SubnetAddress Instance");
					return null;
				}
				return sa;
			}
		}

		return null;
	}

	public Collection<Network> getNetworkList(long tenantID) throws Exception{
		HashMap<Long, Network> networkMap = tenantMap.get(tenantID);
		if (networkMap == null) {
			throw new Exception("getNetworkList(): No such tenant");
		}

		return networkMap.values();
	}

	/**
	 * Assigns a new bridge name for subnet
	 * @return bridge name on success, null on not available
	 */
	private String assignNewSubnetBridgeName() {
		//TODO probably not gonna make more than 10000 subnets
		for (int i = 0; i < 10000; i++) {
			String name = "subnetbr" + i;
			if (!isBridgeNameAlreadyRegistered(name)) {
				usedBridgeNameList.add(name);
				return name;
			}
		}
		return null;
	}



	private boolean isSubnetAddrAlreadyRegistered(Inet4Address subnetAddr) {
		if (usedSubnetAddrList.contains(subnetAddr)) {
			return true;
		}
		return false;
	}

	private boolean isBridgeNameAlreadyRegistered(String bridgeName) {
		if (usedBridgeNameList.contains(bridgeName)) {
			return true;
		}
		return false;
	}


	/**
	 * Deregisters SubnetAddress instance from this application
	 * Should be called only from {@code destroySubnetAddress} method
	 */
	public void deregisterSubnetAddrFromController(SubnetAddress sa) {
		usedSubnetAddrList.remove(sa.subnetAddress);
		usedBridgeNameList.remove(sa.bridgeName);
	}



	/**
	 * Registers a new tenant to this controller
	 * @return tenant ID that was registered
	 */
	public long registerNewTenant() {
		long tenantID;

		// randomly generate tenant ID until it finds a new one
		do {
			tenantID = randomGen.nextLong();
		} while (tenantMap.containsKey(tenantID));

		HashMap<Long, Network> networkMap = new HashMap<Long, Network>();

		tenantMap.put(tenantID, networkMap);
		return tenantID;
	}

	/**
	 * Deletes all resources associated with a specified tenant
	 * @throws Exception - when couldn't find such tenant
	 */
	public void deregisterTenant(long tenantID) throws Exception{
		HashMap<Long, Network> networkMap = tenantMap.remove(tenantID);
		if (networkMap == null) {
			throw new Exception("deregisterTenant(): didn't find such tenant");
		}
		Collection<Network> networks = networkMap.values();
		for (Network network : networks) {
			network.destroy();
		}
	}

	/**
	 * Registers a new network to a specified tenant.
	 * It creates a new Network instance, assigning a new available network ID.
	 * @return Created Network instance on success; otherwise null
	 */
	public Network registerNewNetwork(long tenantID, String networkName) {
		HashMap<Long, Network> networkMap = tenantMap.get(tenantID);
		if (networkMap == null) {
			return null;
		}

		long networkID;
		// randomly generate network ID until it finds a new one
		do {
			networkID = randomGen.nextLong();
		} while (networkMap.containsKey(networkID));

		Network network = new Network(this, networkName, tenantID, networkID);

		networkMap.put(networkID, network);
		return network;
	}

	/**
	 * Get the associated Network instance from tenantID and networkID
	 * @return Associated Network instance on success; otherwise null
	 */
	public Network getNetworkFromID(long tenantID, long networkID) {
		HashMap<Long, Network> networkMap = tenantMap.get(tenantID);
		if (networkMap == null) {
			return null;
		}

		Network network = networkMap.get(networkID);
		if (network == null) {
			return null;
		}

		return network;
	}

	/**
	 * Registers a new open port to this network.
	 * @param int - 0 to randomly generate a port number
	 *            [number] to generate a port at port # [number]
	 * @return port number that was registered or null if already registered
	 */
	public Port registerNewPort(int number) {
		int portNum = number;
		if (portNum == 0){
			// randomly generate port number until it finds a new one
			do {
				portNum = (int) ((this.randomGen.nextLong()) % 65000);
				if (portNum <= 1024){
					continue;
				}
			} while (portMap.containsKey(portNum));
		}
		if (portMap.containsKey(portNum)) // specified port number is occupied
			return null;
		Port newPort = new Port(number, this);
		portMap.put(number, newPort);
		return newPort;
	}


	/**
	 * establishRule - creates a rule in IP table of host machine for a specified routing config
	 * @param up/downstream Addr - address to receive/download from
	 * @param up/downstream port - port to receive/download from
	 * @param bridgeName - name of the network interface card attached to the VM/downstream client
	 */

    // Establishes the rules in iptable on NAT for both prerouting from and to the VM
    public void establishRule(Inet4Address upstreamAddress, int upstreamPort,Inet4Address downstreamAddress,
                              int downstreamPort, String bridgeName){
        String addRuleScript = scriptDirectory + "/add_rule.sh";

        ProcessBuilder pb = new ProcessBuilder("/bin/bash", addRuleScript);
        pb.environment().put("UPSTREAMADDR", upstreamAddress.getHostAddress());
        pb.environment().put("DOWNSTREAMADDR", downstreamAddress.getHostAddress());
        pb.environment().put("UPSTREAMPORT", Integer.toString(upstreamPort));
        pb.environment().put("DOWNSTREAMPORT", Integer.toString(downstreamPort));

        pb.environment().put("host_interface", this.hostNIC);
        pb.environment().put("vnet_interface", bridgeName);

        Process p;

        try {
            p = pb.start();
        } catch (Exception e) {
            Debug.redDebug("Error with executing add_rule.sh");
        }

    }

    // Removes this rule from the IP tables (specifying args)
    public void destroyRule(Inet4Address upstreamAddress, int upstreamPort,Inet4Address downstreamAddress,
                            int downstreamPort, String bridgeName){
        String removeRuleScript = scriptDirectory + "/remove_rule.sh";
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", removeRuleScript);
        pb.environment().put("UPSTREAMADDR", upstreamAddress.getHostAddress());
        pb.environment().put("DOWNSTREAMADDR", downstreamAddress.getHostAddress());
        pb.environment().put("UPSTREAMPORT", Integer.toString(upstreamPort));
        pb.environment().put("DOWNSTREAMPORT", Integer.toString(downstreamPort));

        pb.environment().put("host_interface", this.hostNIC);
        pb.environment().put("vnet_interface", bridgeName);

        Process p;

        try {
            p = pb.start();
        } catch (Exception e) {
            Debug.redDebug("Error with executing remove_rule.sh");
        }
    }


   	/**
	 * Finds the external IP of host by making a web request
	 * @return Inet4Address - Host's External IP
	 */
    public Inet4Address getHostIp() throws Exception {
        Inet4Address ipaddress = null;

        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = new BufferedReader(new InputStreamReader(
                        whatismyip.openStream()));
        String ip = in.readLine(); //you get the IP as a String
        ipaddress = (Inet4Address) Inet4Address.getByName(ip);

		return ipaddress;
    }

	/**
	 * Sets up the intial forward config for routing tables on host machine
	 */
    public void setupRouting(){
        String setupRoutingScript = scriptDirectory + "/routing_setup.sh";
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", setupRoutingScript);
        Process p;
        try {
            p = pb.start();
        } catch (Exception e) {
            Debug.redDebug("Error with executing routing_setup.sh");
        }
    }

}
