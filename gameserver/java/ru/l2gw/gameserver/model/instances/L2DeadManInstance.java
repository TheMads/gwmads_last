package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.ai.L2CharacterAI;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.Die;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public class L2DeadManInstance extends L2MonsterInstance
{
	public L2DeadManInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	/**
	 * Return the L2CharacterAI of the L2Character and if its null create a new one.<BR><BR>
	 */
	@Override
	public L2CharacterAI getAI()
	{
		if(_ai == null)
			_ai = new L2CharacterAI(this);
		return _ai;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		stopHpMpRegeneration();
		setCurrentHp(0);
		setDead(true);
		broadcastStatusUpdate();
		broadcastPacket(new Die(this));
		setWalking();
	}

	@Override
	public void decreaseHp(double damage, L2Character attacker, boolean directHp, boolean reflect)
	{}

	@Override
	public void doDie(L2Character killer)
	{}

	@Override
	public int getAggroRange()
	{
		return 0;
	}

	@Override
	public boolean canMoveToHome()
	{
		return false;
	}

}