package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public class L2SiegeHeadquarterInstance extends L2NpcInstance
{
	private L2Clan _owner;
	private long _lastAnnouncedAttackedTime = 0;

	public L2SiegeHeadquarterInstance(L2Clan owner, int objectId, L2NpcTemplate template)
	{
		super(objectId, template, 0L, 0L, 0L, 0L);
		_owner = owner;
		_showHp = true;
		hasChatWindow = false;
		setDisplayId(getNpcId());
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
		L2Player player = attacker.getPlayer();
		if(player == null)
			return false;

		L2Clan clan = player.getClan();
		return clan == null || _owner.getLeaderId() != clan.getLeaderId();
	}

	@Override
	public void doDie(L2Character killer)
	{
		if(_owner != null)
			_owner.setCamp(null);

		super.doDie(killer);
	}

	@Override
	public void decreaseHp(final double damage, final L2Character attacker, boolean directHp, boolean reflect)
	{
		if(System.currentTimeMillis() - _lastAnnouncedAttackedTime > 120000)
		{
			_lastAnnouncedAttackedTime = System.currentTimeMillis();
			_owner.broadcastToOnlineMembers(new SystemMessage(SystemMessage.YOUR_BASE_IS_BEING_ATTACKED));
		}

		if(attacker.getPlayer() != null && attacker.getPlayer().getClan() == _owner)
			return;

		super.decreaseHp(damage, attacker, directHp, reflect);
	}

	@Override
	public L2Skill.TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		if(target.getPlayer() == null || target.getPlayer().getClan() == _owner)
			return L2Skill.TargetType.invalid;

		return L2Skill.TargetType.enemy_only;
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
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