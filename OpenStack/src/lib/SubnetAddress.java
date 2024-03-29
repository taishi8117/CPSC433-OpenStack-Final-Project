package lib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import project.Controller;

/**
 * Instance that defines the range of IP addresses as well as the gateway address for Subnet
 * This class is also responsible for making a bridge on the host machine for the subnet
 * 
 * 
 * NOTE that in this class, Inet4Address DOES NOT use hostname look up or blocking operation
 * Every Inet4Address this class ever uses needs to be created by using {@code InetAddress.getByAddress(ipAddr)} 
 * where ipAddr is byte[] specifying 4 parts of IP addresses
 * 
 * This class ASSUMES that it takes at least 500ms to create a bridge on the host machine.
 * Thus, one cannot create a virtual server within the subnet until after at least 500ms.
 * The state is checked every 1 second whether the bridge is properly created
 * 
 * Reference: http://stackoverflow.com/questions/4209760/validate-an-ip-address-with-mask
 * @author TAISHI
 *
 */
public class SubnetAddress {
	enum BridgeState {
		NON_EXISTENT,
		CREATED,
		RUNNING,
	}
	

	public final Inet4Address subnetAddress;
	private int mask;
	private BridgeState state;
	

	// list of IP addresses that are already in use
	private List<Inet4Address> usedAddresses;
	
	private Controller controller;
	private String scriptDirectory;
	
	// ID of the parent subnet
	private final long subnetID;
	
	// Bridge name for the bridge on the host for this subnet address
	public String bridgeName;

	// Map of the most recent executed processes for each key for this server
	// Key needs to be one of the following
	// "create" : create_subnet.sh process
	// "destroy" : destroy_subnet.sh process
	private HashMap<String, Process> processMap;
	
	// synchronizing state with the host system
	private static final int DEFAULT_CHECK_INTERVAL = 1000;
	Timer updateTimer;
	UpdateStateTask updateTask;
	
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
		this.state = BridgeState.NON_EXISTENT;
		
		//add broadcast and gateway to usedAddress
		this.usedAddresses.add(getNetworkAddress());
		this.usedAddresses.add(getGatewayAddress());
		this.usedAddresses.add(getBroadcastAddress());
		
		this.subnetID = subnetID;
		this.bridgeName = bridgeName;

		this.scriptDirectory = controller.configMap.get("LocScript");
		this.processMap = new HashMap<>();
		
		this.updateTimer = new Timer();
		this.updateTask = new UpdateStateTask();
		
		// create bridge on host machine
		if (createBridgeOnHost() == 0) {
			Debug.redDebug("createBridgeOnHost() FAILED!");
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
		// remove bridge for this subnet
		if (destroyBridgeOnHost() == 0) {
			//TODO error handling?
		}
		
		// remove this from controller
		controller.deregisterSubnetAddrFromController(this);
	}
	
