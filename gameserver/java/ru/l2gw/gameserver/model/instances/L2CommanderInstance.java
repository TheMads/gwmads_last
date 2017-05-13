package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.NpcSay;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public class L2CommanderInstance extends L2SiegeGuardInstance
{
	private long _lastMessageSend;
	private SiegeUnit _fortress;

	public L2CommanderInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();

		_fortress = getBuilding(1);
		if(_fortress == null)
			_log.warn("Warning: " + this + " has no fortress!");

		_lastMessageSend = System.currentTimeMillis();
		if(System.currentTimeMillis() - _fortress.getSiege().getSiegeDate().getTimeInMillis() > 60000)
		{
			_fortress.getSiege().announceToAttackers(Msg.THE_BARRACKS_FUNCTION_HAS_BEEN_RESTORED);
			_fortress.getSiege().announceToDefenders(Msg.THE_BARRACKS_FUNCTION_HAS_BEEN_RESTORED);
		}
	}

	@Override
	public void doDie(L2Character killer)
	{
		super.doDie(killer);

		if(_fortress.getSiege().isInProgress())
			_fortress.getSiege().killedCommander();
	}

	@Override
	public void decreaseHp(double i, L2Character attacker, boolean directHp, boolean reflect)
	{
		super.decreaseHp(i, attacker, directHp, reflect);

		if(attacker.getPlayer() != null && _lastMessageSend < System.currentTimeMillis() && Rnd.chance(30))
		{
			_lastMessageSend = System.currentTimeMillis() + 60000;
			int rnd = Rnd.get(3);
			CustomMessage cm = new CustomMessage("FortressCapitanAttack" + rnd, Config.DEFAULT_LANG);
			if(rnd == 1)
				cm.addString(attacker.getPlayer().getName());

			broadcastPacket(new NpcSay(this, Say2C.SHOUT, cm.toString()));
		}
	}
}