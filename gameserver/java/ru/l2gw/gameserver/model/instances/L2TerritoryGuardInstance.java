package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

/**
 * @author rage
 * @date 09.07.2010 13:30:02
 */
public class L2TerritoryGuardInstance extends L2SiegeGuardInstance
{
	public L2TerritoryGuardInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_mult = 1;
	}

	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		return TerritoryWarManager.getWar().isInProgress() && attacker.getPlayer() != null && attacker.getPlayer().getTerritoryId() > 0 && _territoryId != attacker.getPlayer().getTerritoryId();
	}

	@Override
	public L2Skill.TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		if(!offensive)
			return L2Skill.TargetType.invalid;

		return super.getTargetRelation(target, offensive);
	}
}
