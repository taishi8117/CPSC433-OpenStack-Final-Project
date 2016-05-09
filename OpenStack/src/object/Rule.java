// package object;

// import java.util.HashMap;
// import java.net.Inet4Address;
// import lib.SubnetAddress;
// import lib.Debug;
// import project.Controller;

// public class Rule {
//     // Type of Rule. -
//     // INPUT (Typically ACCEPTs)
//     // FORWARD
//     // Firewall? (DROP), INPUT ACCEPT (when port created), PREROUTING(change IP on match packets),
//     //                   FORWARD appropriate packet (send packet to the appropriate destination VM)
//     enum Chain {
//         INPUT, // DESTIP is this host
//         FORWARD, // just transfering packet
//         OUTPUT, // SRCIP is this host
//         POSTROUTING,
//         PREROUTING
//     }

//     // FORWARD - [CREATED] VM    with port was initiated
//     //           [REMOVED] port disassociated with VM (might have been removed/deleted)

//     // params: from port, dest port. from ip, To ip, Chain Enum
//     // need to make a list of these rules somewhere...
//     // when you add rule, add to the iptable of host
//     //   - add to prerouting, postrouting
//     // when you remove a rule, remove from iptable

//     private Controller controller;
//     private Chain type;
//     private Inet4Address upstreamAddress;
//     private Inet4Address downstreamAddress;
//     private int upstreamPort;
//     private int downstreamPort;

//     private String scriptDirectory;


//     public Rule(Controller controller, /* Chain chain,*/ Inet4Address upstreamAddress, int upstreamPort,
//                 Inet4Address downstreamAddress, int downstreamPort) {
//         this.type = Chain.PREROUTING;//chain; // temp commented out
//         this.upstreamAddress=upstreamAddress;
//         this.downstreamAddress=downstreamAddress;
//         this.downstreamPort=downstreamPort;
//         this.upstreamPort=upstreamPort;
//         this.controller=controller;
//         this.establishRule();

//         this.scriptDirectory = controller.configMap.get("LocScript");

//     }

//     // Establishes the rules in iptable on NAT for both prerouting from and to the VM
//     public void establishRule(Inet4Address upstreamAddress, int upstreamPort,Inet4Address downstreamAddress,
//                               int downstreamPort){

//         String addRuleScript = scriptDirectory + "/add_rule.sh";

//         ProcessBuilder pb = new ProcessBuilder("/bin/bash", addRuleScript);
//         pb.environment().put("UPSTREAMADDR", upstreamAddress);
//         pb.environment().put("DOWNSTREAMADDR", downstreamAddress);
//         pb.environment().put("UPSTREAMPORT", upstreamPort);
//         pb.environment().put("DOWNSTREAMPORT", downstreamPort);

//         Process p;

//         try {
//             p = pb.start();
//         } catch (Exception e) {
//             Debug.redDebug("Error with executing add_rule.sh");
//         }

//     }

//     // Removes this rule from the IP tables
//     public void destroyRule(){
//         String removeRuleScript = scriptDirectory + "/remove_rule.sh";
//         ProcessBuilder pb = new ProcessBuilder("/bin/bash", removeRuleScript);

//         pb.environment().put("UPSTREAMADDR", this.upstreamAddress);
//         pb.environment().put("DOWNSTREAMADDR", this.downstreamAddress);
//         pb.environment().put("UPSTREAMPORT", this.upstreamPort);
//         pb.environment().put("DOWNSTREAMPORT", this.downstreamPort);

//         Process p;

//         try {
//             p = pb.start();
//         } catch (Exception e) {
//             Debug.redDebug("Error with executing remove_rule.sh");
//         }
//     }

//     // Removes this rule from the IP tables (specifying args)
//     public static void destroyRule(Inet4Address upstreamAddress, int upstreamPort,Inet4Address downstreamAddress,
//                                    int downstreamPort){
//         String removeRuleScript = scriptDirectory + "/remove_rule.sh";
//         ProcessBuilder pb = new ProcessBuilder("/bin/bash", removeRuleScript);
//         pb.environment().put("UPSTREAMADDR", upstreamAddress);
//         pb.environment().put("DOWNSTREAMADDR", downstreamAddress);
//         pb.environment().put("UPSTREAMPORT", upstreamPort);
//         pb.environment().put("DOWNSTREAMPORT", downstreamPort);

//         Process p;

//         try {
//             p = pb.start();
//         } catch (Exception e) {
//             Debug.redDebug("Error with executing remove_rule.sh");
//         }
//     }


//     // When to create a rule:
//     //  - when you're creating a port and assigning it with the VM
//     //
//     // When to remove a rule:
//     //  - when you're done with a VM or deassociate a port with VM

// //    @Override
//     public boolean equals(Rule other){
// //      TODO: COMPARE OTHER STUFF
//         if (this == other) {
//             return true;
//         }
//         else{
//             return false;
//         }
//     }

//     @Override
//     public String toString(){
//         return "Rule: routing for VM: "+ this.downstreamAddress+":"+ this.downstreamPort+ ", routing for host: " + this.upstreamAddress + ":" + this.upstreamPort;
//     }

// }
