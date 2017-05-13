package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.ai.L2CharacterAI;
import ru.l2gw.gameserver.ai.L2StaticObjectAI;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public final class L2ArtefactInstance extends L2NpcInstance
{
	//private static Log _log = LogFactory.getLog(L2GuardInstance.class.getName());

	public L2ArtefactInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
		hasChatWindow = false;
	}

	@Override
	public L2CharacterAI getAI()
	{
		if(_ai == null)
			_ai = new L2StaticObjectAI(this);
		return _ai;
	}

	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		if(sendMessage)
			attacker.sendPacket(Msg.INVALID_TARGET);
		return false;
	}

	/**
	 * Артефакт нельзя убить
	 */
	@Override
	public void doDie(L2Character killer)
	{}

	@Override
	public boolean isInvul()
	{
		return true;
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
		return L2Skill.TargetType.holything;
	}
}
