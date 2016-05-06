package object;

import java.util.HashMap;
import java.net.Inet4Address;
import lib.SubnetAddress;
import project.Controller;

public class Rule {
    // Type of Rule. -
    // INPUT (Typically ACCEPTs)
    // FORWARD
    // Firewall? (DROP), INPUT ACCEPT (when port created), PREROUTING(change IP on match packets),
    //                   FORWARD appropriate packet (send packet to the appropriate destination VM)
    enum Chain {
        INPUT, // DESTIP is this host
        FORWARD, // just transfering packet
        OUTPUT, // SRCIP is this host
        POSTROUTING,
        PREROUTING
    }

    // FORWARD - [CREATED] VM    with port was initiated
    //           [REMOVED] port disassociated with VM (might have been removed/deleted)

    // params: from port, dest port. from ip, To ip, Chain Enum
    // need to make a list of these rules somewhere...
    // when you add rule, add to the iptable of host
    //   - add to prerouting, postrouting
    // when you remove a rule, remove from iptable

    private Controller controller;
    private Chain type;
    private Inet4Address upstreamAddress;
    private Inet4Address downstreamAddress;
    private int upstreamPort;
    private int downstreamPort;

    /**
     * !!
     * @param
     */


    public Rule(Controller controller,/*Chain chain,*/ Inet4Address upstreamAddress, int upstreamPort,
                Inet4Address downstreamAddress, int downstreamPort) {
        this.type = Chain.PREROUTING;//chain; // temp commented out
        this.upstreamAddress=upstreamAddress;
        this.downstreamAddress=downstreamAddress;
        this.downstreamPort=downstreamPort;
        this.upstreamPort=upstreamPort;
        this.controller=controller;
        this.establishRule();
    }

    // Establishes the rules in iptable on NAT for both prerouting from and to the VM
    public void establishRule(){
        String toVM = "iptables -t nat -A PREROUTING -p tcp -d "+ this.upstreamAddress+" --dport "+
                this.upstreamPort +" -j DNAT --to "+ this.downstreamAddress +":"+this.downstreamPort;
        String fromVM = "iptables -t nat -A PREROUTING -p tcp -d "+this.downstreamAddress+" --dport "+
                this.downstreamPort +" -j DNAT --to "+ this.upstreamAddress +":"+this.upstreamPort;

        // todo: run these commands as sudo

        // iptables -t nat -A PREROUTING -p tcp -d 192.168.102.37 --dport 422 -j DNAT --to 192.168.102.37:22

    }

    // TODO: Run as sudo
    // Removes this rule from the IP tables
    public void destroyRule(){
        String removetoVM = "iptables -t nat -D PREROUTING -p tcp -d "+ this.upstreamAddress+" --dport "+
                this.upstreamPort +" -j DNAT --to "+ this.downstreamAddress +":"+this.downstreamPort;
        String removefromVM = "iptables -t nat -D PREROUTING -p tcp -d "+this.downstreamAddress+" --dport "+
                this.downstreamPort +" -j DNAT --to "+ this.upstreamAddress +":"+this.upstreamPort;
        // TODO: Figure out how to run these scripts
    }

    // Removes this rule from the IP tables (specifying args)
    public static void destroyRule(Inet4Address upstreamAddress, int upstreamPort,Inet4Address downstreamAddress,
                                   int downstreamPort){
        String removetoVM = "iptables -t nat -D PREROUTING -p tcp -d "+ upstreamAddress+" --dport "+
                upstreamPort +" -j DNAT --to "+ downstreamAddress +":"+downstreamPort;
        String removefromVM = "iptables -t nat -D PREROUTING -p tcp -d "+ downstreamAddress+" --dport "+
                downstreamPort +" -j DNAT --to "+ upstreamAddress +":"+ upstreamPort;
    }


    // When to create a rule:
    //  - when you're creating a port and assigning it with the VM
    //
    // When to remove a rule:
    //  - when you're done with a VM or deassociate a port with VM

//    @Override
    public boolean equals(Rule other){
//      TODO: COMPARE OTHER STUFF
        if (this == other) {
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public String toString(){
        return "null";
    }

}
