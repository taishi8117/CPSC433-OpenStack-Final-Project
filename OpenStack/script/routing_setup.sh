# host_interface=eth0 # name of host machine interface connected to the Internet
# vnet_interface=vboxnet0 # name of host machine's local network interface
# host_ip[0]=... # array contains external IP addresses for every guest
# guest_ip[0]=... # array contains internal IP addresses for every guest
# guest_ports[0]=21,22,80,443 # array of port lists for every guest

iptables -P INPUT ACCEPT
iptables -F INPUT
iptables -P OUTPUT ACCEPT
iptables -F OUTPUT
iptables -P FORWARD ACCEPT
iptables -F FORWARD
iptables -t nat -F

echo "1" > /proc/sys/net/ipv4/ip_forward

# forward all packets from already established connections
iptables -A FORWARD -m state --state ESTABLISHED,RELATED -j ACCEPT