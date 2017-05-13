package ru.l2gw.gameserver.skills.effects;

import javolution.util.FastMap;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author: rage
 * @date: 18.11.2009 16:19:08
 */
public class i_unlock extends i_effect
{
	private FastMap<Integer, Integer> _chanses;

	public i_unlock(EffectTemplate template)
	{
		super(template);
		_chanses = new FastMap<Integer, Integer>();
		if(template._options != null)
			for(String lvlChance : template._options.split(";"))
			{
				String lvl[] = lvlChance.split("-");
				_chanses.put(Integer.parseInt(lvl[0]), Integer.parseInt(lvl[1]));
			}
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target instanceof L2DoorInstance)
			{
				L2DoorInstance door = (L2DoorInstance) env.target;
				if(!door.isOpen())
				{
					if(_chanses.containsKey(door.getGrade()) && Rnd.chance(_chanses.get(door.getGrade())))
					{
						door.openMe();
						door.onOpen();
					}
					else
						cha.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_UNLOCK_THE_DOOR));
				}
			}
	}
}
