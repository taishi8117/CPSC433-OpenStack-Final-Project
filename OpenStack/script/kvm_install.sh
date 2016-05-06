#!/bin/bash

# Install KVM related applications

# check if the script is run as root user
if [[ $USER != "root" ]]; then
  echo "This script must be run as root!" && exit 1
fi

echo "[+ Installing KVM related applications]"
apt-get install qemu-kvm libvirt-bin virtinst bridge-utils genisoimage

#disable default network
virsh net-autostart default --disable
virsh net-destroy default

#create openstack network
virsh net-define --file ./openstack.xml
virsh net-autostart openstack

virsh net-list --all

/etc/init.d/libvirt-bin restart

echo "Done"


