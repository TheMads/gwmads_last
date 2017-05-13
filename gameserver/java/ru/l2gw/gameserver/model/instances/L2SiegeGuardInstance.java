package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public class L2SiegeGuardInstance extends L2MonsterInstance
{
	private SiegeUnit _siegeUnit;
	protected double _mult = 1;

	public L2SiegeGuardInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
		_canBeChamion = false;
		hasChatWindow = false;
		_cursedDrop = false;
		_showHp = true;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_siegeUnit = getBuilding(-1);
		if(_siegeUnit != null && _siegeUnit.getGuardPowerReinforce() != null)
			_mult = _siegeUnit.getOwnerId() > 0 ? _siegeUnit.getGuardPowerReinforce().getMultByLevel(_siegeUnit.getGuardPowerReinforce().getLevel()) : _siegeUnit.getGuardPowerReinforce().getMultByLevel(_siegeUnit.getGuardPowerReinforce().getMaxLevel());
	}

	public Siege getSiege()
	{
		return _siegeUnit.getSiege();
	}

	@Override
	public int getAggroRange()
	{
		return 900;
	}

	@Override
	public int getMAtk(L2Character target, L2Skill skill)
	{
		return (int)(super.getMAtk(target, skill) * _mult);
	}

	@Override
	public double getMAtkSps(L2Character target, L2Skill skill)
	{
		return (int)(super.getMAtkSps(target, skill) * _mult);
	}

	@Override
	public int getMDef(L2Character target, L2Skill skill)
	{
		return (int)(super.getMDef(target, skill) * _mult);
	}

	@Override
	public int getPAtk(L2Character target)
	{
		return (int)(super.getPAtk(target) * _mult);
	}

	@Override
	public int getPDef(L2Character target)
	{
		return (int)(super.getPDef(target) * _mult);
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
	public boolean canMoveToHome()
	{
		return true;
	}
}