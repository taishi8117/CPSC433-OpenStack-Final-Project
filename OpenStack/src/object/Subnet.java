package object;

import java.net.Inet4Address;
import java.util.HashMap;

import lib.LinkLayerHandler;
import lib.SubnetAddress;
import project.Controller;

public class Subnet {
	public long subnetID;
	
	// network to which this subnet belongs
	private Network parentNetwork;	
	
	// controller
	private Controller controller;
	
	// maps serverID to VirtualServer instance
	private HashMap<Long, VirtualServer> serverMap;
	
	// maintains the range of subnet address and gateway address
	public SubnetAddress subnetAddress;
	
	// this manages LinkLayer stuff
	private LinkLayerHandler linkHandler;
	
	// domain name for this subnet (e.g. network.tenantName ?)
	public String domainName;
	

	

	/**
	 * Create Subnet instance. Should be called when createSubnet() API is called
	 */
	public Subnet(Controller controller, Network parentNetwork, long subnetID, SubnetAddress subnetAddr, String domainName) {
		this.controller = controller;
		this.parentNetwork = parentNetwork;
		this.subnetID = subnetID;
		this.serverMap = new HashMap<Long, VirtualServer>();
		this.subnetAddress = subnetAddr;
		this.domainName = domainName;
		
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
			serverID = controller.randomGen.nextLong();
		} while (serverMap.containsKey(serverID));
		
		// registers a new virtual server
		String networkCfg = getNetworkCfg();
		//TODO this needs to use more specified contstructor later
		VirtualServer server = new VirtualServer(controller, this, serverID, serverName, networkCfg);
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
	
	/**
	 * Get network configuration for virt-install
	 * Cannot contain space in the returned string
	 * 
	 * e.g. -- "bridge=virbr0,model=virtio"
	 * @return
	 */
	public String getNetworkCfg() {
		return "bridge=virbr0,model=virtio";
	}
	
}
