package object;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.Inet4Address;
import java.util.HashMap;


import lib.Debug;
import lib.VirtualNIC;
import project.Controller;

/**
 * Instance that controlls a virtual server (Ubuntu 14.04 Cloud trusty)
 * Note that username is `ubuntu' and password login into ssh is enabled.
 * 
 * @author TAISHI
 *
 */
public class VirtualServer {
	enum ServerState {
		RUNNING,
		SHUTDOWN,
		NON_EXISTENT,	//init or destroyed
		ERROR			//some weird thing happened
	}
	
	public final long serverID;
	private ServerState state;
	
	// Subnet to which this server belong
	private Subnet parentSubnet;
	
	private Controller controller;
	
	
	// Constant
	private static final int DEFAULT_DISKSIZE = 4;
	private static final int DEFAULT_NUMCPU = 2;
	private static final int DEFAULT_MEM = 1024;
	public static final String DATA_NIC_NAME = "eth0";
	public static final String USERNAME = "ubuntu";
	
	// maintains the path to the script directory
	private String scriptDirectory; 
	

	public final String serverName;
	public final String instanceId;
	
	// password for the user `ubuntu'
	// TODO this is stored in plaintext haha
	private String password;

	// Virtual network interfaces
	private VirtualNIC dataNIC;
	private VirtualNIC controlNIC;		//need IP address???
	
	// Ports that are connected to this Virtual Server
	// maps from port number to Port instance
	private HashMap<Integer, Port> portMap;
	
	// maintains the port number for ssh connection to this server
	private Port sshPort;
	
	// Server config
	private int diskSize;
	private int numCpu;
	private int memSize;
	private String networkCfg;
	
	
	
	//TODO I have no idea about ports!!!!
	
	public VirtualServer(Controller controller, Subnet parentSubnet, long serverID, String serverName, String password, String networkCfg) {
		this(controller, parentSubnet, serverID, serverName, password, networkCfg, DEFAULT_DISKSIZE, DEFAULT_NUMCPU, DEFAULT_MEM);
	}
	
