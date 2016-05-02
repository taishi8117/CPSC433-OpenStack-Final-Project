package object;

import java.util.HashMap;

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

    /**
     * !!
     * @param
     */


    public Rule(Chain chain, String networkName, long tenantID, long networkID) {
`
    }

    @Override
    public boolean equals(Rule other){
//        TODO: COMPARE OTHER STUFF
        if (this == other) {
            return true;
        }
        else{
            return false;
        }
    }


    //Use this to get the proper string to match to
    @Override
    public String toString(){
        return "null";
    }

}
