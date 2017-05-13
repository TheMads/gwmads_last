package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.model.instances.L2ItemInstance.ItemLocation;

public class PcFreight extends Warehouse
{
	//private static final Log _log = LogFactory.getLog(PcFreight.class.getName());

	private L2Player _owner; // This is the L2Player that owns this Freight;

	public PcFreight(L2Player owner)
	{
		_owner = owner;
	}

	/**
	 * Returns an int identifying the owner for this PcFreight instance
	 */
	@Override
	public int getOwnerId()
	{
		return _owner.getObjectId();
	}

	@Override
	public L2Character getOwner()
	{
		return _owner;
	}

	/**
	 * Returns an ItemLocation identifying the freight location type
	 */
	@Override
	public ItemLocation getLocationType()
	{
		return ItemLocation.FREIGHT;
	}

	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.FREIGHT;
	}

	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.FREIGHT;
	}

	/**
	 * Returns an String identifying the Location of the freight (for using with SELECT)
	 */
	@Override
	public String getLocationId()
	{
		return "0";
	}

	/**
	 * Returns an int identifying the Location of the freight (for using with INSERT/UPDATE/DELETE)
	 */
	@Override
	public byte getLocationId(boolean addItem)
	{
		return 0;
	}

	/**
	 * Sets the Location of the freight based on the place of player or _owner if player is null
	 */
	@Override
	public void setLocationId(L2Player player)
	{}

	@Override
	public final WarehouseType getWarehouseType()
	{
		return WarehouseType.FREIGHT;
	}
}
