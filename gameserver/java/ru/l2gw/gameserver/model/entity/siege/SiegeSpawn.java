package ru.l2gw.gameserver.model.entity.siege;

import ru.l2gw.util.Location;

public class SiegeSpawn
{
	Location _location;
	private int _npcId;
	private int _heading;
	private int _siegeUnitId;
	private int _hp;
	private int _locId;
	private int _amountMin;
	private int _amountMax;
	private int _controlId = 0;

	public SiegeSpawn(int siegeUnitId, int x, int y, int z, int heading, int npc_id)
	{
		_siegeUnitId = siegeUnitId;
		_location = new Location(x, y, z, heading);
		_heading = heading;
		_npcId = npc_id;
		_hp = Integer.MAX_VALUE;
		_locId = 0;
		_amountMin = 1;
		_amountMax = _amountMin;
	}

	public SiegeSpawn(int siegeUnitId, int x, int y, int z, int heading, int npc_id, int hp)
	{
		_siegeUnitId = siegeUnitId;
		_location = new Location(x, y, z, heading);
		_heading = heading;
		_npcId = npc_id;
		_hp = hp;
		_locId = 0;
		_amountMin = 1;
		_amountMax = _amountMin;
	}

	//for clanhall mobs, byte?only for no Duplacate constructor
	public SiegeSpawn(int siegeUnitId, int locId, int heading, int npc_id, byte amountMin, int amountMax)
	{
		_siegeUnitId = siegeUnitId;
		this._locId = locId;
		_heading = heading;
		_npcId = npc_id;
		_location = null;
		_hp = Integer.MAX_VALUE;
		this._amountMin = amountMin;
		this._amountMax = amountMax;
	}

	public int getSiegeUnitId()
	{
		return _siegeUnitId;
	}

	public int getNpcId()
	{
		return _npcId;
	}

	public int getHeading()
	{
		return _heading;
	}

	public int getHp()
	{
		return _hp;
	}

	public int getLocId()
	{
		return _locId;
	}

	public int getAmountMax()
	{
		return _amountMax;
	}

	public int getAmountMin()
	{
		return _amountMin;
	}

	public Location getLoc()
	{
		return _location;
	}

	public void setControlId(int controlid)
	{
		_controlId = controlid;
	}

	public int getControlId()
	{
		return _controlId;
	}
}