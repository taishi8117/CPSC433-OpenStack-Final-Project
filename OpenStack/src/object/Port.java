package object;


public class Port {

    // Network this belongs to
    public Network network;
    public int num;

    public Port(int num, Network network) {
        this.num = num;
        this.network = network;
    }


}
