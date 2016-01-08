package clusterplayer;

import battlecode.common.*;

import java.util.Random;
import java.util.Arrays;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    static RobotType[] types = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.GUARD,
            RobotType.VIPER, RobotType.TURRET};
    static RobotController rc;
    static int attackRange;
    static int sensorRange;
    static int captainID = 0;
    static RobotType type;
    static Team myTeam;
    static Team enemyTeam;

    public static void run(RobotController rcIn) {
        // You can instantiate variables here.
        rc = rcIn;
        type = rc.getType();
        attackRange = type.attackRadiusSquared;
        sensorRange = type.sensorRadiusSquared;
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();

        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case SCOUT:
                runScout();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case GUARD:
                runGuard();
                break;
            case VIPER:
                runViper();
                break;
            case TURRET:
                runTurret();
                break;
            default:
                System.out.println("help what is the robot type");
                Clock.yield();
                break;
        }
    }

    public static void runArchon() {
        int robotState = 0; /* basically the "mode" of the archon.
                        0 --> building mode, aka early game */
        int nextToBuild = 0;

        try {
            // Any code here gets executed exactly once at the beginning of the game.
            // First thing that the archon does is spawn a scout. If it's successful, it
            // broadcasts a message signal of 1 unit

            rc.setIndicatorString(0, "I'm trying something");

            for (int i = 0; i < 8; i++) {
                boolean hasBeenBuilt = false;
                Direction dirToSpawn = directions[i];
                MapLocation spawn = rc.getLocation().add(dirToSpawn);

                if (rc.onTheMap(spawn) && rc.isLocationOccupied(spawn)) {
                    rc.build(dirToSpawn, RobotType.SCOUT);
                    hasBeenBuilt = true;
                }

                if (hasBeenBuilt) {
                    rc.broadcastMessageSignal(420, 420, 1);
                    break;
                }
            }
        } catch (Exception e) {
            // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
            // Caught exceptions will result in a bytecode penalty.
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        while (true) {
            // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
            // at the end of it, the loop will iterate once per game round.
            try {
                if (robotState == 0) {
                    rc.setIndicatorString(0, "RobotState: 0");
                }
                Signal[] signals = rc.emptySignalQueue();
                if (rc.isCoreReady()) {
                    ;
                }

                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void runScout() {
        int robotState = 0;

        try {
            // Any code here gets executed exactly once at the beginning of the game.
            // Should have received an Archon message upon creation
            int[] messageSignal = rc.readSignal().getMessage();
            int[] initializeSignal = {420, 420};

            if(messageSignal == initializeSignal){
                robotState = 0;
            }
            else {
                robotState = -1; // this shouldn't ever happen...
            }
        } catch (Exception e) {
            // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
            // Caught exceptions will result in a bytecode penalty.
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        while (true) {
            // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
            // at the end of it, the loop will iterate once per game round.
            try {
                if (robotState == 0) {
                    // this is an idle stage where we wait for enough units to be within its range
                    RobotInfo[] squadron = rc.senseNearbyRobots(rc.getLocation(),
                            rc.getType().sensorRadiusSquared, rc.getTeam());

                    if (squadron.length >= 8) {
                        // we now have enough people in the squadron
                        robotState = 1; // robotState 1 means that we start navigating
                        rc.broadcastMessageSignal(1337, 1337, rc.getType().sensorRadiusSquared); // we send some
                        // type of signal to start moving the pod
                    }
                } else if (robotState == 1) {
                    // robotState = 1 means that we will look for a target and go there
                    // for now that's just going to be forward
                    MapLocation targetLocation = rc.getLocation().add(Direction.NORTH, 5);
                    if (rc.getLocation() != targetLocation){
                        rc.move(rc.getLocation().directionTo(targetLocation));
                        rc.broadcastMessageSignal(1111, 0, rc.getType().sensorRadiusSquared);
                    }
                }

                Clock.yield();

            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // this reads signals or message signals
    public static int readSignal(Signal signal) {
        try {
            int id = signal.getID();
            MapLocation loc = signal.getLocation();
            Team team = signal.getTeam();
            int[] message = signal.getMessage();

            if (message[0] == 1337 && team == rc.getTeam()) {
                captainID = id; // for now, message[0] == 1337 sets a new captain.
                // almost def not the best way to do it but whatever
                return 1337;
            }

            if (id == captainID) {
                int move_signal = 1111;

                if (message[0] == move_signal) {
                    int location = message[1]; //direction should be a coordinate to go towards
                    int x_coord = location % 128;
                    int y_coord = (location % 16384) / 128;
                    MapLocation destination = new MapLocation(x_coord, y_coord);

                    Direction travelDirection = rc.getLocation().directionTo(destination);

                    if (rc.isCoreReady()) {
                        if (rc.senseRubble(rc.getLocation().add(travelDirection)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
                            // Too much rubble, so I should clear it
                            // currently a dumb AF alg but whatever
                            rc.clearRubble(travelDirection);
                            // Check if I can move in this direction
                        } else if (rc.canMove(travelDirection)) {
                            // Move
                            rc.move(travelDirection);
                        }
                    }
                }
            } else {
                // this mean this was an enemy signal
                // pass for now
                ;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void runSoldier() {
        int robotState = 0; //soldier robot states:
        // 0 is idle
        // 1 is follow orders from scout

        try {
            ;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        while (true) {
            try{
                if (robotState == 0) { //robotState is an idle state
                    Signal first_signal = rc.readSignal();
                    if(readSignal(first_signal) == 1337) {
                        robotState = 1;
                        Clock.yield(); // set the robot's state to 1, yield the rest of the time
                    }
                } else if (robotState == 1) { // This is the "in a squadron, following a leader" stage
                    Signal[] signalList = rc.emptySignalQueue();
                    for (int i = 0; i < signalList.length; i++) {
                        readSignal(signalList[i]);
                    }
                    Clock.yield();
                } else {
                    ;
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void runGuard() {
        int robotState = 0; //soldier robot states:
        // 0 is idle
        // 1 is follow orders from scout

        try {
            ;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        while (true) {
            try{
                if (robotState == 0) { //robotState is an idle state
                    Signal first_signal = rc.readSignal();
                    if(readSignal(first_signal) == 1337) {
                        robotState = 1;
                        Clock.yield(); // set the robot's state to 1, yield the rest of the time
                    }
                } else if (robotState == 1) { // This is the "in a squadron, following a leader" stage
                    Signal[] signalList = rc.emptySignalQueue();
                    for (int i = 0; i < signalList.length; i++) {
                        readSignal(signalList[i]);
                    }
                    Clock.yield();
                } else {
                    ;
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void runViper() {
        int robotState = 0; //soldier robot states:
        // 0 is idle
        // 1 is follow orders from scout

        try {
            ;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        while (true) {
            try{
                if (robotState == 0) { //robotState is an idle state
                    Signal first_signal = rc.readSignal();
                    if(readSignal(first_signal) == 1337) {
                        robotState = 1;
                        Clock.yield(); // set the robot's state to 1, yield the rest of the time
                    }
                } else if (robotState == 1) { // This is the "in a squadron, following a leader" stage
                    Signal[] signalList = rc.emptySignalQueue();
                    for (int i = 0; i < signalList.length; i++) {
                        readSignal(signalList[i]);
                    }
                    Clock.yield();
                } else {
                    ;
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void runTurret() {
        int robotState = 0;

        try {
            ;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
