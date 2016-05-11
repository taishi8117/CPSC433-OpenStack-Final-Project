# CPSC433-OpenStack-Final-Project

####Goal: To design an Openstack-based network of virtual machines accessible using REST API

####Instructions to Run:
1. clone the Repository
2. cd Openstack
2. make
3. sudo ./scripts/kvm_install.sh  (installs the necessary packages for supporting virtual machines)
4. make run

####APIs Supported: [API Specifications](https://github.com/taishi8117/CPSC433-OpenStack-Final-Project/blob/master/OpenStack/APIDescription.txt)

#####Create in order:
1. tenants
2. subnets
3. servers (ssh port provided)
4. additional ports (optional)

Afterwards, the user can ssh into a provided port on the servers, or open and link their own custom ports. 

####Design Choices: [Decisions](https://github.com/taishi8117/CPSC433-OpenStack-Final-Project/blob/master/design.txt)

