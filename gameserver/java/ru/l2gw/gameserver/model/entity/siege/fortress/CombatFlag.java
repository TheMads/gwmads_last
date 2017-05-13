package ru.l2gw.gameserver.model.entity.siege.fortress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.instancemanager.FortressSiegeManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.InventoryUpdate;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.util.Location;

public class CombatFlag
{
	protected static Log _log = LogFactory.getLog(CombatFlag.class.getName());

	public L2Player _player = null;
	public int playerId = 0;
	public L2ItemInstance _item = null;
	private Location _location;
	public L2ItemInstance itemInstance;
	public int _itemId;

	@SuppressWarnings("unused")
	private int _heading;
	@SuppressWarnings("unused")
	private int _fortId;

	public CombatFlag(int fort_id, int x, int y, int z, int heading, int item_id)
	{
		_fortId = fort_id;
		_location = new Location(x, y, z, heading);
		_heading = heading;
		_itemId = item_id;
	}

	public synchronized void spawnMe()
	{
		// Init the dropped L2ItemInstance and add it in the world as a visible object at the position where mob was last
		itemInstance = ItemTable.getInstance().createItem("Combat", _itemId, 1, null, null);
		itemInstance.spawnMe(_location);
	}

	public synchronized void unSpawnMe()
	{
		if(_player != null)
			dropIt();
		if(itemInstance != null)
			itemInstance.decayMe();
	}

	public void activate(L2Player player, L2ItemInstance item)
	{
		// Player holding it data
		_player = player;
		playerId = player.getObjectId();
		itemInstance = null;

		// Add skill
		giveSkill();

		// Equip with the weapon
		_item = item;
		InventoryUpdate iu = new InventoryUpdate(_player.getInventory().equipItemAndRecord(_item));
		_player.sendChanges();
		_player.sendPacket(iu);
		SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_EQUIPPED_YOUR_S1);
		sm.addItemName(_item.getItemId());
		_player.sendPacket(sm);

		//_player.sendPacket(new ItemList(_player, false));
		Siege siege = FortressSiegeManager.getSiege(player);
		sm = new SystemMessage(SystemMessage.C1_HAS_ACQUIRED_THE_FLAG).addCharName(_player);
		siege.announceToAttackers(sm);
		siege.announceToDefenders(sm);

		// Refresh player stats
		_player.broadcastUserInfo(true);
		_player.setCombatFlagEquipped(true);
	}

	public void checkPlayer(L2Player player, L2ItemInstance item)
	{
		if(player == null || item == null)
			return;

		if(FortressSiegeManager.getInstance().checkIfCanPickup(player))
			activate(player, item);
		else
		{
			// wtf? how you get it?
			_log.warn("FortressManager: " + player + " tried to obtain " + item + " in wrong way");
			player.getInventory().destroyItem("CombatFlag", item, player, null);
		}
	}

	public void dropIt()
	{
		// Reset player stats
		_player.setCombatFlagEquipped(false);
		removeSkill();
		_player.getInventory().destroyItem("CombatFlag", _item.getObjectId(), 1, _player, null);
		_item = null;
		_player.broadcastUserInfo(true);
		_player = null;
		playerId = 0;
	}

	public void giveSkill()
	{
		_player.addSkill(SkillTable.getInstance().getInfo(3318, 1), false);
		_player.addSkill(SkillTable.getInstance().getInfo(3358, 1), false);
		_player.sendPacket(new SkillList(_player));
	}

	public void removeSkill()
	{
		_player.removeSkill(SkillTable.getInstance().getInfo(3318, 1), false);
		_player.removeSkill(SkillTable.getInstance().getInfo(3358, 1), false);
		_player.sendPacket(new SkillList(_player));
	}
}