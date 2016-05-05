package object;

import java.util.HashMap;

import lib.SubnetAddress;
import project.Controller;

public class Network {
	enum NetworkState {
		UP,
		DOWN
	}

	// Tenant ID to which this network belong
	public long tenantID;
	public long networkID;

	private NetworkState state;
	public String networkName;
	
	private Controller controller;
	
	// maps subnetID to Subnet instance
	private HashMap<Long, Subnet> subnetMap;

	// maps portNums to Port instance
	private HashMap<Long, Port> portMap;

	/**
	 * !!
	 * @param networkID 
	 */
	public Network(Controller controller, String networkName, long tenantID, long networkID) {
		this.controller = controller;
		this.tenantID = tenantID;
		this.networkID = networkID;
		this.networkName = networkName;
		this.state = NetworkState.UP;
		this.subnetMap = new HashMap<Long, Subnet>();
	}


	//Deactivates this network
	public void deactivate(){
		this.state = NetworkState.DOWN;
	}
	
	/**
	 * Registers a new subnet to this network.
	 * It automatically finds an available range of IP address, which will be
	 * assigned to this subnet
	 * @return subnet ID that was registered
	 */
	public long registerNewSubnet(String domainName) {
		long subnetID;
		
		// randomly generate subnet ID until it finds a new one
		do {
			subnetID = controller.randomGen.nextLong();
		} while (subnetMap.containsKey(subnetID));
		
		// get currently available subnet address space
		SubnetAddress subnetAddr = controller.getAvailableSubnetAddr();
		
		Subnet subnet = new Subnet(controller, this, subnetID, subnetAddr, domainName);
		
		subnetMap.put(subnetID, subnet);
		
		return subnetID;
	}
	
	/**
	 * Get the associated Subnet instance from subnetID
	 * @return Associated Subnet instance on success; otherwise null
	 */
	public Subnet getSubnetFromID(long subnetID) {
		Subnet subnet = subnetMap.get(subnetID);
		if (subnet == null) {
			return null;
		}

		return subnet;
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
