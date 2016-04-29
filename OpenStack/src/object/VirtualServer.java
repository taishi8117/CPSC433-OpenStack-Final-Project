package object;

import java.net.Inet4Address;

import lib.VirtualNIC;

public class VirtualServer {
	public long serverID;
	
	public String serverName;
	private VirtualNIC dataNIC;
	private VirtualNIC controlNIC;		//need IP address???

	// Subnet to which this server belong
	private Subnet parentSubnet;
	
	//TODO I have no idea about ports!!!!
	
	public VirtualServer(Subnet parentSubnet, long serverID, String serverName) {
		this.parentSubnet = parentSubnet;
		this.serverID = serverID;
		this.serverName = serverName;
		
		//TODO configuration of virtual NIC
		this.dataNIC = new VirtualNIC();
		this.controlNIC = new VirtualNIC();
	}
	
	/**
	 * Assigns an IP address to data NIC. Should be called when dealing with DHCP
	 * @param address
	 */
	public void assignDataIPAddr(Inet4Address address) {
		this.dataNIC.assignIPAddr(address);
	}


}
