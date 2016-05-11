[//]: # (TODO: techniques you used, input and output, design goals, achievements and impacts, future improvements, problems encountered.)
##Final Project Report

#####Interesting Code/Techniques:

#####Design Goals: [Design document](https://github.com/taishi8117/CPSC433-OpenStack-Final-Project/blob/master/Documentation/design.md)

#####Achievement and Impact:

#####Future Improvements:
Initially we had planned to create a separate storage component on the host machine accessible for each virtual server. This would allow a more efficient use of space and resources than the current design, allocating a certain amount per virtual machine, even if it did not fully consume all of that space.

Additionally, we could scale to more than one host, combining the hardware capabilities of different machines to pool resources to create a greater number of services able to be provided. 

#####Problems Encountered:
We faced an issue when creating the Virtual Machines that made them unable to be accessed via SSH or any other console due to an error on bootup. Although we suspected it was errors in the intial creation script, the actual bugs were hard to track down due to the nature of our project. 
