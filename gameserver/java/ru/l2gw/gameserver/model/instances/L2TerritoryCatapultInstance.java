package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.tables.SpawnTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

/**
 * @author rage
 * @date 09.07.2010 13:51:09
 */
public class L2TerritoryCatapultInstance extends L2TerritoryGuardInstance
{
	public L2TerritoryCatapultInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void doDie(L2Character killer)
	{
		super.doDie(killer);
		Castle castle = ResidenceManager.getInstance().getCastleById(_territoryId - 80);
		if(castle != null)
		{
			SpawnTable.getInstance().stopEventSpawn("territory_c_" + _territoryId, true);
			SpawnTable.getInstance().startEventSpawn("territory_d_" + _territoryId);
			for(L2DoorInstance door : castle.getDoors())
				if(!door.isWall())
				{
					_log.info(this + " killed, open castle door: " + door);
					door.openMe();
				}
		}
		
	}

	@Override
	public int getMaxHp()
	{
		return super.getMaxHp() * 400;
	}

	@Override
	public boolean isMovementDisabled()
	{
		return true;
	}

	@Override
	public L2Skill.TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		if(!offensive)
			return L2Skill.TargetType.invalid;

		return super.getTargetRelation(target, offensive);
	}
}
