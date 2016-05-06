package lib;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import project.Controller;

/**
 * Instance that defines the range of IP addresses as well as
 * the gateway address for Subnet
 * This class is also responsible for making a bridge on the host machine for the subnet
 * 
 * 
 * NOTE that in this class, Inet4Address DOES NOT use hostname look up or blocking operation
 * Every Inet4Address this class ever uses needs to be created by using {@code InetAddress.getByAddress(ipAddr)} 
 * where ipAddr is byte[] specifying 4 parts of IP addresses
 * 
 * Reference: http://stackoverflow.com/questions/4209760/validate-an-ip-address-with-mask
 * @author TAISHI
 *
 */
public class SubnetAddress {
	public final Inet4Address subnetAddress;
	private int mask;

	// list of IP addresses that are already in use
	private List<Inet4Address> usedAddresses;
	
	private Controller controller;
	private String scriptDirectory;
	
	// ID of the parent subnet
	private long subnetID;
	
	// Bridge name for the bridge on the host for this subnet address
	public String bridgeName;

	
	/**
	 * Creates a SubnetAddress instance
	 * -> can specify subnet address with mask as follows
	 * 
	 * In an example case of 10.1.1.0/24
	 *    This automatically sets as following:
	 *    + network 10.1.1.0
	 *    + netmask 255.255.255.0
	 *    + broadcast 10.1.1.255
	 *    + gateway 10.1.1.1
	 * 
	 * @param subnetAddress - should be 10.1.1.0 (network address)
	 * @param mask - should be 24
	 * 
	 * 
	 * @throws Exception - happens when creating bridge on host machine failed or subnetAddress was illegal length
	 */
	public SubnetAddress(Controller controller, Inet4Address subnetAddress, int mask, long subnetID, String bridgeName) throws Exception {
		this.controller = controller;
		this.subnetAddress = subnetAddress;
		this.mask = mask;
		this.usedAddresses = new ArrayList<Inet4Address>();
		
		//add broadcast and gateway to usedAddress
		this.usedAddresses.add(getNetworkAddress());
		this.usedAddresses.add(getGatewayAddress());
		this.usedAddresses.add(getBroadcastAddress());
		
		this.subnetID = subnetID;
		this.bridgeName = bridgeName;

		this.scriptDirectory = controller.configMap.get("LocScript");
		
		// create bridge on host machine
		if (createBridgeOnHost() == 0) {
			throw new Exception("Error when creating a bridge on host for the subnet: " + subnetID);
		}
	}
	
	/* APIs */
	
	/**
	 * Get network address
	 * For a subnet of 10.1.1.0/24, it would be 10.1.1.0
	 */
	public Inet4Address getNetworkAddress() {
		return subnetAddress;
	}
	
