package object;

public class Port {


    public enum Type {
        CONTROL,
        SSH,
        USER
    }

    private Controller controller;
    // Network this belongs to
    public Network network;
    public int num;
    public Type type;

    public Port(Controller controller, Type type, int num, Network network) {
        this.controller = controller;
        this.num = num;
        this.network = network;
        this.type = type;
    }

    public void update(){

        return;
    }

    @Override
    public String toString(){
        return type + ":" + num;
    }

}
