package object;

import java.net.Inet4Address;

import lib.Debug;
import lib.VirtualNIC;
import project.Controller;

public class VirtualServer {
	public long serverID;
	
	// Subnet to which this server belong
	private Subnet parentSubnet;
	
	private Controller controller;
	
	
	// Constant
	private static final int DEFAULT_DISKSIZE = 4;
	private static final int DEFAULT_NUMCPU = 2;
	private static final int DEFAULT_MEM = 1024;
	

	public String serverName;
	private VirtualNIC dataNIC;
	private VirtualNIC controlNIC;		//need IP address???
	
	// Server config
	private int diskSize;
	private int numCpu;
	private int memSize;

	private String networkCfg;
	
	
	
	
	//TODO I have no idea about ports!!!!
	
	public VirtualServer(Controller controller, Subnet parentSubnet, long serverID, String serverName, String networkCfg) {
		this(controller, parentSubnet, serverID, serverName, networkCfg, DEFAULT_DISKSIZE, DEFAULT_NUMCPU, DEFAULT_MEM);
	}
	
	public VirtualServer(Controller controller, Subnet parentSubnet, long serverID, 
			String serverName, String networkCfg, int diskSize, int numCpu, int memSize) {
		this.controller = controller;
		this.parentSubnet = parentSubnet;
		this.serverID = serverID;
		this.serverName = serverName;
		
		this.networkCfg = networkCfg;
		this.diskSize = diskSize;
		this.numCpu = numCpu;
		this.memSize = memSize;
		
		//TODO configuration of virtual NIC
		this.dataNIC = new VirtualNIC(parentSubnet.subnetAddress);
		this.controlNIC = new VirtualNIC();
	}

	
	/**
	 * Assigns an IP address to data NIC. Should be called when dealing with DHCP
	 * @param address
	 */
	public void assignDataIPAddr(Inet4Address address) {
		//TODO error handling
		this.dataNIC.assignIPAddr(address);
	}
	
	
	/**
	 * Creates a virtual machine using create_server.sh script
	 * 
	 * # create_server.sh : script to create a virtual server using Ubuntu 14.04 cloud image on kvm
	 *	 Usage:
	 *		# ./create_server.sh host domain disksize numcpu mem network metadata userdata
	 *	 * host: the hostname of the server (needs to be unique) == serverName
	 *	 * domain: the domain name in which the server is created
	 *	 * disksize: the disk size for the server in GB
	 *	 * numcpu: the number of CPUs allocated for the server
	 *	 * mem: the amount of memory allocated for the server in MB
	 *	 * network: the network configuration (VNIC) with no space
	 *	 * metadata: the location of meta-data file
	 *	 * userdata: the location of user-data file
	 *
	 *	@exception when error creating a virtual machine
	 */
	public void createVirtualMachine() throws Exception {
		//TODO error checking on arguments for script
		String scriptDirectory = controller.configMap.get("LocScript");
		String locCreateScript = scriptDirectory + "/create_server.sh";
		
		String locMetadata = "/tmp/metadata-" + serverName.trim() + Long.toString(serverID);
		String locUserdata = "/tmp/userdata-" + serverName.trim() + Long.toString(serverID);
		
		// create metadata and userdata
		if (createMetadata(locMetadata) != 1 || createUserdata(locUserdata) != 1) {
			//error!!!!!!
			throw new Exception("Error when creating metadata/userdata");
		}
				
		// need to change to string before passing them as arguments
		ProcessBuilder pb = new ProcessBuilder(locCreateScript, serverName, parentSubnet.domainName,
												Integer.toString(diskSize), Integer.toString(numCpu),
												Integer.toString(memSize), networkCfg,
												locMetadata, locUserdata);
		
		Process p = pb.start();
		int exitStatus = p.waitFor();
		if (exitStatus != 0) {
			// error executing this script
			Debug.redDebug("Error with createVirtualMachine, with exit status: " + exitStatus);
			throw new Exception("Error running create_server.sh with exit code: " + exitStatus);
		}
	}
	
	/**
	 * Creates a metadata file for Cloud Init
	 * 
	 * Metadata is a text configuration file like following
	 *	instance-id: iid-${SERVERID};
	 *	network-interfaces: |
	 *	  auto eth0
	 *	  iface eth0 inet static
	 *	  address 192.168.122.10
	 *	  network 192.168.122.0
	 *	  netmask 255.255.255.0
	 *	  broadcast 192.168.122.255
	 *	  gateway 192.168.122.1
	 *	  dns-search ${DOMAIN}
	 *	  dns-nameservers 192.168.122.1
	 *	hostname: ${SERVERNAME}
	 *	local-hostname: ${SERVERNAME}
	 * 
	 * 
	 * @return 1 on success, 0 on error
	 */
	private int createMetadata(String location) {
		String instanceId = "iid-" + serverName + Long.toString(serverID);
		String strIpAddr = dataNIC.ipAddr.getHostAddress();
		String strNtwkAddr = dataNIC.subnetAddr.getNetworkAddress().getHostAddress();
		String strNetmask = dataNIC.subnetAddr.getNetMask().getHostAddress();
		String strBroadcast = dataNIC.subnetAddr.getBroadcastAddress().getHostAddress();
		String strGatewayAddr = dataNIC.subnetAddr.getGatewayAddress().getHostAddress();
		
		//TODO create data


		return 1;
	}
	
	/**
	 * Creates a userdata file for Cloud Init
	 * @return 1 on success, 0 on error
	 */
	private int createUserdata(String location) {
		return 1;
	}
	
	
	


}