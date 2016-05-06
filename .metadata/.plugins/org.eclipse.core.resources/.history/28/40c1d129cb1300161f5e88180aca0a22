package lib;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;

/**
 * Instance that defines the range of IP addresses as well as
 * the gateway address for Subnet
 * 
 * Kind of do DHCP stuff as well for the subnet
 * 
 * @author TAISHI
 *
 */
public class SubnetAddress {
	private Inet4Address subnetAddress;
	private int mask;

	//TODO not yet implemented at all
	private Inet4Address gatewayAddress;
	
	// list of IP addresses that are already in use
	private List<Inet4Address> usedAddresses;

	
	/**
	 * Creates a SubnetAddress instance
	 * -> can specify subnet address with mask as follows
	 * 
	 * In an example case of 10.1.1.0/24
	 * @param subnetAddress - should be 10.1.1.0 (network address)
	 * @param mask - should be 24
	 * 
	 * This automatically sets as following:
	 * + network 10.1.1.0
	 * + netmask 255.255.255.0
	 * + broadcast 10.1.1.255
	 * + gateway 10.1.1.1
	 * 
	 * Reference: http://stackoverflow.com/questions/4209760/validate-an-ip-address-with-mask
	 */
	public SubnetAddress(Inet4Address subnetAddress, int mask) {
		this.subnetAddress = subnetAddress;
		this.mask = mask;
		this.usedAddresses = new ArrayList<Inet4Address>();
	}
	
	/* APIs */
	
	//TODO Implement

	/**
	 * Get network address
	 * For a subnet of 10.1.1.0/24, it would be 10.1.1.0
	 */
	public Inet4Address getNetworkAddress() {
		return null;
	}
	
	/**
	 * Get netmask
	 * For a subnet of 10.1.1.0/24, it would be 255.255.255.0
	 */
	public Inet4Address getNetMask() {
		return null;
	}
	
	
	/**
	 * Get broadcast address
	 * For a subnet of 10.1.1.0/24, it would be 10.1.1.255
	 */
	public Inet4Address getBroadcastAddress() {
		return null;
	}
	
	/**
	 * Get gateway address
	 * For a subnet of 10.1.1.0/24, it would be 10.1.1.1
	 */
	public Inet4Address getGatewayAddress() {
		return null;
	}
	
	
	
	
	/**
	 * Validate if a given IP address is in this subnet
	 * @return true if a given IP addr is in this subnet, false otherwise
	 */
	public boolean isInSubnet(Inet4Address address) {
		byte[] sb = subnetAddress.getAddress();
		int subnet_int = ((sb[0] & 0xFF) << 24) |
						((sb[1] & 0xFF) << 16) |
						((sb[2] & 0xFF) << 8)  |
						((sb[3] & 0xFF) << 0);
		
		byte[] b = address.getAddress();
		int address_int = ((b[0] & 0xFF) << 24) |
						((b[1] & 0xFF) << 16) |
						((b[2] & 0xFF) << 8)  |
						((b[3] & 0xFF) << 0);
		
		int bitmask = -1 << (32 - mask);
		
		if ((subnet_int & bitmask) == (address_int & bitmask)) {
			return true;
		}else {
			return false;
		}
	}

	/**
	 * Finds an available IP address and register it 
	 * @return A newly registered IP address on success; null otherwise
	 */
	public Inet4Address assignNewIPAddress() {
		//TODO find an available IP address and register it to usedAddresses
		return null;
	}




}