	/**
	 * Get netmask
	 * For a subnet of 10.1.1.0/24, it would be 255.255.255.0
	 */
	public Inet4Address getNetMask() {
		//TODO hardcoding it with assumption that it's 255.255.255.0
		byte[] netmask = new byte[]{(byte) 255,(byte) 255,(byte) 255,0};
		try {
			return (Inet4Address) Inet4Address.getByAddress(netmask);
		} catch (UnknownHostException e) {
			//Weird shouldn't happen
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Get broadcast address
	 * For a subnet of 10.1.1.0/24, it would be 10.1.1.255
	 */
	public Inet4Address getBroadcastAddress() {
		//TODO hardcoding it with assumption that mask == 24
		byte[] rawBroadcastAddr = Arrays.copyOf(subnetAddress.getAddress(), 4);
		rawBroadcastAddr[3] = (byte) 255;
		
		try {
			return (Inet4Address) Inet4Address.getByAddress(rawBroadcastAddr);
		} catch (UnknownHostException e) {
			//Weird shouldn't happen
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Get gateway address
	 * For a subnet of 10.1.1.0/24, it would be 10.1.1.1
	 */
	public Inet4Address getGatewayAddress() {
		byte[] rawGatewayAddr = Arrays.copyOf(subnetAddress.getAddress(), 4);
		rawGatewayAddr[3] = 1;

		try {
			return (Inet4Address) Inet4Address.getByAddress(rawGatewayAddr);
		} catch (UnknownHostException e) {
			//Weird shouldn't happen
			e.printStackTrace();
			return null;
		}
	}
	
	private boolean isAddressAlreadyRegistered(Inet4Address address) {
		if (usedAddresses.contains(address)) {
			return true;
		}
		return false;
		
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
	 * Finds an available IP address for a server and register it 
	 * @return A newly registered IP address on success; null otherwise
	 */
	public Inet4Address assignNewIPAddress() {
		byte[] rawNewAddr = Arrays.copyOf(subnetAddress.getAddress(), 4);

		//TODO find an available IP address and register it to usedAddresses
		//TODO hardcoding 256 assuming that mask == 24; all trying ones would be in this subnet
		// starting from 2 since 0 and 1 are assumed to be used already (for network and gateway)
		for (int i = 2; i < 256; i++) {
			rawNewAddr[3] = (byte) i;
			Inet4Address newAddr;

			try {
				newAddr = (Inet4Address) Inet4Address.getByAddress(rawNewAddr);
			} catch (UnknownHostException e) {
				// shouldn't happen
				continue;
			}

			if (!isAddressAlreadyRegistered(newAddr)) {
				//address not registered yet -> let's register
				usedAddresses.add(newAddr);
				return newAddr;
			}
		}
		return null;
	}
	
	/**
	 * Deregisters IP Address from this subnet
	 * @param address
	 */
	public void deregisterIPAddress(Inet4Address address) {
		usedAddresses.remove(address);
	}
	
	/**
	 * Destroys all resources associated with this SubnetAddress
	 * This removes the bridge on the host machine that is associated with this subnet
	 * 
	 * SHOULD BE CALLED AFTER all the servers associated with this subnet
	 * are destroyed
	 */
	public void destroySubnetAddress() {
		//TODO remove bridge for this subnet
		
		// remove this from controller
		controller.deregisterSubnetAddrFromController(this);
	}
	
	/**
	 * Creates a new bridge on the host machine for this subnet, using create_subnet.sh script
	 * # Usage:
     *   # ./create_subnet.sh networkCfg
     * 	  * networkCfg : Bridge interface configuration for libvirt (xml)
	 * 
	 * @return 1 on success, 0 on error
	 */
	private int createBridgeOnHost() {
		
		//create bridge configuration (xml file)
		String bridgeCfgPath = createBridgeConfig();
		if (bridgeCfgPath == null) {
			return 0;
		}
		
		String locCreateScript = scriptDirectory + "/create_subnet.sh";
		
		ProcessBuilder pb = new ProcessBuilder(locCreateScript, bridgeCfgPath);

		try {
			Process p = pb.start();
			//TODO NEED TO BE ASYNCHRONOUSS
			int exitStatus = p.waitFor();
			if (exitStatus != 0) {
				Debug.redDebug("Error while executing create_subnet.sh with exit status: " + exitStatus);
				return 0;
			}else {
				return 1;
			}
			
		} catch (Exception e) {
			// error executing this script
			Debug.redDebug("Error with create bridge on host machine -- couldn't run the script");
			return 0;
		}
	}
		
	/**
	 * Creates a new bridge configuration temporary file
	 * This is a helper method for createBridgeOnHost()
	 * 
	 * Format of configuration is like following:
     *   <network>
     *     <name>${SUBNET_ID}</name>
     *     <bridge name="${BRIDGE_NAME}"/>
     *     <forward/>
     *     <ip address="192.168.101.1" netmask="255.255.255.0">
     *     </ip>
     *   </network>
     *   
     * NOTE that bridge name needs to be unique, and should be confirmed by controller
     * 
     * @return String - path to the created config in String on success, or null on error
	 */
	private String createBridgeConfig() {
		StringBuilder bridgeCfgBuilder = new StringBuilder();
		bridgeCfgBuilder.append("<network>\n")
						.append("  <name>" + subnetID + "</name>\n")
						.append("  <bridge name=\"" + bridgeName + "\"/>\n")
						.append("  <forward/>\n")
						.append("  <ip address=\"" + getGatewayAddress().getHostAddress() +"\" netmask=\"" + getNetMask().getHostAddress() + "\">\n")
						.append("  </ip>\n")
						.append("</network>\n");
		
		String location = "/tmp/" + "brcfg_" + subnetID + ".xml";
		
		try {
			// create metadata file at location
			File file = new File(location);
			
			if (!file.exists()) {
				file.createNewFile();
			}
			
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(bridgeCfgBuilder.toString());
			bw.close();

			return location;
		} catch (Exception e) {
			if (Debug.IS_DEBUG) {
				e.printStackTrace();
			}
			return null;
		}
	}
}
