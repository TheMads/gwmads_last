package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

/**
 * @author rage
 * @date 02.07.2009 12:35:27
 */
public class L2BallistaInstance extends L2SiegeGuardInstance
{
	public L2BallistaInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void decreaseHp(double i, L2Character attacker, boolean directHp, boolean reflect)
	{
		L2Player player = null;

		if(attacker instanceof L2Playable)
			player = attacker.getPlayer();

		SiegeUnit fort = getBuilding(1);

		if(fort != null && player != null && fort.getSiege().isInProgress() && fort.getSiege().checkIsAttacker(player.getClanId()))
			super.decreaseHp(i, attacker, directHp, reflect);
	}

	@Override
	public void doDie(L2Character killer)
	{
		super.doDie(killer);

		L2Player player = null;
		if(killer != null)
			player = killer.getPlayer();

		if(player != null && player.getClanId() != 0 && player.getClan().getLevel() > 4 && getBuilding(-1) != null && getBuilding(-1).isFort && getBuilding(-1).getSiege().isInProgress())
		{
			player.getClan().incReputation(25, false, "BallistaKill");
			player.sendPacket(new SystemMessage(SystemMessage.THE_BALLISTA_HAS_BEEN_SUCCESSFULLY_DESTROYED_AND_THE_CLANS_REPUTATION_WILL_BE_INCREASED));
		}
	}

	@Override
	public boolean canMoveToHome()
	{
		return false;
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	@Override
	public boolean isLethalImmune()
	{
		return true;
	}

	@Override
	public L2Skill.TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		if(!offensive)
			return L2Skill.TargetType.invalid;

		return super.getTargetRelation(target, offensive);
	}
}
