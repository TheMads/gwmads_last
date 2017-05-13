package ru.l2gw.gameserver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.lang.reflect.Constructor;

/**
 * This class defines the spawn data of a Minion type
 * In a group mob, there are one master called RaidBoss and several slaves called Minions.
 *
 * <B><U> Data</U> :</B><BR><BR>
 * <li>_minionId : The Identifier of the L2Minion to spawn </li>
 * <li>_minionAmount :  The number of this Minion Type to spawn </li><BR><BR>
 *
 */
public class L2MinionData
{
	private static final Log _log = LogFactory.getLog(L2MinionData.class);
	/** The Identifier of the L2Minion */
	public final int minionId;
	public Constructor<?> minionAi = null;

	/** The number of this Minion Type to spawn */
	public final int minionAmount;
	public final int minionRespawn;
	public final int weight_point;

	public L2MinionData(int id, String ai, int max, int respawn, int weight)
	{
		minionId = id;
		minionAmount = max;
		minionRespawn = respawn;
		weight_point = weight;

		L2NpcTemplate template = NpcTable.getTemplate(id);
		if(template == null)
		{
			_log.info("L2MinionData: no template for: " + id);
			return;
		}
		if(ai != null && !ai.isEmpty() && !ai.equals(template.ai_type))
			try
			{
				if(!ai.equalsIgnoreCase("npc"))
					minionAi = Class.forName("ru.l2gw.gameserver.ai." + ai).getConstructors()[0];
			}
			catch(Exception e)
			{
				try
				{
					minionAi = Scripts.getInstance().getClasses().get("ai." + ai).getRawClass().getConstructors()[0];
				}
				catch(Exception e1)
				{
					_log.warn(this + " AI type " + ai + " not found!");
				}
			}
	}

	@Override
	public String toString()
	{
		return "L2MinionData[id=" + minionId +";amount=" + minionAmount + ";respawn=" + minionRespawn + "]";
	}
}
