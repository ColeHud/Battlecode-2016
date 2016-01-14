package wintermute1;
import battlecode.common.*;
import java.util.*;

public class Utility 
{
	//message ints
	public static final int ZOMBIE_DEN_CODE = 84031;
	public static final int ENEMY_ARCHON_CODE = 42650;
	public static final int PARTS_CODE = 94572;
	public static final int SOLDIER_HELP_CODE = 97525;
	
	public static final double PERCENTAGE_TURRETS = .3;
	public static final double PERCENTAGE_SOLDIERS = .65;
	public static final double PERCENTAGE_SCOUTS = 0.05;
	public static final int MAX_FOES_TO_BUILD = 4;
	public static final int NULL_LOC_COORD = 35497234;
	
	//get an arraylist of directions
	public static ArrayList<Direction> arrayListOfDirections()
	{
		ArrayList<Direction> directions = new ArrayList<Direction>();
		Direction[] arrayDirections = Direction.values();
		for(Direction dir : arrayDirections)
		{
			directions.add(dir);
		}
		return directions;
	}
	
	//maplocation array to arraylist
	public static ArrayList<MapLocation> arrayToArrayList(MapLocation[] locations)
	{
		ArrayList<MapLocation> returnArrayList = new ArrayList<MapLocation>();
		for(MapLocation location : locations)
		{
			returnArrayList.add(location);
		}
		return returnArrayList;
	}
}
