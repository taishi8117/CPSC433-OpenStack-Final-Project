#!/bin/bash
# create_server.sh : script to create a virtual server using Ubuntu 14.04 cloud image on kvm
# Needs to be run as root
#
# Usage:
#	# ./create_server.sh 
# List of Environment Variables									-- In VirtualServer.java
# * VSHOST: the instanceID of the server (needs to be unique)	-- instanceId
# * VSDOMAIN: the domain name in which the server is created		-- parentSubnet.domainName
# * VROOTDISKSIZE: the disk size for the server in GB			-- diskSize + "G" (e.g. "10G")
# * VCPUS: the number of CPUs allocated for the server			-- numCpu
# * VMEM: the amount of memory allocated for the server in MB	-- memSize
# * VSNETWORK: the network configuration (VNIC) with no space	-- networkCfg
# * METADATA_FILE: the location of meta-data file				-- locMetadata
# * USERDATA_FILE: the location of user-data file				-- locUserdata
#
# Note that this script DOES NOT check if all environment variables are set properly
#
# Author : Taishi Nojima
# Referenced from https://github.com/clauded/virt-tools/blob/master/virt-install-cloud.sh
#
# Requires `kvm_install.sh' to be run in advance

set -x

# assumes that ubuntu 14.04 (trusty) on amd64
IMG="trusty"
ARCH="amd64"

# image format: qcow2
FORMAT=qcow2
# kvm pool
POOL=vm
POOL_PATH=/home/vm

# check if metadata file and userdata file exist
if [[ -r ${METADATA_FILE}  && -r ${USERDATA_FILE} ]]; 
then
	echo "Found Metadata_file and Userdata_file"
else
	echo "Metadata_file or userdata_file not found"
	exit 1
fi

# check if the script is run as root user
if [[ $USER != "root" ]]; then
  echo "This script must be run as root!" && exit 1
fi


# download cloud image if not already downloaded
#  ref. https://openstack.redhat.com/Image_resources
case $IMG in
  trusty)   IMG_USER="ubuntu"
            IMG_URL="http://cloud-images.ubuntu.com/server/releases/14.04/release"
            IMG_NAME="ubuntu-14.04-server-cloudimg-${ARCH}-disk1.img"
            ;;
  *)        echo "Cloud image not available!"
            exit 1
            ;;
esac
if [[ ! -f ${IMG_NAME} ]]; then
  echo "Downloading image ${IMG_NAME}..."
  wget ${IMG_URL}/${IMG_NAME} -O ${IMG_NAME}
fi

# check if pool exists, otherwise create it
if [[ "$(virsh pool-list|grep ${POOL} -c)" -ne "1" ]]; then
  virsh pool-define-as --name ${POOL} --type dir --target ${POOL_PATH}
  virsh pool-autostart ${POOL}
  virsh pool-build ${POOL}
  virsh pool-start ${POOL}
fi

# write the two cloud-init files into an ISO
genisoimage -output configuration.iso -volid cidata -joliet -rock ${USERDATA_FILE} ${METADATA_FILE}
# keep a backup of the files for future reference
cp ${USERDATA_FILE} ${POOL_PATH}/user-data.${VSHOST}
cp ${METADATA_FILE} ${POOL_PATH}/meta-data.${VSHOST}
# copy ISO into libvirt's directory
cp configuration.iso ${POOL_PATH}/${VSHOST}.configuration.iso
virsh pool-refresh ${POOL}

# copy image to libvirt's pool
if [[ ! -f ${POOL_PATH}/${IMG_NAME} ]]; then
  cp ${IMG_NAME} ${POOL_PATH}
  virsh pool-refresh ${POOL}
fi

# clone cloud image
virsh vol-clone --pool ${POOL} ${IMG_NAME} ${VSHOST}.root.img
virsh vol-resize --pool ${POOL} ${VSHOST}.root.img ${VROOTDISKSIZE}


# POSSIBLY IMPORTANT
echo "Converting image to format ${FORMAT}..."
qemu-img convert -O ${FORMAT} ${POOL_PATH}/${VSHOST}.root.img ${POOL_PATH}/${VSHOST}.root.img.${FORMAT}
rm ${POOL_PATH}/${VSHOST}.root.img
mv ${POOL_PATH}/${VSHOST}.root.img.${FORMAT} ${POOL_PATH}/${VSHOST}.root.img



echo "Creating host ${VSHOST}..."
virt-install \
  --name ${VSHOST} \
  --ram ${VMEM} \
  --vcpus=${VCPUS} \
  --autostart \
  --memballoon virtio \
  --network ${VSNETWORK} \
  --boot hd \
  --disk vol=${POOL}/${VSHOST}.root.img,device=disk,format=${FORMAT},bus=virtio \
  --disk vol=${POOL}/${VSHOST}.configuration.iso,device=disk,format=${FORMAT},bus=virtio \
  --noautoconsole


# cleanup
rm configuration.iso ${METADATA_FILE} ${USERDATA_FILE}

exit 0
