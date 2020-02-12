package za.co.entelect.challenge;

import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.BuildingType;
import za.co.entelect.challenge.enums.PlayerType;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Bot {
    private static final String NOTHING_COMMAND = "";
    private GameDetails gameDetails;
    private int gameWidth;
    private int gameHeight;
    private Player myself;
    private Player opponent;
    private List<Building> buildings;
    private List<Missile> missiles;

    /**
     * Constructor
     *
     * @param gameState the game state
     **/
    public Bot(GameState gameState) {
        gameDetails = gameState.getGameDetails();
        gameWidth = gameDetails.mapWidth;
        gameHeight = gameDetails.mapHeight;
        myself = gameState.getPlayers().stream().filter(p -> p.playerType == PlayerType.A).findFirst().get();
        opponent = gameState.getPlayers().stream().filter(p -> p.playerType == PlayerType.B).findFirst().get();

        buildings = gameState.getGameMap().stream()
                .flatMap(c -> c.getBuildings().stream())
                .collect(Collectors.toList());

        missiles = gameState.getGameMap().stream()
                .flatMap(c -> c.getMissiles().stream())
                .collect(Collectors.toList());
    }

    /**
     * Run
     *
     * @return the result
     **/
    public String run() {
        String command = "";

        // kalau musuh ada iron curtain, kita membuat iron curtain juga
        if (opponent.isIronCurtainActive && gameDetails.round>0){
            if (canAffordActivatingShield()){
                command = "7,7,5";
                return command;
            }
            // jika energi kurang, maka tunggu energi cukup di turn selanjutnya
            else return NOTHING_COMMAND;
        }
        else if (canAffordActivatingShield()){
            return "7,7,5";
        }

        // kalau musuh ada tesla, kita buat iron curtain
        List<Building> buildingPlayerBtesla = getPlayerThingList(buildings,b->b.isEqual(BuildingType.TESLA, PlayerType.B))
                                                .stream().filter(b->b.constructionTimeLeft<=gameDetails.ironCurtainStats.activeRounds)
                                                .collect(Collectors.toList());
        if (buildingPlayerBtesla.size()>0){
            if (canAffordActivatingShield()){
                command = "7,7,5";
                return command;
            }   
            else if (myself.energy<90) {
                return NOTHING_COMMAND;
            }
        }

        // membangun defensive wall jika kita punya tesla
        List<Building> myTesla = getPlayerThingList(buildings, b->b.isEqual(BuildingType.TESLA,PlayerType.A));
        if (myTesla.size()>0 && canAffordBuilding(BuildingType.DEFENSE)){
            for (int i=0; i<myTesla.size(); ++i){
                final int y = myTesla.get(i).getY();
                int myDefenseOnRow = getPlayerThingList(buildings, b->b.isEqual(y, BuildingType.DEFENSE, PlayerType.A)).size();

                if (myDefenseOnRow==0 && isCellEmpty(gameWidth/2-1, y)){
                    return buildFromFrontAtRow(BuildingType.DEFENSE, y);
                }
            }
        }
        
        // membangun tesla bila sudah punya banyak energy. Bila sudah bikin 1, bangun defense saja
        if (myself.energy>=gameDetails.buildingsStats.get(BuildingType.TESLA).price){
            if (myTesla.size()<2){
                int y=99,maxEnemyAttack=-1;
                for (int i = 0; i<gameHeight; ++i){
                    final int y_temp = i;
                    int enemyAttackOnRow = getPlayerThingList(buildings, b-> b.isEqual(y_temp, BuildingType.ATTACK, PlayerType.B)).size();
                    int myBuildingsOnRow = getPlayerThingList(buildings, b-> b.isEqual(y_temp, PlayerType.A)).size();
                    boolean isThereMyTesla = getPlayerThingList(buildings, b-> b.isEqual(y_temp, BuildingType.ATTACK,PlayerType.A)).size()==1;
                    if (enemyAttackOnRow>maxEnemyAttack && (myBuildingsOnRow<gameWidth/2-1) && !isThereMyTesla){
                        maxEnemyAttack = enemyAttackOnRow;
                        y = i;
                    }
                }

                for (int j = gameWidth/2-3; j>=3; --j){
                    if (isCellEmpty(j, y)){
                        return buildCommand(j, y, BuildingType.TESLA);
                    }
                }

                return "4,"+y+",3"; // deconstruct bangunan karena akan dibangun tesla
            }
            else{ // bangun defense saja, dan pasti mampu membangun karena energy ada banyak
                int maxEnemyAttack = -1, y = 99;
                for (int i=0; i<gameHeight; ++i){
                    final int y_temp = i;
                    int enemyAttackOnRow = getPlayerThingList(buildings, b->b.isEqual(y_temp, BuildingType.ATTACK, PlayerType.B)).size();
                    if (enemyAttackOnRow>maxEnemyAttack && isCellEmpty((gameWidth/2-1), i)){
                        maxEnemyAttack = enemyAttackOnRow;
                        y = i;
                    }
                }
                if (maxEnemyAttack>0){
                    return buildFromFrontAtRow(BuildingType.DEFENSE, y);
                }
            }
        }

        // langsung bangun attack di baris yg bangunan musuh paling sedikit ketika sudah punya cukup energy
        if (canAffordBuilding(BuildingType.ATTACK)){
            int minEnemyBuilding = 99, y = 99;
            for (int i=gameHeight-1; i>=0; --i){
                final int y_temp=i;
                int enemyBuilding = getPlayerThingList(buildings, b->b.isEqual(y_temp, PlayerType.B)).size();
                int myBuilding = getPlayerThingList(buildings, b->b.isEqual(y_temp, PlayerType.A)).size();
                if (enemyBuilding<minEnemyBuilding && myBuilding<gameWidth/2-1){  // tidak penuh-1
                    minEnemyBuilding = enemyBuilding;
                    y = i;
                }
            }
            if (y!=99){
                final int y_temp = y;
                int myAttack = getPlayerThingList(buildings, b->b.isEqual(y_temp, BuildingType.ATTACK,PlayerType.A)).size(),
                    enemyAttack = getPlayerThingList(buildings, b->b.isEqual(y_temp, BuildingType.ATTACK,PlayerType.B)).size();
                if ((myAttack>=enemyAttack+4) && isCellEmpty(gameWidth/2-1, y)){
                    return buildFromFrontAtRow(BuildingType.DEFENSE, y);
                }
                else {
                    return buildFromFrontAtRow(BuildingType.ATTACK, y);
                }
            }
        }

        // defense di row yang ada attack musuh nya
        if (canAffordBuilding(BuildingType.DEFENSE)){
            for (int i = 0; i<gameHeight; ++i){
                final int y = i;
                List<Building> buildingPlayerAdefense = getPlayerThingList(buildings, b->b.isEqual(y, BuildingType.DEFENSE, PlayerType.A));
                List<Building> buildingPlayerBattack = getPlayerThingList(buildings, b->b.isEqual(y, BuildingType.ATTACK, PlayerType.B));
                
                if (buildingPlayerAdefense.size()==0 && buildingPlayerBattack.size()>0){   
                    command = buildFromFrontAtRow(BuildingType.DEFENSE,i);
                    if (!command.equals("")){
                        return command;
                    }
                }
            }
        }

        // kalau ada defense di depan, pasang bangunan attack di paling belakang
        if (canAffordBuilding(BuildingType.ATTACK)){
            for (int i = 0; i<gameHeight; ++i){
                final int y = i;
                int myDefenseOnRow = getPlayerThingList(buildings, b->b.isEqual(y, BuildingType.DEFENSE, PlayerType.A)).size();

                if (myDefenseOnRow>0){
                    command = buildFromBacktAtRow(BuildingType.ATTACK,i);
                    if (!command.equals("")){
                        return command;
                    }
                }
            }
        }
        
        // kalau tidak ada attack musuh di row, bangun energy
        for (int i=0; i<gameHeight; ++i) {
            final int y = i;
            List<Building> friendBuildingEnergy = getPlayerThingList(buildings, b->b.isEqual(y, BuildingType.ENERGY, PlayerType.A));
            List<Building> friendBuilding = getPlayerThingList(buildings, b->b.isEqual(y, PlayerType.A));
            List<Building> opponentBuildingAttack = getPlayerThingList(buildings, b->b.isEqual(y, BuildingType.ATTACK, PlayerType.B));

            if (friendBuildingEnergy.size()==0 && (opponentBuildingAttack.size()==0 || friendBuilding.size()>0)) {
                if (canAffordBuilding(BuildingType.ENERGY)) {
                    command = buildFromBacktAtRow(BuildingType.ENERGY,i);
                    if (!command.equals("")){
                        return command;
                    }
                }
            }
            else if (friendBuildingEnergy.size()==0 && friendBuilding.size()==0){
                if (canAffordBuilding(BuildingType.DEFENSE)) {
                    command = buildFromBacktAtRow(BuildingType.DEFENSE,i);
                    if (!command.equals("")){
                        return command;
                    }
                }
            }
        }

        // kalau bisa bangun energy, bangun di baris yang attack kita lebih banyak atau tidak ada attack musuh
        if (canAffordBuilding(BuildingType.ENERGY)){
            for (int i=gameHeight-1; i>=0; --i){
                final int y = i;
                int myAttack = getPlayerThingList(buildings, b->b.isEqual(y, BuildingType.ATTACK, PlayerType.A)).size();
                int enemymyAttack = getPlayerThingList(buildings, b->b.isEqual(y, BuildingType.ATTACK, PlayerType.B)).size();

                if (myAttack>enemymyAttack || enemymyAttack == 0){
                    command = buildFromBacktAtRow(BuildingType.ENERGY,i);
                    if (!command.equals("")){
                        return command;
                    }
                }
            }
        }
        return NOTHING_COMMAND;
    }

    // mengembalikan true apabila kita dapat membangun suatu gedung
    private boolean canAffordBuilding(BuildingType buildingType) {
        return myself.energy >= gameDetails.buildingsStats.get(buildingType).price;
    }

    // mengembalikan true apabila kita dapat mengaktifkan ironcurtain
    private boolean canAffordActivatingShield() {
        return myself.energy >= gameDetails.ironCurtainStats.price && myself.ironCurtainAvailable;
    }

    // mengembalikan string command untuk membangung bangunan
    private String buildCommand(int x, int y, BuildingType buildingType) {
        return buildingType.buildCommand(x,y);
    }

    // membangun bangunan di suatu baris dari terdepan
    private String buildFromFrontAtRow(BuildingType bType, int y){
        for (int x=gameWidth/2-1; x>=0; --x){
            if (isCellEmpty(x, y)){
                return buildCommand(x, y, bType);
            }
        }
        return "";
    }

    // membangun bangunan di suatu baris dari terbelakang
    private String buildFromBacktAtRow(BuildingType bType, int y){
        for (int x=0; x<gameWidth/2; ++x){
            if (isCellEmpty(x, y)){
                return buildCommand(x, y, bType);
            }
        }
        return "";
    }

    // mengembalikan true apabila cell (x,y) kosong (tidak ada bangunan)
    private boolean isCellEmpty(int x, int y){
        return buildings.stream().filter(b->b.getX()==x && b.getY()==y).collect(Collectors.toList()).size()==0;
    }

    // mengembalikan list l yang telah difilter dengan predikat/ kondisi p
    private <T> List<T> getPlayerThingList(List<T> l, Predicate<T> p){
        return l.stream().filter(p).collect(Collectors.toList());
    }
}
