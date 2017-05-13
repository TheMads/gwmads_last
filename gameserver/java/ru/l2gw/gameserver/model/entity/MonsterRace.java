package ru.l2gw.gameserver.model.entity;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.lang.reflect.Constructor;

@SuppressWarnings( { "nls", "unqualified-field-access", "boxing" })
public class MonsterRace
{
	private L2NpcInstance[] monsters;
	private static MonsterRace _instance;
	private Constructor<?> _constructor;
	private int[][] speeds;
	private int[] first, second;

	private MonsterRace()
	{
		monsters = new L2NpcInstance[8];
		speeds = new int[8][20];
		first = new int[2];
		second = new int[2];
	}

	public static MonsterRace getInstance()
	{
		if(_instance == null)
			_instance = new MonsterRace();
		return _instance;
	}

	public void newRace()
	{
		int random = 0;

		for(int i = 0; i < 8; i++)
		{
			int id = 31003;
			random = Rnd.get(25);
			for(int j = i - 1; j >= 0; j--)
				if(monsters[j].getTemplate().npcId == id + random)
					random = Rnd.get(25);
			try
			{
				L2NpcTemplate template = NpcTable.getTemplate((short) (id + random));
				_constructor = Class.forName("ru.l2gw.gameserver.model.instances." + template.type + "Instance").getConstructors()[0];
				int objectId = IdFactory.getInstance().getNextId();
				monsters[i] = (L2NpcInstance) _constructor.newInstance(objectId, template, 0L, 0L, 0L, 0L);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		newSpeeds();
	}

	public void newSpeeds()
	{
		speeds = new int[8][20];
		int total = 0;
		for(int i = 0; i < 8; i++)
		{
			total = 0;
			for(int j = 0; j < 20; j++)
			{
				if(j == 19)
					speeds[i][j] = 100;
				else
					speeds[i][j] = Rnd.get(65, 124);
				total += speeds[i][j];
			}
			if(total > first[1])
			{
				second[0] = first[0];
				second[1] = first[1];
				first[0] = 8 - i;
				first[1] = total;
			}
			else if(total > second[1])
			{
				second[0] = 8 - i;
				second[1] = total;
			}

		}
	}

	/**
	 * @return Returns the monsters.
	 */
	public L2NpcInstance[] getMonsters()
	{
		return monsters;
	}

	/**
	 * @return Returns the speeds.
	 */
	public int[][] getSpeeds()
	{
		return speeds;
	}

	public int getFirstPlace()
	{
		return first[0];
	}

	public int getSecondPlace()
	{
		return second[0];
	}
}
