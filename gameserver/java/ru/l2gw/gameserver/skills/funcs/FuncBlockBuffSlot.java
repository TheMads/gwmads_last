package ru.l2gw.gameserver.skills.funcs;

import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.templates.StatsSet;

/**
 * @author: rage
 * @date: 24.01.12 16:43
 */
public class FuncBlockBuffSlot extends Func
{
	private String[] buffSlots;

	public FuncBlockBuffSlot(Stats stat, int order, Object owner, double value)
	{
		super(Stats.BLOCK_BUFF_SLOT, order, owner, value);
	}

	@Override
	public void setAttributes(StatsSet set)
	{
		buffSlots = set.getString("slot", "").split(";");
	}

	@Override
	public void calc(Env env)
	{
		if(buffSlots == null || buffSlots.length < 1 || env.skill == null)
			return;

		for(String at : buffSlots)
			if(env.skill.getAbnormalTypes().contains(at))
			{
				env.success = false;
				return;
			}
	}
}
