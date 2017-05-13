package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

/**
 * @author: rage
 * @date: 10.07.2010 14:25:44
 */
public class L2TerritoryOutpostInstance extends L2NpcInstance
{
	private final L2Clan _owner;

	public L2TerritoryOutpostInstance(L2Clan owner, int objectId, L2NpcTemplate template)
	{
		super(objectId, template, 0L, 0L, 0L, 0L);
		_owner = owner;
		hasChatWindow = false;
		setDisplayId(getNpcId());
	}

	public L2Clan getOwner()
	{
		return _owner;
	}

	@Override
	public String getName()
	{
		return _owner.getName();
	}

	@Override
	public String getTitle()
	{
		return "";
	}

	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		return false;
	}

	@Override
	public void doDie(L2Character killer)
	{
		if(_owner != null)
			_owner.setCamp(null);

		super.doDie(killer);
	}

	@Override
	public L2Skill.TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		return L2Skill.TargetType.target;
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}

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
	public boolean isMovementDisabled()
	{
		return true;
	}

	@Override
	public boolean isLethalImmune()
	{
		return true;
	}
}