	public VirtualServer(Controller controller, Subnet parentSubnet, long serverID, 
			String serverName, String password, String networkCfg, int diskSize, int numCpu, int memSize) {

		this.state = ServerState.NON_EXISTENT;
		
		this.controller = controller;
		this.parentSubnet = parentSubnet;
		this.serverID = serverID;
		this.serverName = serverName;
		this.password = password;
		this.instanceId = "iid-" + serverName + Long.toString(serverID);
		
		this.networkCfg = networkCfg;
		this.diskSize = diskSize;
		this.numCpu = numCpu;
		this.memSize = memSize;
		
		//TODO configuration of virtual NIC
		this.dataNIC = new VirtualNIC(parentSubnet, DATA_NIC_NAME);
		this.controlNIC = new VirtualNIC();
		
		this.portMap = new HashMap<>();
		
		this.scriptDirectory = controller.configMap.get("LocScript");
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
	 *	create_server.sh basically does following:
	 *	 ** POOL is located in /home/vm
	 *	 ** Image format is qcow2
	 *
	 *	 + download Ubuntu 14.04 cloud image 
	 *	 + create ISO image from userdata + metadata files and copy it to the pool
	 *   + create a virtual machine using virt-install
	 *   + cleanup temporary files
	 *
	 *	@exception when error creating a virtual machine
	 */
	public void createVirtualMachine() throws Exception {
		if (state != ServerState.NON_EXISTENT) {
			throw new Exception("Server already exists");
		}
		if (dataNIC.isIPAddrAssigned() == false) {
			throw new Exception("IP address is not assigned to Data NIC yet");
		}

		//TODO error checking on arguments for script
		String locCreateScript = scriptDirectory + "/create_server.sh";
		
		String locMetadata = "/tmp/metadata-" + serverName.trim() + Long.toString(serverID);
		String locUserdata = "/tmp/userdata-" + serverName.trim() + Long.toString(serverID);
		
		// create metadata and userdata
		if (createMetadata(locMetadata) != 1 || createUserdata(locUserdata) != 1) {
			//error!!!!!!
			throw new Exception("Error when creating metadata/userdata");
		}
				
		// need to change to string before passing them as arguments
		ProcessBuilder pb = new ProcessBuilder(locCreateScript, instanceId, parentSubnet.domainName,
												Integer.toString(diskSize), Integer.toString(numCpu),
												Integer.toString(memSize), networkCfg,
												locMetadata, locUserdata);
		
		Process p = pb.start();
		//TODO NEED TO BE ASYNCHRONOUSSSSSSS
		int exitStatus = p.waitFor();
		if (exitStatus != 0) {
			// error executing this script
			Debug.redDebug("Error with createVirtualMachine, with exit status: " + exitStatus);
			throw new Exception("Error running create_server.sh with exit code: " + exitStatus);
		} else {
			state = ServerState.RUNNING;
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
	 *	  hwaddress ether ${MAC ADDRESS}
	 *	hostname: ${SERVERNAME}
	 *	local-hostname: ${SERVERNAME}
	 * 
	 * 
	 * @return 1 on success, 0 on error
	 */
	private int createMetadata(String location) {
		String strIpAddr = dataNIC.ipAddr.getHostAddress();
		String strNtwkAddr = dataNIC.subnetAddr.getNetworkAddress().getHostAddress();
		String strNetmask = dataNIC.subnetAddr.getNetMask().getHostAddress();
		String strBroadcast = dataNIC.subnetAddr.getBroadcastAddress().getHostAddress();
		String strGatewayAddr = dataNIC.subnetAddr.getGatewayAddress().getHostAddress();
		String strMacAddr = dataNIC.macAddress.toString();

		
		StringBuilder metadataBuilder = new StringBuilder();
		metadataBuilder.append("instance-id: " + instanceId + ";\n")
					   .append("network-interfaces: |\n")
					   .append("  auto " + dataNIC.interfaceName + "\n")
					   .append("  iface " + dataNIC.interfaceName + " inet static\n")
					   .append("  address " + strIpAddr + "\n")
					   .append("  network " + strNtwkAddr + "\n")
					   .append("  netmask " + strNetmask + "\n")
					   .append("  broadcast " + strBroadcast + "\n")
					   .append("  gateway " + strGatewayAddr + "\n")
					   .append("  dns-search " + parentSubnet.domainName + "\n")
					   .append("  dns-nameservers " + strGatewayAddr + "\n")
					   .append("  hwaddress ether " + strMacAddr + "\n")
					   .append("hostname: " + serverName + "\n")
					   .append("local-hostname: " + serverName + "\n");

		String strMetadata = metadataBuilder.toString();
		
		try {
			// create metadata file at location
			File file = new File(location);
			
			if (!file.exists()) {
				file.createNewFile();
			}
			
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(strMetadata);
			bw.close();

			return 1;
		} catch (Exception e) {
			if (Debug.IS_DEBUG) {
				e.printStackTrace();
			}
			return 0;
		}
	}
	
	/**
	 * Creates a userdata file for Cloud Init
	 * User data is a text configuration file like following
	 *    #cloud-config
	 *    password: password
	 *    chpasswd: { expire: False }
	 *    ssh_pwauth: True
	 *    
	 *    Other configuration such as writing files follows.
	 * @return 1 on success, 0 on error
	 */
	private int createUserdata(String location) {
		StringBuilder userdataBuilder = new StringBuilder();
		userdataBuilder.append("#cloud-config\n")
					   .append("password: " + password + "\n")
					   .append("chpasswd: { expire: False }\n")
					   .append("ssh_pwauth: True\n");
		
		try {
			//write-files option -- sshdnull_config related
			String sshd_cfg = getSSHConfig();
			userdataBuilder.append("write_files:\n")
						   .append("-   path: /etc/ssh/sshd_config")
						   .append("content: |\n")
						   .append(sshd_cfg);

			// create metadata file at location
			File file = new File(location);
			
			if (!file.exists()) {
				file.createNewFile();
			}
			
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(userdataBuilder.toString());
			bw.close();

			return 1;
		} catch (Exception e) {
			Debug.debug(e.getMessage());
			if (Debug.IS_DEBUG) {
				e.printStackTrace();
			}
			return 0;
		}

	}

	/**
	 * Returns sshd_config string for userdata file, based on sshd_cfg_template
	 * This assigns an available port as SSH port
	 * The returned string NEEDS TO BE INTENDED BY 8 SPACES
	 */
	private String getSSHConfig() throws Exception{
		StringBuilder sshConfigBuilder = new StringBuilder();

		// assigns and registers a new port for SSH connection
		sshPort = controller.getAvailablePort();
		
		if (sshPort == null || portMap.containsKey(sshPort.num)) {
			//TODO this port is for some reason registered already!!! weird!!!!!!!
			throw new Exception("SSH port is weirdly already taken or null");
		}
		
		//register that internally
		portMap.put(sshPort.num, sshPort);
		
		//create sshd_config from sshd_config template
		sshConfigBuilder.append("        " + "Port " + sshPort.num + "\n");

		String locCfgTemplate = scriptDirectory + "/sshd_cfg_template";
		try (BufferedReader br = new BufferedReader(new FileReader(new File(locCfgTemplate)))){
			String line;
			while ((line = br.readLine()) != null) {
				sshConfigBuilder.append("        " + line + "\n");
			}
		} catch (Exception e) {
			throw new Exception("Error reading sshd_cfg_template");
		}

		return sshConfigBuilder.toString();
	}
	
	/**
	 * Destroys a virtual machine using destroy_server.sh script
	 * @throws Exception
	 */
	public void destroyVirtualMachine() throws Exception {
		if (state == ServerState.NON_EXISTENT) {
			throw new Exception("Server doesn't exist");
		}
		
		String locDestroyScript = scriptDirectory + "/destroy_server.sh";
		
		ProcessBuilder pb = new ProcessBuilder(locDestroyScript, instanceId);
		Process p = pb.start();
		//TODO NEED TO BE ASYNCHRONOUSSSSSSSS
		int exitStatus = p.waitFor();
		if (exitStatus != 0) {
			// error executing this script
			state = ServerState.ERROR;
			Debug.redDebug("Error with createVirtualMachine, with exit status: " + exitStatus);
			throw new Exception("Error running create_server.sh with exit code: " + exitStatus);
		} else {
			state = ServerState.NON_EXISTENT;
		}
		
		
		
		
	}

}
