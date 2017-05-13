package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.model.instances.L2ItemInstance.ItemLocation;

public final class ClanWarehouse extends Warehouse
{
	private L2Clan _clan;

	public ClanWarehouse(L2Clan clan)
	{
		_clan = clan;
	}

	@Override
	public int getOwnerId()
	{
		return _clan.getClanId();
	}

	@Override
	public L2Character getOwner()
	{
		return _clan.getLeader().getPlayer();	
	}

	@Override
	public ItemLocation getLocationType()
	{
		return ItemLocation.CLANWH;
	}

	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.CLANWH;
	}

	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.CLANWH;
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
		return WarehouseType.CLAN;
	}
}