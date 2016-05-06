#!/bin/bash
# destroy_server.sh : script to destroy a virtual server
# Needs to be run as root
#
# Usage:
#   # ./destroy_server.sh instanceID
#
# Author: Taishi Nojima

if [ "$#" -ne 8 ]; then
	echo "Usage: # ./destroy_server.sh instanceID"
	exit 1
fi

# check if the script is run as root user
if [[ $USER != "root" ]]; then
  echo "This script must be run as root!" && exit 1
fi

HOST=$1

set -x

virsh destroy ${HOST}
#virsh list --all

virsh undefine ${HOST}
#virsh list --all

#virsh vol-list vm
virsh vol-delete --pool vm ${HOST}.configuration.iso
virsh vol-delete --pool vm ${HOST}.root.img

#virsh list --all
echo "[+] Complete"
