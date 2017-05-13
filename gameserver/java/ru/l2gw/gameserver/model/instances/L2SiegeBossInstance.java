package ru.l2gw.gameserver.model.instances;

import javolution.util.FastMap;
import ru.l2gw.extensions.scripts.Functions;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.instancemanager.ClanHallSiegeManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public class L2SiegeBossInstance extends L2SiegeGuardInstance
{
	private Siege _siege;
	private FastMap<Integer, Integer> _attackers;

	public L2SiegeBossInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
		_canBeChamion = false;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_siege = ClanHallSiegeManager.getSiege(this);
		_attackers = new FastMap<Integer, Integer>();

		if(_siege == null)
			_log.warn(this + " has no siege!");

		if(getNpcId() == 35629 || getNpcId() == 35629)
			Functions.npcSay(this, Say2C.ALL, "Gustave's soldiers, fight! Delivers the invader to die!");
		else if(getNpcId() == 35630 || getNpcId() == 35630)
			Functions.npcSay(this, Say2C.ALL, "Qrants kingdom of Aden lion, honorable! Grants does not die $$ln Gustave to be honorable!");
		else if(getNpcId() == 35631 || getNpcId() == 35631)
			Functions.npcSay(this, Say2C.ALL, "Comes to understand! Your these foreign lands invaders! This fort forever ruler, my Gustave lifts the sword!");
	}

	@Override
	public void decreaseHp(double i, L2Character attacker, boolean directHp, boolean reflect)
	{
		super.decreaseHp(i, attacker, directHp, reflect);
		if(attacker.getPlayer() != null && _siege.checkIsAttacker(attacker.getPlayer().getClanId()))
			_attackers.put(attacker.getPlayer().getClanId(), _attackers.get(attacker.getPlayer().getClanId()) == null ? (int) i : _attackers.get(attacker.getPlayer().getClanId()) + (int) i);
	}

	@Override
	public void doDie(L2Character killer)
	{
		super.doDie(killer);
		if(_siege != null)
		{
			int clanId = 0;
			int maxDmg = 0;

			for(Integer clan : _attackers.keySet())
				if(maxDmg < _attackers.get(clan))
				{
					maxDmg = _attackers.get(clan);
					clanId = clan;
				}

			if(clanId > 0)
				_siege.getSiegeUnit().changeOwner(clanId);

			_siege.endSiege();
		}

		if(getNpcId() == 35408)
			Functions.npcSay(this, Say2C.ALL, "Has once more $$ln the defeat the shame.. But the tragedy had not ended...");
		else if(getNpcId() == 35409)
			Functions.npcSay(this, Say2C.ALL, "Is this my boundary.. But does not have Gustave's permission, I can die in no way!");
		else if(getNpcId() == 35410)
			Functions.npcSay(this, Say2C.ALL, "Day.. Unexpectedly is defeated? But I certainly can again come back! Comes back takes your head!");
	}
}
