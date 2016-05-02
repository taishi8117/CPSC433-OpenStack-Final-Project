package project;

import object.Network;
import object.Subnet;

public class APIHandler {
	private Controller controller;
	
	/**
	 * Creates a new tenant and network. Should be called when the corresponding API is called
	 * @return long - tenantID
	 */
	public long createNewTenantAndNetwork(String networkName) throws Exception {
		long tenantID = controller.registerNewTenant();

		if (controller.registerNewNetwork(tenantID, networkName) == null) {
			//error registering!!!!!
			throw new Exception();
		}
		return tenantID;
	}
	
	
	/**
	 * Creates a new subnet for a specified tenantID and networkID.
	 * Should be called when the corresponding API is called
	 * 
	 * @return long - subnet ID that was created and registered
	 * @throws Exception - when there is no associated network from given IDs
	 */
	public long createNewSubnet(long tenantID, long networkID, String domainName) throws Exception {
		Network network = controller.getNetworkFromID(tenantID, networkID);
		if (network == null) {
			//error finding network!!!!
			throw new Exception();
		}
		long subnetID = network.registerNewSubnet(domainName);
		
		return subnetID;
	}
	
	/**
	 * Creates a new server for specified IDs
	 * Should be called when the corresponding API is called
	 * @return long - server ID that was created and registered
	 * @throws Exception - when there is an errorrrrr
	 */
	public long createNewServer(long tenantID, long networkID, long subnetID, String serverName) throws Exception {
		Network network = controller.getNetworkFromID(tenantID, networkID);
		if (network == null) {
			//error finding network!!!!
			throw new Exception();
		}

		Subnet subnet = network.getSubnetFromID(subnetID);
		if (subnet == null) {
			//error finding subnet!!!!
			throw new Exception();
		}
		
		//TODO make sure serverName is unique

		
		long serverID = subnet.registerNewServer(serverName);
		return serverID;
	}
	
}
