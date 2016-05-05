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


    // iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
    // iptables -t nat -A PREROUTING -i eth0 -p tcp --dport 80 -j DNAT --to 172.31.0.23:80
    // example of prerouting to run
    // iptables -t nat -A PREROUTING -p tcp -d 192.168.102.37 --dport 422 -j DNAT --to 192.168.102.37:22

    private Chain type;
    private Inet4Address hostAddress;
    private Inet4Address VMAddress;
    private int hostPort;
    private int VMPort;

    /**
     * !!
     * @param
     */


    public Rule(/*Chain chain,*/ Inet4Address hostAddress, int hostPort,
                Inet4Address VMAddress, int VMPort) {
        this.type = Chain.PREROUTING;//chain; // temp commented out
        this.hostAddress=hostAddress;
        this.VMAddress=VMAddress;
        this.VMPort=VMPort;
        this.hostPort=hostPort;
        this.establishRule();
    }

    // Establishes the rules in iptable on NAT for both prerouting from and to the VM
    public void establishRule(){
        String toVM = "iptables -t nat -A PREROUTING -p tcp -d "+ this.hostAddress+" --dport "+
                this.hostPort +" -j DNAT --to "+ this.VMAddress +":"+this.VMPort;
        String fromVM = "iptables -t nat -A PREROUTING -p tcp -d "+this.VMAddress+" --dport "+
                this.VMPort +" -j DNAT --to "+ this.hostAddress +":"+this.hostPort;

        // todo: run these commands as sudo

        // iptables -t nat -A PREROUTING -p tcp -d 192.168.102.37 --dport 422 -j DNAT --to 192.168.102.37:22

    }

    // TODO: Run as sudo
    // Removes this rule from the IP tables
    public void destroyRule(){
        String removetoVM = "iptables -t nat -D PREROUTING -p tcp -d "+ this.hostAddress+" --dport "+
                this.hostPort +" -j DNAT --to "+ this.VMAddress +":"+this.VMPort;
        String removefromVM = "iptables -t nat -D PREROUTING -p tcp -d "+this.VMAddress+" --dport "+
                this.VMPort +" -j DNAT --to "+ this.hostAddress +":"+this.hostPort;
        // TODO: Figure out how to run these scripts
    }

    // Removes this rule from the IP tables (specifying args)
    public static void destroyRule(Inet4Address hostAddress, int hostPort,Inet4Address VMAddress,
                                   int VMPort){
        String removetoVM = "iptables -t nat -D PREROUTING -p tcp -d "+ hostAddress+" --dport "+
                hostPort +" -j DNAT --to "+ VMAddress +":"+VMPort;
        String removefromVM = "iptables -t nat -D PREROUTING -p tcp -d "+ VMAddress+" --dport "+
                VMPort +" -j DNAT --to "+ hostAddress +":"+ hostPort;
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


    //Use this to get the proper string to match to when finding which table entries to delete
    @Override
    public String toString(){
        return "null";
    }

}
