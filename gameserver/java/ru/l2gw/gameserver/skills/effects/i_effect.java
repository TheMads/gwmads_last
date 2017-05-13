package ru.l2gw.gameserver.skills.effects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 10.07.2010 15:28:30
 */
public abstract class i_effect
{
	protected final static Log _log = LogFactory.getLog(i_effect.class);
	protected final EffectTemplate _template;

	public i_effect(EffectTemplate template)
	{
		_template = template;
	}
/*
	public EffectType getEffectType()
	{
		return EffectType.instant;
	}
*/
	public abstract void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter);

	public L2Skill getSkill()
	{
		return _template._skill;
	}

	public double calc()
	{
		return _template._val;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " " + getSkill();
	}
}
