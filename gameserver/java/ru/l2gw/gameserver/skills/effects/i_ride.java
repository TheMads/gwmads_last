package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.tables.PetDataTable;

/**
 * @author: rage
 * @date: 15.07.2010 13:38:52
 */
public class i_ride extends i_effect
{
	public i_ride(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(cha.isPlayer())
		{
			if(getSkill().getNpcId() > 0)
				cha.getPlayer().getMountEngine().setMount(PetDataTable.getInstance().getInfo(getSkill().getNpcId(), cha.getPlayer().getLevel()), 0);
			else
				cha.getPlayer().getMountEngine().dismount();
		}
	}
}
