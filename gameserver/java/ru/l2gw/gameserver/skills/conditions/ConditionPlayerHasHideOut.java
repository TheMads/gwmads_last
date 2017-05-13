package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 25.11.2009 11:25:12
 */
public class ConditionPlayerHasHideOut extends Condition
{
	private int _unitId;

	public ConditionPlayerHasHideOut(int unitId)
	{
		_unitId = unitId;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.character.isPlayer())
		{
			L2Clan clan = ((L2Player) env.character).getClan();
			if(clan != null)
			{
				if(clan.getHasCastle() == _unitId)
					return true;
				else if(clan.getHasFortress() == _unitId)
					return true;
				else if(clan.getHasHideout() == _unitId)
					return true;
			}
		}
		return false;
	}
}
