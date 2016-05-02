#!/bin/bash
#
# virt-install-cloud.sh : script to start an OpenStack cloud image on kvm
# version : 1.0
#
# Author : Claude Durocher
# License : GPLv3
#
# ref. http://mojodna.net/2014/05/14/kvm-libvirt-and-ubuntu-14-04.html
#
# requires the following packages on Ubuntu host:
#  apt-get install qemu-kvm libvirt-bin virtinst bridge-utils genisoimage
# requires the following packages on CentOS host:
#  yum install qemu-kvm libvirt virt-install bridge-utils genisoimage
#
set -x
# image selection : trusty, precise, centos7, fedora20, ...
IMG="trusty"
# architecture : amd64 or i386
ARCH="amd64"
# kvm defaults pool paths
DEF_POOL=default
DEF_POOL_PATH=/var/lib/libvirt/images
# vm prefs : specify vm preferences for your guest
GUEST=py
DOMAIN=intranet.local
VROOTDISKSIZE=10G
VCPUS=2
VMEM=2048
NETWORK="bridge=virbr0,model=virtio"
# guest image format: qcow2 or raw
FORMAT=qcow2
# convert image format : yes or no
CONVERT=no
# kvm pool
POOL=vm
POOL_PATH=/home/vm
#
# cloud-init config files : specify cloud-init data for your guest
cat <<EOF > meta-data
instance-id: iid-${GUEST};
network-interfaces: |
  auto eth0
  iface eth0 inet static
  address 192.168.122.10
  network 192.168.122.0
  netmask 255.255.255.0
  broadcast 192.168.122.255
  gateway 192.168.122.1
  dns-search ${DOMAIN}
  dns-nameservers 192.168.122.1
hostname: ${GUEST}
local-hostname: ${GUEST}
EOF
#
cat <<EOF > user-data
#cloud-config
password: password
chpasswd: { expire: False }
ssh_pwauth: True
# upgrade packages on startup
package_upgrade: false
#run 'apt-get upgrade' or yum equivalent on first boot
apt_upgrade: false
#manage_etc_hosts: localhost
manage_etc_hosts: true
fqdn: ${GUEST}.${DOMAIN}
# install additional packages
packages:
  - mc
  - htop
#  - language-pack-fr
# run commands
runcmd:
# install htop on centos/fedora
#  - [ sh, -c, "curl http://dl.fedoraproject.org/pub/epel/7/x86_64/e/epel-release-7-2.noarch.rpm -o /tmp/epel-release.rpm" ]
#  - [ sh, -c, "yum install -y /tmp/epel-release.rpm" ]
#  - [ sh, -c, "yum install -y htop" ]
#ssh_authorized_keys:
#  - ssh-rsa AAAAB3NzaC1yc2QwAAADAQABAAa3BAQC0g+ZTxC7weoIJLUafOgrm+h...
EOF

# don't edit below unless you know wat you're doing!

# check if the script is run as root user
if [[ $USER != "root" ]]; then
  echo "This script must be run as root!" && exit 1
fi

