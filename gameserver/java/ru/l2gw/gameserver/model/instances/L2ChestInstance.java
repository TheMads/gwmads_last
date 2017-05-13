package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public class L2ChestInstance extends L2MonsterInstance
{
	public L2ChestInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public boolean canMoveToHome()
	{
		return false;
	}

	@Override
	public L2Skill.TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		if(target == this)
			return L2Skill.TargetType.self;

		if(isDead())
			return L2Skill.TargetType.npc_body;

		return L2Skill.TargetType.treasure;
	}
}