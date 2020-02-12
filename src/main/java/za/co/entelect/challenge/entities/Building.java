package za.co.entelect.challenge.entities;

import za.co.entelect.challenge.enums.BuildingType;
import za.co.entelect.challenge.enums.PlayerType;

public class Building extends Cell {

    public int health;
    public int constructionTimeLeft;
    public int price;
    public int weaponDamage;
    public int weaponSpeed;
    public int weaponCooldownTimeLeft;
    public int weaponCooldownPeriod;
    public int destroyScore;
    public int energyGeneratedPerTurn;
    public BuildingType buildingType;

    public boolean isEqual(int y, PlayerType pType){
        return (this.y==y && playerType==pType);
    }

    public boolean isEqual(int y, BuildingType bType, PlayerType pType){
        return (isEqual(y, pType) && buildingType==bType);
    }

    public boolean isEqual(BuildingType bType, PlayerType pType){
        return (buildingType==bType && playerType==pType);
    }
}