# download cloud image if not already downloaded
#  ref. https://openstack.redhat.com/Image_resources
case $IMG in
  precise)  IMG_USER="ubuntu"
            IMG_URL="http://cloud-images.ubuntu.com/server/releases/12.04/release"
            IMG_NAME="ubuntu-12.04-server-cloudimg-${ARCH}-disk1.img"
            ;;
  trusty)   IMG_USER="ubuntu"
            IMG_URL="http://cloud-images.ubuntu.com/server/releases/14.04/release"
            IMG_NAME="ubuntu-14.04-server-cloudimg-${ARCH}-disk1.img"
            ;;
  centos7)  IMG_USER="centos"
            IMG_URL="http://cloud.centos.org/centos/7/devel"
            if [[ $ARCH = "amd64" ]]; then
              IMG_NAME="CentOS-7-x86_64-GenericCloud.qcow2"
            else
              echo "Cloud image not available!"
              exit 1
            fi
            ;;
  fedora20) IMG_USER="fedora"
            if [[ $ARCH = "amd64" ]]; then
              IMG_URL="http://download.fedoraproject.org/pub/fedora/linux/updates/20/Images/x86_64"
              IMG_NAME="Fedora-x86_64-20-20140407-sda.qcow2"
            else
              IMG_URL="http://download.fedoraproject.org/pub/fedora/linux/updates/20/Images/i386"
              IMG_NAME="Fedora-i386-20-20140407-sda.qcow2"
            fi
            ;;
  wheezy)   IMG_USER="debian"
            if [[ $ARCH = "amd64" ]]; then
              IMG_URL="Coud image not available. Use build-openstack-debian-image to build one!"
              IMG_NAME="debian-wheezy-7.0.0-3-amd64.qcow2"
            else
              IMG_NAME="Cloud image not available."
              exit 1
            fi
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
genisoimage -output configuration.iso -volid cidata -joliet -rock user-data meta-data
# keep a backup of the files for future reference
cp user-data user-data.${GUEST}
cp meta-data meta-data.${GUEST}
# copy ISO into libvirt's directory
cp configuration.iso ${POOL_PATH}/${GUEST}.configuration.iso
virsh pool-refresh ${POOL}

# copy image to libvirt's pool
if [[ ! -f ${POOL_PATH}/${IMG_NAME} ]]; then
  cp ${IMG_NAME} ${POOL_PATH}
  virsh pool-refresh ${POOL}
fi

# clone cloud image
virsh vol-clone --pool ${POOL} ${IMG_NAME} ${GUEST}.root.img
virsh vol-resize --pool ${POOL} ${GUEST}.root.img ${VROOTDISKSIZE}

# convert image format
if [[ "${CONVERT}" == "yes" ]]; then
  echo "Converting image to format ${FORMAT}..."
  qemu-img convert -O ${FORMAT} ${POOL_PATH}/${GUEST}.root.img ${POOL_PATH}/${GUEST}.root.img.${FORMAT}
  rm ${POOL_PATH}/${GUEST}.root.img
  mv ${POOL_PATH}/${GUEST}.root.img.${FORMAT} ${POOL_PATH}/${GUEST}.root.img
fi

echo "Creating guest ${GUEST}..."
virt-install \
  --name ${GUEST} \
  --ram ${VMEM} \
  --vcpus=${VCPUS} \
  --autostart \
  --memballoon virtio \
  --network ${NETWORK} \
  --boot hd \
  --disk vol=${POOL}/${GUEST}.root.img,format=${FORMAT},bus=virtio \
  --disk vol=${POOL}/${GUEST}.configuration.iso,bus=virtio \
  --noautoconsole

# display result
echo
echo "List of running VMs :"
echo
virsh list

# cleanup
rm configuration.iso meta-data user-data

# stuff to remember
echo
echo "************************"
echo "Useful stuff to remember"
echo "************************"
echo
echo "To login to vm guest:"
echo " sudo virsh console ${GUEST}"
echo "Default user for cloud image is :"
echo " ${IMG_USER}"
echo
echo "To edit guest vm config:"
echo " sudo virsh edit ${GUEST}"
echo
echo "To create a volume:"
echo " virsh vol-create-as ${POOL} ${GUEST}.vol1.img 20G --format ${FORMAT}"
echo "To attach a volume to an existing guest:"
echo " virsh attach-disk ${GUEST} --source ${POOL_PATH}/${GUEST}.vol1.img --target vdc --driver qemu --subdriver ${FORMAT} --persistent"
echo "To prepare the newly attached volume on guest:"
echo " sgdisk -n 1 -g /dev/vdc && && mkfs -t ext4 /dev/vdc1 && sgdisk -c 1:'vol1' -g /dev/vdc && sgdisk -p /dev/vdc"
echo " mkdir /mnt/vol1"
echo " echo '/dev/vdc1 /mnt/vol1 ext4 defaults,relatime 0 0' >> /etc/fstab"
echo
echo "To shutdown a guest vm:"
echo "  sudo virsh shutdown ${GUEST}"
echo
