#!/bin/bash
NAME=$1
virsh destroy ${NAME}
virsh list --all

virsh undefine ${NAME}
virsh list --all

virsh vol-list vm
virsh vol-delete --pool vm ${NAME}.configuration.iso
virsh vol-delete --pool vm ${NAME}.root.img

virsh list --all


mv user-data.${NAME} user-data
mv meta-data.${NAME} meta-data
echo "[+] Complete"
