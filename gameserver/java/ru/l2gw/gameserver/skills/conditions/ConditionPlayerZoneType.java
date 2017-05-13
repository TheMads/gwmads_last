package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.skills.Env;

/**
 * Created by IntelliJ IDEA.
 * User: rage
 * Date: 21.05.2009
 * Time: 13:24:17
 */
public class ConditionPlayerZoneType extends Condition
{
	private final L2Zone.ZoneType _zoneType;
	boolean in;
	public ConditionPlayerZoneType(L2Zone.ZoneType zt,boolean in)
	{
		_zoneType = zt;
		this.in = in;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.isInZone(_zoneType) == in;
	}
}
