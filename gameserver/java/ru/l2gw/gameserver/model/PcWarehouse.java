package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.model.instances.L2ItemInstance.ItemLocation;

public class PcWarehouse extends Warehouse
{
	private L2Player _owner;

	public PcWarehouse(L2Player owner)
	{
		_owner = owner;
	}

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
	
	@Override
	public ItemLocation getLocationType()
	{
		return ItemLocation.WAREHOUSE;
	}

	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.WAREHOUSE;
	}

	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.WAREHOUSE;
	}

	@Override
	public String getLocationId()
	{
		return "0";
	}

	@Override
	public byte getLocationId(@SuppressWarnings("unused") boolean dummy)
	{
		return 0;
	}

	@Override
	public void setLocationId(@SuppressWarnings("unused") L2Player dummy)
	{}

	@Override
	public final WarehouseType getWarehouseType()
	{
		return WarehouseType.PRIVATE;
	}
}