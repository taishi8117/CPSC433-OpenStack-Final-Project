#!/bin/bash
# destroy_server.sh : script to destroy a virtual server
# Needs to be run as root
#
# Usage:
#   # ./destroy_server.sh 
# List of Environment Variables
# * VSHOST: the instance ID of the server
#
# Author: Taishi Nojima

# check if the script is run as root user
if [[ $USER != "root" ]]; then
  echo "This script must be run as root!" && exit 1
fi

set -x

virsh destroy ${VSHOST}
#virsh list --all

virsh undefine ${VSHOST}
#virsh list --all

#virsh vol-list vm
virsh vol-delete --pool vm ${VSHOST}.configuration.iso
virsh vol-delete --pool vm ${VSHOST}.root.img

#virsh list --all
echo "[+] Complete"
