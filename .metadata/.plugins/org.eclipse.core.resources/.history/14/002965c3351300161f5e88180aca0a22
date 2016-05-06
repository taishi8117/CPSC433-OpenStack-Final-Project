package lib;

import java.net.Inet4Address;

/**
 * Virtual Network Interface --- can be owned by server or controller
 * It maintains IP address, MAC address, etc. who knows what
 * @author TAISHI
 *
 */
public class VirtualNIC {
	
	// IP address
	public Inet4Address ipAddr;
	
	// SubnetAddress instance of subnet to which this VNIC belong
	public SubnetAddress subnetAddr;
	
	//TODO deal with MAC address
	
	/**
	 * Creates Data VNIC instance with subnet address
	 * @param subnetAddr
	 */
	public VirtualNIC(SubnetAddress subnetAddr) {
		this.subnetAddr = subnetAddr;
	}
	
	
	/**
	 * Creates Control VNIC instance
	 */
	public VirtualNIC() {
		// TODO Auto-generated constructor stub
	}
	
	
	/**
	 * Should be called before any non-constructor methods are called
	 * @param address
	 */
	public void assignIPAddr(Inet4Address address) {
		this.ipAddr = address;
	}


}
