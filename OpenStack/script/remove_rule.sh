#!/bin/bash
# remove_rule.sh : deletes routing rule -- restores original routing
# Needs to be run as root
#
# Usage:
#   # ./remove_rule.sh
# List of Environment Variables
# * UPSTREAMADDR : address of host upstream from VM client
# * DOWNSTREAMADDR : address of VM client downstream from host
# * UPSTREAMPORT : port of host upstream from VM client
# * DOWNSTREAMPORT : port of VM client downstream from host
#
# Author: Alan Liu

# check if the script is run as root user
if [[ $USER != "root" ]]; then
  echo "This script must be run as root!" && exit 1
fi

set -x

# remove routing from upstream to downstream
iptables -t nat -D PREROUTING -p tcp -d $UPSTREAMADDR --dport $UPSTREAMPORT -j DNAT --to $DOWNSTREAMADDR:$DOWNSTREAMPORT

# remove routing from downstream to upstream
iptables -t nat -D PREROUTING -p tcp -d $DOWNSTREAMADDR --dport $DOWNSTREAMPORT -j DNAT --to $UPSTREAMADDR:$UPSTREAMPORT

# remove forwarding new connections from host to guest
iptables -D FORWARD -i $host_interface -o $vnet_interface -d $DOWNSTREAMADDR -m state --state NEW -j ACCEPT

# remove forwarding new connections from guest to host
iptables -D FORWARD -i $vnet_interface -o $host_interface -s $DOWNSTREAMADDR -m state --state NEW -j ACCEPT

iptables -D nat -A POSTROUTING -o $host_interface -s $DOWNSTREAMADDR -j SNAT --to-source $UPSTREAMADDR

echo "Successfully removed rules"