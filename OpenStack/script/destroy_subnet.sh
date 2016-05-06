#!/bin/bash
# destroy_subnet.sh : destroy a subnet
# Needs to be run as root
#
# Usage:
#   # ./destroy_subnet.sh networkCfgName
#
# Author: Taishi Nojima

if [ "$#" -ne 1 ]; then
	echo "Usage: # ./destroy_subnet.sh networkCfgName"
	exit 1
fi

# check if the script is run as root user
if [[ $USER != "root" ]]; then
  echo "This script must be run as root!" && exit 1
fi

NWNAME=$1

set -x


virsh net-destroy $NWNAME
virsh net-undefine $NWNAME

