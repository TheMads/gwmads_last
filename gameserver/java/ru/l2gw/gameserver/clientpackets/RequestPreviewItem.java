package ru.l2gw.gameserver.clientpackets;

import gnu.trove.map.hash.TIntIntHashMap;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.controllers.TradeController;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.NpcTradeList;
import ru.l2gw.gameserver.model.instances.L2ClanBaseManagerInstance;
import ru.l2gw.gameserver.model.instances.L2MercManagerInstance;
import ru.l2gw.gameserver.model.instances.L2MerchantInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.ShopPreviewInfo;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Armor;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.L2Weapon;

import java.lang.ref.WeakReference;

public class RequestPreviewItem extends L2GameClientPacket
{
	private static final TIntIntHashMap _previewOrder = new TIntIntHashMap(23);

	private int _listId;
	private int _count;
	private int[] _items; // count*2

	class RemoveWearItemsTask implements Runnable
	{
		private WeakReference<L2Player> _player;

		public RemoveWearItemsTask(L2Player player)
		{
			_player = new WeakReference<L2Player>(player);
		}

		public void run()
		{
			L2Player player = _player.get();
			if(player != null)
			{
				player.sendPacket(Msg.YOU_ARE_NO_LONGER_TRYING_ON_EQUIPMENT);
				player.sendUserInfo(true);
			}
		}
	}

	@Override
	public void readImpl()
	{
		readD();
		_listId = readD();
		_count = readD();
		if(_count * 4 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_count = 0;
			return;
		}

		_items = new int[_count];
		for(int i = 0; i < _count; i++)
			_items[i] = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0 && !player.isGM())
		{
			player.sendActionFailed();
			return;
		}
		L2NpcInstance npc = player.getLastNpc();

		boolean isValidMerchant = npc instanceof L2ClanBaseManagerInstance || npc instanceof L2MerchantInstance || npc instanceof L2MercManagerInstance;

		if(!player.isGM() && (npc == null || !isValidMerchant || !player.isInRange(npc, player.getInteractDistance(npc))))
		{
			player.sendActionFailed();
			return;
		}

		if(_count < 1)
		{
			player.sendActionFailed();
			return;
		}

		NpcTradeList list = TradeController.getInstance().getSellList(_listId);

		if(list == null)
		{
			player.sendActionFailed();
			return;
		}

		boolean wear = false;
		int[] previewItems = new int[29];

		for(int itemId : _items)
		{
			L2Item item = ItemTable.getInstance().getTemplate(itemId);

			if(!(item instanceof L2Weapon) && !(item instanceof L2Armor) || !_previewOrder.containsKey(item.getBodyPart()) || list.getTradeItem(itemId) == null)
				continue;

			int slot = _previewOrder.get(item.getBodyPart());
			if(slot == 15) // Full Body Armor
			{
				if(previewItems[15] != 0 || previewItems[10] != 0)
				{
					player.sendPacket(Msg.THOSE_ITEMS_MAY_NOT_BE_TRIED_ON_SIMULTANEOUSLY);
					return;
				}
				previewItems[10] = itemId;
				previewItems[15] = itemId;
				wear = true;
			}
			else if(slot == 29) // LRHAND Slot
			{
				if(previewItems[7] != 0 || previewItems[8] != 0)
				{
					player.sendPacket(Msg.THOSE_ITEMS_MAY_NOT_BE_TRIED_ON_SIMULTANEOUSLY);
					return;
				}
				previewItems[7] = itemId;
				previewItems[8] = itemId;
				wear = true;
			}
			else
			{
				if(previewItems[slot] != 0)
				{
					player.sendPacket(Msg.THOSE_ITEMS_MAY_NOT_BE_TRIED_ON_SIMULTANEOUSLY);
					return;
				}
				previewItems[slot] = itemId;
				wear = true;
			}
		}

		if(!wear)
		{
			player.sendActionFailed();
			return;
		}

		long neededMoney = 0;

		for(int itemId : previewItems)
			if(itemId > 0)
			{
				L2Item item = ItemTable.getInstance().getTemplate(itemId);
				if(item == null)
				{
					System.out.println(getClass().getSimpleName() + ": no item template for itemId: " + itemId);
					return;
				}

				neededMoney += item.getCrystalType().externalOrdinal == 0 ? 10 : item.getCrystalType().externalOrdinal * 50;
			}

		if(player.reduceAdena("WearEquipment", neededMoney, npc, true))
		{
			player.sendPacket(new ShopPreviewInfo(previewItems));
			ThreadPoolManager.getInstance().scheduleGeneral(new RemoveWearItemsTask(player), 5000);
		}
	}

	static
	{
		_previewOrder.put(L2Item.SLOT_NECK, 3);
		_previewOrder.put(L2Item.SLOT_L_EAR, 2);
		_previewOrder.put(L2Item.SLOT_R_EAR, 1);
		_previewOrder.put(L2Item.SLOT_R_FINGER, 4);
		_previewOrder.put(L2Item.SLOT_L_FINGER, 5);
		_previewOrder.put(L2Item.SLOT_HAIR, 15);
		_previewOrder.put(L2Item.SLOT_DHAIR, 16);
		_previewOrder.put(L2Item.SLOT_HEAD, 6);
		_previewOrder.put(L2Item.SLOT_R_HAND, 7);
		_previewOrder.put(L2Item.SLOT_L_HAND, 8);
		_previewOrder.put(L2Item.SLOT_GLOVES, 9);
		_previewOrder.put(L2Item.SLOT_LEGS, 11);
		_previewOrder.put(L2Item.SLOT_CHEST, 10);
		_previewOrder.put(L2Item.SLOT_FULL_ARMOR, 15);
		_previewOrder.put(L2Item.SLOT_FORMAL_WEAR, 10);
		_previewOrder.put(L2Item.SLOT_BACK, 13);
		_previewOrder.put(L2Item.SLOT_FEET, 12);
		_previewOrder.put(L2Item.SLOT_UNDERWEAR, 0);
		_previewOrder.put(L2Item.SLOT_BELT, 25);
		_previewOrder.put(L2Item.SLOT_LR_HAND, 29);
		_previewOrder.put(L2Item.SLOT_L_BRACELET, 18);
		_previewOrder.put(L2Item.SLOT_R_BRACELET, 17);
		_previewOrder.put(L2Item.SLOT_DECO, 19);
	}
}
