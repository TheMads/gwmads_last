package ru.l2gw.gameserver.skills;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

/**
 *
 * An Env object is just a class to pass parameters to a calculator such as L2Player,
 * L2ItemInstance, Initial value.
 *
 */
public final class Env
{
	public L2Character character;
	public L2Character target;
	public L2ItemInstance item;
	public L2Skill skill;
	public double value;
	public double effectTime;
	//public double baseValue;
	public boolean first;
	public boolean success;
	public boolean crit;

	public Env()
	{}

	public Env(L2Character cha, L2Character tar, L2Skill sk)
	{
		character = cha;
		target = tar;
		skill = sk;
		effectTime = 1;
		success = true;
	}
}
