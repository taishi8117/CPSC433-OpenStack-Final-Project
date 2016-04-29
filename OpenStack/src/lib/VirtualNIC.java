package lib;

import java.net.Inet4Address;

/**
 * Virtual Network Interface --- can be owned by server or controller
 * It maintains IP address, MAC address, etc. who knows what
 * @author TAISHI
 *
 */
public class VirtualNIC {
	public Inet4Address ipAddr;
	
	//TODO deal with MAC address
	
	public VirtualNIC() {
		// TODO Auto-generated constructor stub
	}
	
	public void assignIPAddr(Inet4Address address) {
		this.ipAddr = address;
	}


}
