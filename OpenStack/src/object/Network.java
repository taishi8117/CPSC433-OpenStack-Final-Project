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

	public NetworkState state;
	public String networkName;
	
	public Controller controller;
	
	// maps subnetID to Subnet instance
	private HashMap<Long, Subnet> subnetMap;	
	
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
	
	/**
	 * Registers a new subnet to this network.
	 * It automatically finds an available range of IP address, which will be
	 * assigned to this subnet
	 * @return subnet ID that was registered
	 */
	public long registerNewSubnet() {
		long subnetID;
		
		// randomly generate subnet ID until it finds a new one
		do {
			subnetID = controller.randomGen.nextLong();
		} while (subnetMap.containsKey(subnetID));
		
		// get currently available subnet address space
		SubnetAddress subnetAddr = controller.getAvailableSubnetAddr();
		
		Subnet subnet = new Subnet(this, subnetID, subnetAddr);
		
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
	

}
