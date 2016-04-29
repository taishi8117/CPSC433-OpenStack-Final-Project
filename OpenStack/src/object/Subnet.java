package object;

import java.net.Inet4Address;
import java.util.HashMap;

import lib.LinkLayerHandler;
import lib.SubnetAddress;

public class Subnet {
	public long subnetID;
	
	// network to which this subnet belongs
	private Network parentNetwork;	
	
	// maps serverID to VirtualServer instance
	private HashMap<Long, VirtualServer> serverMap;
	
	// maintains the range of subnet address and gateway address
	private SubnetAddress subnetAddress;
	
	// this manages LinkLayer stuff
	private LinkLayerHandler linkHandler;
	

	/**
	 * Create Subnet instance. Should be called when createSubnet() API is called
	 */
	public Subnet(Network parentNetwork, long subnetID, SubnetAddress subnetAddr) {
		this.parentNetwork = parentNetwork;
		this.subnetID = subnetID;
		this.serverMap = new HashMap<Long, VirtualServer>();
		this.subnetAddress = subnetAddr;
		
		this.linkHandler = new LinkLayerHandler();

	}
	
	
	/**
	 * Registers a new server to this subnet.
	 * It automatically finds an available IP address and assigns it (DHCP)
	 * @return server ID that was registered
	 * @throws Exception - when IP address not assignable
	 */
	//TODO error handling when there is no available IP??
	public long registerNewServer(String serverName) throws Exception {
		long serverID;
		// randomly generate subnet ID until it finds a new one
		do {
			serverID = parentNetwork.controller.randomGen.nextLong();
		} while (serverMap.containsKey(serverID));
		
		// registers a new virtual server
		VirtualServer server = new VirtualServer(this, serverID, serverName);
		serverMap.put(serverID, server);

		// assign an available IP address to the newly created server
		Inet4Address ipAddr = subnetAddress.assignNewIPAddress();
		if (ipAddr == null) {
			//IP address not assignable!!!!!!
			throw new Exception();
		}
		server.assignDataIPAddr(ipAddr);
		
		return serverID;

	}

	
}
