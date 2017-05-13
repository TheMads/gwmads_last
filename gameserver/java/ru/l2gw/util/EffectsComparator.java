package ru.l2gw.util;

import ru.l2gw.gameserver.model.L2Effect;

import java.util.Comparator;

public class EffectsComparator implements Comparator<L2Effect>
{
	private static final EffectsComparator instance = new EffectsComparator();

	public static final EffectsComparator getInstance()
	{
		return instance;
	}

	public int compare(L2Effect o1, L2Effect o2)
	{
		if(o1 == null || o2 == null)
			return 0;

		if(o1.getSkill().isDebuff() && !o2.getSkill().isDebuff())
			return 1;

		if(!o1.getSkill().isDebuff() && o2.getSkill().isDebuff())
			return -1;

		if(o1.getSkill().isTriggered() && !o2.getSkill().isTriggered())
			return 1;

		if(!o1.getSkill().isTriggered() && o2.getSkill().isTriggered())
			return -1;

		if(o1.getSkill().isSongDance() && !o2.getSkill().isSongDance())
			return 1;

		if(!o1.getSkill().isSongDance() && o2.getSkill().isSongDance())
			return -1;

		if(o1.getSkill().isToggle() && !o2.getSkill().isToggle())
			return 1;

		if(!o1.getSkill().isToggle() && o2.getSkill().isToggle())
			return -1;

		if(o1.getEffectStartTime() > o2.getEffectStartTime())
			return 1;

		if(o1.getEffectStartTime() < o2.getEffectStartTime())
			return -1;

		return 0;
	}
}