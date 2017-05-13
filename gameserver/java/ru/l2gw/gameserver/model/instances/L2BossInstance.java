package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public class L2BossInstance extends L2RaidBossInstance
{
	private boolean _teleportedToNest;
	private boolean _alreadyAttacked;

	private static final int BOSS_MAINTENANCE_INTERVAL = 10000;

	/**
	 * Constructor for L2BossInstance. This represent all grandbosses:
	 * <ul>
	 * <li>29001	Queen Ant</li>
	 * <li>29014	Orfen</li>
	 * <li>29019	Antharas</li>
	 * <li>29020	Baium</li>
	 * <li>29022	Zaken</li>
	 * <li>29028	Valakas</li>
	 * <li>29006	Core</li>
	 * </ul>
	 * <br>
	 * <b>For now it's nothing more than a L2Monster but there'll be a scripting<br>
	 * engine for AI soon and we could add special behaviour for those boss</b><br>
	 * <br>
	 * @param objectId ID of the instance
	 * @param template L2NpcTemplate of the instance
	 */
	public L2BossInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	protected int getMaintenanceInterval()
	{
		return BOSS_MAINTENANCE_INTERVAL;
	}

	@Override
	public final boolean isMovementDisabled()
	{
		// Core should stay anyway
		return getNpcId() == 29006 || super.isMovementDisabled();
	}

	public void setAttacked(boolean param)
	{

		_alreadyAttacked = param;

	}

	public boolean getAttacked()
	{

		return _alreadyAttacked;

	}

	/**
	 * Used by Orfen to set 'teleported' flag, when hp goes to <50%
	 * @param flag
	 */
	public void setTeleported(boolean flag)
	{
		_teleportedToNest = flag;
	}

	public boolean getTeleported()
	{
		return _teleportedToNest;
	}

	/**
	 * Reduce the current HP of the L2NpcInstance, update its _aggroList and launch the doDie Task if necessary.<BR><BR>
	 *
	 */
	@Override
	public void decreaseHp(double damage, L2Character attacker, boolean directHp, boolean reflect)
	{
		if(getTemplate().npcId == 29014 && getCurrentHp() - damage < getMaxHp() / 2 && !getTeleported()) // Orfen
		{
			stopHate();
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null, null);
			teleToLocation(43577, 15985, -4396);
			setTeleported(true);
		}

		super.decreaseHp(damage, attacker, directHp, reflect);
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}

	@Override
	public boolean canMoveToHome()
	{
		return false;
	}
}
