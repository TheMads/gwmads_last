package ru.l2gw.gameserver.model.entity.siege.reinforce;

/**
 * @author rage
 * @date 30.06.2009 11:19:58
 */
public class GuardReinforce extends Reinforce
{
	public GuardReinforce(int id, int level, int siegeUnitId)
	{
		super(id, level, siegeUnitId);
	}

	public void setActive(boolean active)
	{ }

	public String getType()
	{
		return "GUARD";
	}
}
