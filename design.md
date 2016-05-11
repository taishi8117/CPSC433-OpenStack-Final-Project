###Topics:
1. VMs and Configuration
2. Networks and Subnets
3. Routing

####VMs and Configuration



####Networks and Subnets

#####SSH

#####Ports
Each port that is opened on the subnet can be joined to a valid port on the host machine. This allows access both to and from external networks with the subnet through that particular port.

By default, one port is opened on each virtual server that is created to allow for SSH access upon creation of the VM.

####Routing

#####NAT
The host handles both DNAT and SNAT. It reads and processes all packets that are directed towards the external network or from the external network to the host. The host tracks each connection with its VMs using the ports specified on packets that are configured to be sent and packets that are being received and uses that to determine where to route each packet. These NAT rules are configured within the host's IP Tables.

#####IP Tables
Upon linking ports in the subnets to external ports on the host or upon creating of the SSH port, rules are added to the IP tables in the host which redirect the traffic to the bridges on the subnets. We chose to employ the IP Tables to make use of the routing capabilities of Linux. It simplified much of our routing code.

