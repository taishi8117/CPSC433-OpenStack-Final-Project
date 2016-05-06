#!/bin/bash
# create_subnet.sh : creates a new subnet --- create a new bridge based on a given network config (xml) file
# Preferbly, network cfg file should have the name of subnetID
# Needs to be run as root
#
# Usage:
#   # ./create_subnet.sh networkCfg
#  * networkCfg : Bridge interface configuration for libvirt (xml)
#
# Author: Taishi Nojima

if [ "$#" -ne 1 ]; then
	echo "Usage: # ./create_subnet.sh networkCfg"
	exit 1
fi

# check if the script is run as root user
if [[ $USER != "root" ]]; then
  echo "This script must be run as root!" && exit 1
fi

CFGFILE=$1
#get the name of the network
NWNAME="${CFGFILE%.*}"

set -x

virsh net-define --file $CFGFILE
virsh net-start network2

virsh net-list

