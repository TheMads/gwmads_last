package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.templates.L2Armor.ArmorType;

public class ConditionUsingArmor extends Condition
{
	private final ArmorType _armor;

	public ConditionUsingArmor(ArmorType armor)
	{
		_armor = armor;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.isPlayer() && ((L2Player) env.character).isWearingArmor(_armor);
	}
}
