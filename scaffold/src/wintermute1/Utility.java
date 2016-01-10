package wintermute1;
import battlecode.common.*;
import java.util.*;

public class Utility 
{
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
