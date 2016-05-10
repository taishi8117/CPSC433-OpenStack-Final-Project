package object;

import java.net.Inet4Address;

import project.Controller;

public class Port {

    public enum Status {
        CLOSED,
        UNLINKED,
        LINKED
    }

    private Controller controller;
    public int num; // port number
    public Status status; // status of this port

    public Port(int num, Controller controller) {
        this.controller = controller;
        this.num = num;
        this.status = Status.UNLINKED;
    }

    /* linkPort - associate the port with a VM's port
    * @params - address/port of associated VM, and the VNIC associated with it
    *
    *
    *
    */
	public void linkPort(Inet4Address downstreamAddress, int downstreamPort, String vnicName){
        controller.establishRule(controller.hostIP, this.num, downstreamAddress,downstreamPort, vnicName);
        this.status = Status.LINKED;
    }

    public void unlinkPort(Inet4Address downstreamAddress, int downstreamPort, String vnicName){
        controller.destroyRule(controller.hostIP, this.num, downstreamAddress,downstreamPort, vnicName);
        this.status = Status.UNLINKED;
    }

    @Override
    public String toString(){
        return status + ":" + num;
    }

}
