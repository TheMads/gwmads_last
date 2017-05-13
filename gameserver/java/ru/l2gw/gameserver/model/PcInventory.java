package ru.l2gw.gameserver.model;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2ItemInstance.ItemLocation;
import ru.l2gw.gameserver.serverpackets.ExQuestItemList;
import ru.l2gw.gameserver.serverpackets.ItemList;
import ru.l2gw.gameserver.serverpackets.ExAdenaInvenCount;
import ru.l2gw.gameserver.templates.L2Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class PcInventory extends Inventory
{

	private final L2Player _owner;
	public static final int ADENA_ID = 57;

	public PcInventory(L2Player owner)
	{
		_owner = owner;
	}

	@Override
	public L2Player getOwner()
	{
		return _owner;
	}

	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.INVENTORY;
	}

	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PAPERDOLL;
	}

	public long getAdena()
	{
		L2ItemInstance _adena = getItemByItemId(ADENA_ID);
		if(_adena == null)
			return 0;
		return _adena.getCount();
	}

	/**
	 * Get all augmented items
	 */
	public ArrayList<L2ItemInstance> getAugmentedItems()
	{
		ArrayList<L2ItemInstance> list = new ArrayList<L2ItemInstance>();
		for(L2ItemInstance item : _items)
			if(item != null && item.isAugmented())
				list.add(item);
		return list;
	}

	@Override
	protected void addItem(L2ItemInstance item)
	{
		if(item.isShadowItem() && item.isEquipped() || item.isTemporalItem())
			item.startLifeTask();

		_items.add(item);
	}

	/**
	 * Adds adena to PCInventory
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAdena(String process, long count, L2Player actor, L2Object reference)
	{
		if(count > 0)
			addItem(process, ADENA_ID, count, actor, reference);
	}

	public void reduceAdena(String process, long count, L2Player actor, L2Object reference)
	{
		if(count > 0)
			destroyItemByItemId(process, ADENA_ID, count, actor, reference);
	}

	public static int[][] restoreVisibleInventory(int objectId)
	{
		int[][] paperdoll = new int[Inventory.PAPERDOLL_MAX][4];
		Connection con = null;
		PreparedStatement statement2 = null;
		ResultSet invdata = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement2 = con.prepareStatement("SELECT i.object_id,i.item_id,i.loc_data,i.enchant_level,a.attributes FROM items i LEFT OUTER JOIN augmentations a on(i.object_id = a.item_id) WHERE owner_id=? and loc='PAPERDOLL'");
			statement2.setInt(1, objectId);
			invdata = statement2.executeQuery();

			while(invdata.next())
			{
				int slot = invdata.getInt("loc_data");
				paperdoll[slot][0] = invdata.getInt("object_id");
				paperdoll[slot][1] = invdata.getInt("item_id");
				paperdoll[slot][2] = invdata.getInt("enchant_level");
				paperdoll[slot][3] = invdata.getInt("attributes");
			}
		}
		catch(Exception e)
		{
			_log.warn("could not restore inventory:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement2, invdata);
		}
		return paperdoll;
	}

	public boolean validateCapacity(L2ItemInstance item)
	{
		int slots = _items.size() - getQuestItemsCount();

		if(!(item.isStackable() && getItemByItemId(item.getItemId()) != null))
			slots++;

		return validateCapacity(slots);
	}

	public boolean validateCapacity(GArray<L2ItemInstance> items)
	{
		int slots = _items.size() - getQuestItemsCount();

		for(L2ItemInstance item : items)
			if(!(item.isStackable() && getItemByItemId(item.getItemId()) != null))
				slots++;

		return validateCapacity(slots);
	}

	public boolean validateCapacity(int slots)
	{
		return slots <= _owner.getInventoryLimit();
	}

	public short slotsLeft()
	{
		short slots = (short) (_owner.getInventoryLimit() - (_items.size() - getQuestItemsCount()));
		return slots > 0 ? slots : 0;
	}

	public boolean validateWeight(L2ItemInstance item)
	{
		long weight = item.getItem().getWeight() * item.getCount();
		return validateWeight(weight);
	}

	public boolean validateWeight(long weight)
	{
		return getTotalWeight() + weight <= _owner.getMaxLoad();
	}

	public L2ItemInstance getEquippedItemBySkill(L2Skill skill)
	{
		for(L2ItemInstance item : getPaperdollItems())
			if(item.getItem().getAttachedSkills() != null)
				for(L2Skill itemSkill : item.getItem().getAttachedSkills())
					if(itemSkill == skill)
						return item;

		return null;
	}

	public void sendItemList(boolean showWindow)
	{
		GArray<L2ItemInstance> items = new GArray<L2ItemInstance>();
		GArray<L2ItemInstance> questItems = new GArray<L2ItemInstance>();

		for(L2ItemInstance item : _items)
			if(item.getItem().getType2() == L2Item.TYPE2_QUEST)
				questItems.add(item);
			else
				items.add(item);

		_owner.sendPacket(new ItemList(items, showWindow));
		_owner.sendPacket(new ExQuestItemList(questItems));
		//sendPacket(new ExAdenaInvenCount(this));
	}

	public int getQuestItemsCount()
	{
		int c = 0;

		for(L2ItemInstance item : _items)
			if(item.getItem().getType2() == L2Item.TYPE2_QUEST)
				c++;

		return c;
	}
}