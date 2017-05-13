package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public final class L2GuardInstance extends L2NpcInstance
{
	public L2GuardInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		return forceUse || attacker.isMonster() || attacker.isPlayer() && attacker.getKarma() > 0;
	}

	@Override
	public String getHtmlPath(int npcId, int val, int karma)
	{
		String pom;
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;
		return "data/html/guard/" + pom + ".htm";
	}

	@Override
	public boolean isInvul()
	{
		return false;
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	@Override
	public boolean canMoveToHome()
	{
		return true;
	}
}