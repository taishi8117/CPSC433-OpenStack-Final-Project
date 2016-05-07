#!/bin/bash
# create_subnet.sh : creates a new subnet --- create a new bridge based on a given network config (xml) file
# Preferbly, network cfg file should have the name of subnetID
# Needs to be run as root
#
# Usage:
#   # ./create_subnet.sh
# List of Environment Variables
# * CFGFILE : Bridge interface configuration for libvirt, should be in the form of "brcfg_" + subnetID + ".xml" 
# * SUBNWNAME : The name of the subnet created, which should be subnetID
#
# Author: Taishi Nojima

# check if the script is run as root user
if [[ $USER != "root" ]]; then
  echo "This script must be run as root!" && exit 1
fi

set -x

virsh net-define --file $CFGFILE
virsh net-start $SUBNWNAME

virsh net-list

