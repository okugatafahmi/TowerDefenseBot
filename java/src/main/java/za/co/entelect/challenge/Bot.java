package za.co.entelect.challenge;

import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.BuildingType;
import za.co.entelect.challenge.enums.PlayerType;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Optional;

public class Bot {
    private static final String NOTHING_COMMAND = "";
    private GameState gameState;
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
        this.gameState = gameState;
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
            if (myself.energy>=gameDetails.ironCurtainStats.price){
                command = "7,7,5";
                return command;
            }
            // jika energi kurang, maka tunggu energi cukup di turn selanjutnya
            else return NOTHING_COMMAND;
        }
        else if (myself.energy>=gameDetails.ironCurtainStats.price && myself.ironCurtainAvailable){
            return "7,7,5";
        }

        // kalau musuh ada tesla, kita buat iron curtain
        for (int i = 0; i<gameHeight; ++i){
            List<Building> buildingPlayerBtesla = getAllBuildingsInRow(PlayerType.B, i,99);
            if (buildingPlayerBtesla.size()>0){
                if (myself.energy>=gameDetails.ironCurtainStats.price){
                    command = "7,7,5";
                    return command;
                }   
                else return NOTHING_COMMAND;
            }
        }
        if (myself.energy-400>=gameDetails.buildingsStats.get(BuildingType.TESLA).price){
            int y=99,maxEnemyAttack=-1;
            for (int i = 1; i<gameHeight-1; ++i){
                int enemyAttackOnRow = getPlayerBuildingsInRow(PlayerType.B, i, BuildingType.ATTACK).size();
                if (enemyAttackOnRow>maxEnemyAttack){
                    maxEnemyAttack = enemyAttackOnRow;
                    y = i;
                }
            }

            for (int j = 2; j<gameWidth/2-2; ++j){
                if (isCellEmpty(j, y)){
                    return buildCommand(j, y, BuildingType.TESLA);
                }
            }
        }

        // langsung bangun attack ketika sudah punya cukup energy
        if (canAffordBuilding(BuildingType.ATTACK)){
            int minEnemyBuilding = 99, y = 99;
            for (int i=0; i<gameHeight; ++i){
                int enemyBuilding = getPlayerBuildingsInRow(PlayerType.B, i).size();
                if (enemyBuilding<minEnemyBuilding){
                    minEnemyBuilding = enemyBuilding;
                    y = i;
                }
            }
            return buildFromFrontAtRow(BuildingType.ATTACK, y);
        }

        // defense di row yang ada attack musuh nya
        for (int i = 0; i<gameHeight; ++i){
            List<Building> buildingPlayerAdefense = getAllBuildingsInRow(PlayerType.A, i,-1);
            List<Building> buildingPlayerBattack= getAllBuildingsInRow(PlayerType.B, i,1);
            // List<Missile> enemymMissiles = getPlayerMissiles(PlayerType.B, i);
            
            // if (buildingPlayerBattack.size()==1 && enemymMissiles.size()==1){
            //     int x = enemymMissiles.get(0).getMissilePosWhen(gameDetails.buildingsStats.get(BuildingType.ATTACK).constructionTime);
            //     if (isCellEmpty(x, i) && canAffordBuilding(BuildingType.ATTACK)){
            //         return buildCommand(x, i, BuildingType.ATTACK);
            //     }
            // }
            if (buildingPlayerAdefense.size()==0 && buildingPlayerBattack.size()>0){
                if (canAffordBuilding(BuildingType.DEFENSE)){
                    command = buildFromFrontAtRow(BuildingType.DEFENSE,i);
                    if (!command.equals("")){
                        return command;
                    }
                }
            }
        }

        // kalau tidak ada attack musuh di row, bangun energy
        for (int i=0; i<gameHeight; ++i) {
            List<Building> friendBuildingEnergy = getAllBuildingsInRow(PlayerType.A, i, 0);
            List<Building> friendBuildingDefense = getAllBuildingsInRow(PlayerType.A, i, -1);
            List<Building> opponentBuildingAttack = getAllBuildingsInRow(PlayerType.B, i, 1);

            if (friendBuildingEnergy.size()==0 && (opponentBuildingAttack.size()==0 || friendBuildingDefense.size()>0)) {
                if (canAffordBuilding(BuildingType.ENERGY)) {
                    command = buildFromBacktAtRow(BuildingType.ENERGY,i);
                    if (!command.equals("")){
                        return command;
                    }
                }
            }
            else if (friendBuildingEnergy.size()==0 && friendBuildingDefense.size()==0){
                if (canAffordBuilding(BuildingType.DEFENSE)) {
                    command = buildFromBacktAtRow(BuildingType.DEFENSE,i);
                    if (!command.equals("")){
                        return command;
                    }
                }
            }
        }

        // // kalau punya cukup energi bangun tesla, langsung bangun tesla
        // if (canAffordBuilding(BuildingType.TESLA)){
        //     for (int i=0; i<gameHeight; ++i){
        //         // bangun tesla di baris yg attack musuh paling sedikit
        //         int enemyAttackOnRow = getPlayerBuildingsInRow(PlayerType.B,i,BuildingType.ATTACK).size();

        //         if (canAffordBuilding(BuildingType.TESLA)){
        //             command = buildFromFrontAtRow(BuildingType.TESLA,i);
        //             if (!command.equals("")){
        //                 return command;
        //             }
        //         }
        //     }
        // }

        // kalau ada defense di depan, pasang bangunan attack di paling belakang
        for (int i = 0; i<gameHeight; ++i){
            int myDefenseOnRow = getPlayerBuildingsInRow(PlayerType.A,i,BuildingType.DEFENSE).size();
            // int myAttackOnRow = getPlayerBuildingsInRow(PlayerType.A,i,BuildingType.ATTACK).size();
            // int enemyAttackOnRow = getPlayerBuildingsInRow(PlayerType.B,i,BuildingType.ATTACK).size();

            if (myDefenseOnRow>0 && canAffordBuilding(BuildingType.ATTACK)){
                command = buildFromBacktAtRow(BuildingType.ATTACK,i);
                if (!command.equals("")){
                    return command;
                }
            }
        }

        // bangun random apabila tidak ada yang terpenuhi
        if (canAffordBuilding(BuildingType.ATTACK)){
            int minEnemyBuilding = 99;
            int y = 99;
            boolean sudah = false;
            for (int i=gameHeight/2-1; !sudah; i=(i+1)%gameHeight){
                int enemyBuildingOnRow = getPlayerBuildingsInRow(PlayerType.B, i).size();
                int myAttackOnRow = getPlayerBuildingsInRow(PlayerType.A, i, BuildingType.ATTACK).size();
                int enemyAttackOnRow = getPlayerBuildingsInRow(PlayerType.B, i, BuildingType.ATTACK).size();
                if (myAttackOnRow>=enemyAttackOnRow){
                    command = buildFromBacktAtRow(BuildingType.ATTACK, i);
                    if (!command.equals("")){
                        return command;
                    }
                }
                if (enemyBuildingOnRow<minEnemyBuilding){
                    minEnemyBuilding = enemyBuildingOnRow;
                    y = i;
                }
                sudah = (i==gameHeight/2-2);
            }
            return buildFromBacktAtRow(BuildingType.ATTACK,y);
        }
        // List<CellStateContainer> emptyCells = gameState.getGameMap().stream()
        //     .filter(c -> c.getBuildings().size() == 0 && c.x < (gameWidth / 2))
        //     .collect(Collectors.toList());

        // if (emptyCells.isEmpty()) {
        //     return NOTHING_COMMAND;
        // }

        //    CellStateContainer randomEmptyCell = getRandomElementOfList(emptyCells);
        //    BuildingType randomBuildingType = getRandomElementOfList(Arrays.asList(BuildingType.values()));

        //    if (!canAffordBuilding(randomBuildingType)) {
        //        return doNothingCommand();
        //    }

        //    return randomBuildingType.buildCommand(randomEmptyCell.x, randomEmptyCell.y);

        return NOTHING_COMMAND;
    }

    private boolean canAffordBuilding(BuildingType buildingType) {
        return myself.energy >= gameDetails.buildingsStats.get(buildingType).price;
    }

    private String buildCommand(int x, int y, BuildingType buildingType) {
        return buildingType.buildCommand(x,y);
    }

    private String buildFromFrontAtRow(BuildingType bType, int y){
        for (int x=gameWidth/2-1; x>=0; --x){
            if (isCellEmpty(x, y)){
                return buildCommand(x, y, bType);
            }
        }
        return "";
    }

    private String buildFromBacktAtRow(BuildingType bType, int y){
        for (int x=0; x<gameWidth/2; ++x){
            if (isCellEmpty(x, y)){
                return buildCommand(x, y, bType);
            }
        }
        return "";
    }

    private boolean isCellEmpty(int x, int y){
        List<CellStateContainer> allCell = gameState.getGameMap();

        for (int i = 0; i<allCell.size(); ++i){
            if (allCell.get(i).x ==x && allCell.get(i).y == y){
                return (allCell.get(i).getBuildings().size()==0);
            } 
        }
        return false;
    }

    private List<Building> getAllBuildingsInRow(PlayerType player, int y, int condition){
        List<CellStateContainer> allCell = gameState.getGameMap();
        List<Building> allBuildings = new ArrayList<Building>();
        for (int i = 0; i<allCell.size(); ++i){
                if (allCell.get(i).cellOwner == player && allCell.get(i).y == y){
                    allBuildings.addAll(allCell.get(i).getBuildings());
                } 
        }

        if (condition==1){ // building attack
            List<Building> allBuildingsAttack = new ArrayList<Building>();
            for (int i = 0; i<allBuildings.size(); ++i){
                if (allBuildings.get(i).buildingType == BuildingType.ATTACK){
                    allBuildingsAttack.add(allBuildings.get(i));
                }
            }
            return allBuildingsAttack;
        }
        else if (condition==-1){   // building defense
            List<Building> allBuildingsDefense = new ArrayList<Building>();
            for (int i = 0; i<allBuildings.size(); ++i){
                if (allBuildings.get(i).buildingType == BuildingType.DEFENSE){
                    allBuildingsDefense.add(allBuildings.get(i));
                }
            }
            return allBuildingsDefense;
        }

        else if (condition==99){   // building tesla
            List<Building> allBuildingsTesla = new ArrayList<Building>();
            for (int i = 0; i<allBuildings.size(); ++i){
                if (allBuildings.get(i).buildingType == BuildingType.TESLA){
                    allBuildingsTesla.add(allBuildings.get(i));
                }
            }
            return allBuildingsTesla;
        }

        else if (condition==0){   // building energy
            List<Building> allBuildingsEnergy = new ArrayList<Building>();
            for (int i = 0; i<allBuildings.size(); ++i){
                if (allBuildings.get(i).buildingType == BuildingType.ENERGY){
                    allBuildingsEnergy.add(allBuildings.get(i));
                }
            }
            return allBuildingsEnergy;
        }

        // else return all
        return allBuildings;
    }

    private List<Building> getPlayerBuildingsInRow(PlayerType player, int y, BuildingType bType){
        return buildings.stream()
                        .filter(b->b.getY()==y && b.getPlayerType()==player && b.buildingType==bType)
                        .collect(Collectors.toList());
    }

    private List<Building> getPlayerBuildingsInRow(PlayerType player, int y){
        return buildings.stream()
                        .filter(b->b.getY()==y && b.getPlayerType()==player)
                        .collect(Collectors.toList());
    }
    
    private List<Missile> getPlayerMissiles(PlayerType p, int y){
        return missiles.stream()
                        .filter(m->m.getPlayerType()==p && m.getY()==y)
                        .collect(Collectors.toList());
    }

    private <T> List<T> getPlayerThingList(List<T> l, PlayerType pType, Predicate<T> p){
        return l.stream().filter(p).collect(Collectors.toList());
    }
}
