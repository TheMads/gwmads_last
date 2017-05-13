package ru.l2gw.gameserver.model.entity;

import java.util.ArrayList;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.UnderGroundColliseumManager;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.zone.L2Zone;

/**
 * This class is simple relise of Ungerground Coliseum
 *@author FlareDrakon l2f
 */
public class Coliseum
{

	public class Battle implements Runnable
	{

		int number;

		public Battle(L2Party party, L2Party party2)
		{
			number = getcoliseummatchnumber();
			party_list.remove(party);
			for(L2Player user : party.getPartyMembers())
			{
				players_list.remove(user);
			}
			party_list.remove(party2);
			for(L2Player user : party2.getPartyMembers())
			{
				players_list.remove(user);
			}
			run();
		}

		public void run()
		{
		//startTimer()
		//SpawnLifeTowers()
		//startCompWinners()
		}

	}

	private static ArrayList<L2Player> players_list = new ArrayList<L2Player>();
	private static ArrayList<L2Party> party_list = new ArrayList<L2Party>();
	private static ArrayList<Integer[][][]> lvlsandcoord = new ArrayList<Integer[][][]>();

	private L2Zone _zone;
	private int _id = 0;
	private int minlvl = 40;
	private static Integer _event_cycle = 0;

	public Coliseum(L2Zone zone)
	{
		_id = zone.getEntityId();
		_zone = zone;
		try
		{
			load(_id);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		if(_event_cycle == 0)
			init();
	}

	private void load(int id)
	{
		lvlsandcoord.add(new Integer[40][49][id]);
		_event_cycle = getcoliseummatchnumber();//tempory need db table and save system
		if(_event_cycle == Integer.MAX_VALUE)
			_event_cycle = 1;
		else
			_event_cycle++;
	}

	private void init()
	{
		setcoliseummatchnumber(1);
	}

	private void startBattle(L2Party party, L2Party party2)
	{
		Battle b = new Battle(party, party2);
		ThreadPoolManager.getInstance().scheduleGeneral(b, 10);
	}

	public static void register(L2Player player)
	{
		party_list.add(player.getParty());
		for(L2Player member : player.getParty().getPartyMembers())
			players_list.add(member);
	}

	public int getcoliseummatchnumber()
	{
		return _event_cycle;
	}

	public void setcoliseummatchnumber(int number)
	{
		_event_cycle = number;
	}

	public static ArrayList<L2Party> getPartysList()
	{
		return party_list;
	}

	public static ArrayList<L2Player> getOponentList()
	{
		return players_list;
	}

	/** Return true if object is inside the zone */
	public boolean checkIfInZone(int x, int y)
	{
		return getZone().isInsideZone(x, y);
	}

	public int getId()
	{
		return _id;
	}

	public final L2Zone getZone()
	{
		return _zone;
	}

	public static void teleportPlayers(L2Party party, L2Party party2, int id)
	{
		//for partymamabers : party.getmambers()
		//{
		//partymamabers.teleport(getstadiumbyid(id).getZone().getRestartPoints().getFirst()
		//}
		//for partymamabers : party2.getmambers()
		//{
		//partymamabers.teleport(getstadiumbyid(id).getZone().getRestartPoints().getSecond()
		//}
		Coliseum co = UnderGroundColliseumManager.getInstance().getBuildingById(id);
		co.startBattle(party, party2);
	}
}
