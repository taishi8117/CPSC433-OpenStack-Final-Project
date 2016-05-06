package project;

import object.Network;
import object.Subnet;
import object.VirtualServer;
import java.net.HttpURLConnection;

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
	 * Delete a specified tenant and its associated resources
	 */
	public long deleteTenant(long tenantID) {
		return 0;
		
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
	 * Creates a new port for a specified network.
	 * Should be called when the corresponding API is called
	 * @param number - port number desired (optional)
	 *
	 * @return long - port that was created and registered, or -1 if the port is already registered
	 * @throws Exception - when there is no associated network from given IDs
	 */
	public long createNewPort(long tenantID, long networkID, int number) throws  Exception{
		Network network = controller.getNetworkFromID(tenantID, networkID);
		if (network == null) {
			//error finding network!!!!
			throw new Exception();
		}
		long portNum = network.registerNewPort(number);

		return portNum;
	}

	// Create port with unspecified port number. (creates random number)
	public int createNewPort(long tenantID, long networkID) throws  Exception{
		Network network = controller.getNetworkFromID(tenantID, networkID);
		if (network == null) {
			//error finding network!!!!
			throw new Exception();
		}
		int portNum = network.registerNewPort(0);

		return portNum;
	}
	
	public String listPort() {
		return null;
	}
	
	public String getPortDetails() {
		return null;
	}
	
	public long updatePort() {
		return 0;
	}
	
	public long deletePort() {
		return 0;
	}

	
	/**
	 * Creates a new server for specified IDs
	 * Should be called when the corresponding API is called
	 * @return long - server ID that was created and registered
	 * @throws Exception - when there is an errorrrrr
	 */
	public long createNewServer(long tenantID, long networkID, long subnetID, String serverName, String password) throws Exception {
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

		
		long serverID = subnet.registerNewServer(serverName, password);
		return serverID;
	}
	

	public String listServers(long tenantID, long networkID, long subnetID) {
		return null;
	}
	
	/**
	 * Returns the server details in XML form
	 */
	public String getServerDetails(long tenantID, long networkID, long subnetID, long serverID) {
		
		return null;
	}
	
	public int startServer() {
		return 1;
	}
	
	public int stopServer() {
		return 1;
	}
	
	
	
	
}