	/**
	 * Creates a new bridge on the host machine for this subnet, using create_subnet.sh script
	 * Also, starts the {@code updateTimer} so as to synchronize the state of the bridge
	 * with the host system
	 * 
     *   Usage:
     *     # ./create_subnet.sh
     *   List of Environment Variables
     *   * CFGFILE : Bridge interface configuration for libvirt, should be in the form of "brcfg_" + subnetID + ".xml" 
     *   * SUBNWNAME : The name of the subnet created, which should be subnetID
     *   
     *   THIS DOES NOT GUARANTEE THAT BRIDGE IS PROPERLY WORKING UNTIL {@code getSubnetAddressDetail} 
     *   IS CALLED AND THE STATE IS CONFIRMED
	 * 
	 * @return 1 on success, 0 on error
	 */
	private int createBridgeOnHost() {
		
		//create bridge configuration (xml file)
		File bridgeCfgFile = createBridgeConfig();
		if (bridgeCfgFile == null) {
			return 0;
		}
		
		String locCreateScript = scriptDirectory + "/create_subnet.sh";
		
		ProcessBuilder pb = new ProcessBuilder("/bin/bash", locCreateScript);
		pb.environment().put("CFGFILE", bridgeCfgFile.getAbsolutePath());
		pb.environment().put("SUBNWNAME", Long.toString(subnetID));
		if (Debug.IS_DEBUG) {
			pb.redirectOutput(Redirect.INHERIT);
			pb.redirectError(Redirect.INHERIT);
		}

		Process p;
		try {
			p = pb.start();
			
			processMap.put("create", p);
			state = BridgeState.CREATED;
			
			// start the timer
			updateTimer.schedule(updateTask, 500, DEFAULT_CHECK_INTERVAL);
			return 1;
		} catch (Exception e) {
			Debug.redDebug("Error with executing create_subnet.sh");
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
     * @return File - File instance of the created config in String on success, or null on error
	 */
	private File createBridgeConfig() {
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

			return file;
		} catch (Exception e) {
			if (Debug.IS_DEBUG) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	/**
	 * Destroys a bridge on the host machine for this subnet, using destroy_subnet.sh
     *   Usage:
     *     # ./destroy_subnet.sh
     *   List of Environment Variables
     *   * SUBNWNAME : The name of the subnet to destroy, which should be subnetID
     *   
	 * @return 1 on success, 0 on error
	 */
	private int destroyBridgeOnHost() {
		//should still run, it's safer and more resource-friendly to run even when it's non-existent
		//if (state == BridgeState.NON_EXISTENT) {
		//	Debug.debug("destroyBridgeOnHost() called when subnet doesn't exist");
		//	return 0;
		//}
		updateTask.destroyCalled();
		updateTimer.cancel();

		String locDestroyScript = scriptDirectory + "/destroy_subnet.sh";
		
		ProcessBuilder pb = new ProcessBuilder(locDestroyScript, Long.toString(subnetID));
		pb.environment().put("SUBNWNAME", Long.toString(subnetID));
		
		Process p;
		try {
			p = pb.start();
			processMap.put("destroy", p);
			
			state = BridgeState.NON_EXISTENT;
			return 1;
		} catch (Exception e) {
			Debug.redDebug("Error with executing destroy_subnet.sh");
			return 0;
		}
	}
	
	/**
	 * Get the subnet address detail. Needs to be run before a user can attempt to
	 * create a server within the subnet. This confirms with the system if 
	 * the bridge for this subnet is properly created, and changes the state accordingly.
	 * 
	 * Returns HashMap object about the subnet, with following keys 
	 * [== if the bridge was not created properly (error) ==]
	 * + state : "non-existent"
	 * 
	 * [== if the bridge was created properly, and the subnet is properly running ==]
	 * + state : "running"
	 * + network_address : network address of this subnet
	 * + netmask : netmask of this subnet
	 * + broadcast_address : broadcast address of this subnet
	 * + gateway_address : gateway address of this subent
	 * + num_ip : number of IP addresses already registered (incl. network, broadcast, gateway)
	 * + bridge : name of the bridge for this subnet
	 */
	public HashMap<String, String> getSubnetAddressDetail() {
		HashMap<String, String> subnetDetailMap = new HashMap<>();
		if (state == BridgeState.NON_EXISTENT) {
			subnetDetailMap.put("state", "non-existent");
		}
		
		// checks if this subnet is properly running
		if (state == BridgeState.RUNNING) {
			subnetDetailMap.put("state", "running");
			subnetDetailMap.put("network_address", getNetworkAddress().getHostAddress());
			subnetDetailMap.put("netmask", getNetMask().getHostAddress());
			subnetDetailMap.put("broadcast_address", getBroadcastAddress().getHostAddress());
			subnetDetailMap.put("gateway_address", getGatewayAddress().getHostAddress());
			subnetDetailMap.put("num_ip", Integer.toString(usedAddresses.size()));
			subnetDetailMap.put("bridge", bridgeName);
		}else {
			subnetDetailMap.put("state", "non-existent");
		}
		
		return subnetDetailMap;
	}

	public boolean isRunning() {
		if (state == BridgeState.RUNNING) {
			return true;
		} else {
			return false;
		}
	}
	
	
	/**
	 * Callback function for when the bridge was in fact non-existent,
	 * meaning that there is an ERRORR
	 */
	private void foundNonExistent() {
		state = BridgeState.NON_EXISTENT;
	}
	
	/**
	 * Callback function for when the bridge was found to be properly working
	 */
	private void foundRunning() {
		if (state == BridgeState.RUNNING || state == BridgeState.CREATED) {
			state = BridgeState.RUNNING;
		}
	}
	
	
	/**
	 * Helper class for SubnetAddress that updates the state by
	 * checking the bridge state with the host system, using check_bridge.sh script
	 * 
	 * Note that this task is blocking, since it waits for the result of the script
	 * 
	 * When state == CREATED, this state doesn't change to NON-EXISTENT for the first 5 calls (5 seconds)
	 * even if active network isn't found
	 * 
	 * The output of check_bridge.sh is expected as following:
     *    Name                 State      Autostart     Persistent
     *   ----------------------------------------------------------
     *    {subnetId}          active     no            yes
	 * 
	 * @author TAISHI
	 */
	class UpdateStateTask extends TimerTask {
		boolean isDestroyed;
		ProcessBuilder pb;
		private final Matcher activeRegex;
		private int numberCalls;
		
		public UpdateStateTask() {
			numberCalls = 0;
			isDestroyed = false;
			String locCheckScript = scriptDirectory + "/check_bridge.sh";
			pb = new ProcessBuilder("/bin/bash", locCheckScript);
			
			activeRegex = Pattern.compile("\\s*" + subnetID + "\\s+([^\\s]*).*").matcher("");
		}

		@Override
		public void run() {
			numberCalls++;
			if (isDestroyed) {
				foundNonExistent();
				return;
			}
			
			try {
				Process p = pb.start();
				// blocking operation
				BufferedReader buffer = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;

				while ((line = buffer.readLine()) != null) {
					//Debug.debug("In UpdateState: " + line);
					if (activeRegex.reset(line).find()) {
						// found the network name
						String status = activeRegex.group(1).trim();

						if (status.equals("active")) {
							// it was active!
							if (!isDestroyed) {
								foundRunning();
							}else {
								foundNonExistent();
							}
							return;
						}else {
							//not active
							if (state == BridgeState.CREATED && numberCalls < 6) {
								return;
							}else {
								foundNonExistent();
								return;
							}
						}
					}
				}
				
				// not found such network
				if (state == BridgeState.CREATED && numberCalls < 6) {
					return;
				}else {
					foundNonExistent();
					return;
				}
			} catch (Exception e) {
				Debug.redDebug("Error executing check_bridge.sh");
			}
		}
		
		public void destroyCalled() {
			isDestroyed = true;
		}
	}
	
}

