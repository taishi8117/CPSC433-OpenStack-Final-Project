package project;

import java.util.HashMap;
import java.util.Random;

import lib.SubnetAddress;
import object.DNS;
import object.Network;
import object.Port;

/**
 * Controller Instance
 * @author TAISHI
 *
 */
public class Controller {
	// global OpenStack configuration
	public HashMap<String, String> configMap;


	// maps tenantID to Hash Map of Network (mapping network ID to Network)
	private HashMap<Long, HashMap<Long,Network>> tenantMap;
	private DNS dnsServer;

	// maps portNums to Port instance
	public HashMap<Integer, Port> portMap;

	// Miscellaneous
	public Random randomGen;


	public Controller(HashMap<String,String> configMap) {
		// TODO Auto-generated constructor stub
		this.tenantMap = new HashMap<Long, HashMap<Long,Network>>();
		this.randomGen = new Random();
		this.dnsServer = new DNS();
		this.configMap = configMap;
	}



	/**
	 * Assigns available private address space for a subset.
	 * Should be called when a new subnet is created to assign
	 * the range of IP address
	 * @return SubnetAddress
	 */
	public SubnetAddress getAvailableSubnetAddr() {
		//TODO need to maintain the range of private IP address that is already used by some other networks
		//TODO return available range of IP address as SubnetAddress instance
		//TODO need to register that
		return null;
	}

	/**
	 * Assigns available port for a virtual server
	 * @return Port
	 */
	public Port getAvailablePort() {
		//TODO need to maintain what ports are available
		//TODO return available port as Port instance
		//TODO need to register that
		return null;
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
	 * @return port number that was registered or -1 if already registered
	 */
	public int registerNewPort(int number) {
		int portNum = number;
		if (portNum == 0){
			// randomly generate subnet ID until it finds a new one
			do {
				portNum = (int) controller.randomGen.nextLong();
			} while (portMap.containsKey(portNum));
		}
		if (portMap.containsKey(portNum)) // specified port number is occupied
			return -1;
		Port port = new Port(number, this);
		return number;
	}


}
