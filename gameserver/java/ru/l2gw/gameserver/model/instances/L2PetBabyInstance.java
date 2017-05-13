package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.templates.L2PetTemplate;

public final class L2PetBabyInstance extends L2PetInstance
{
	public L2PetBabyInstance(Integer objectId, L2PetTemplate template, L2Player owner, Integer controlObjId, Long exp, Integer level)
	{
		super(objectId, template, owner, controlObjId, exp, level);
	}

	public L2PetBabyInstance(Integer objectId, L2PetTemplate template, L2Player owner, Integer itemObjId, Integer level)
	{
		super(objectId, template, owner, itemObjId, level);
	}

	@Override
	public void deleteMe()
	{
		getAI().stopAITask();
		super.deleteMe();
	}

	public int getRechargeLevel()
	{
		return Math.min(Math.max((getLevel() - 55) / 3, 1), 8);
	}

	public int getBuffLevel()
	{
		return Math.min(Math.max((getLevel() - 55) / 5, 0), 3);
	}
}