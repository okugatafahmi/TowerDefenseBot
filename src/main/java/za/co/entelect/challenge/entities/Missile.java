package za.co.entelect.challenge.entities;

public class Missile extends Cell {
    private int damage;
    private int speed;

    public int getMissilePosWhen(int t){
        return x+speed*t;
    }
}
