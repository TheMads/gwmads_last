package ru.l2gw.gameserver.clientpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.SafeMath;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Multisell;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PcInventory;
import ru.l2gw.gameserver.model.base.L2Augmentation;
import ru.l2gw.gameserver.model.base.MultiSellEntry;
import ru.l2gw.gameserver.model.base.MultiSellIngredient;
import ru.l2gw.gameserver.model.base.MultiSellListContainer;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;
import ru.l2gw.gameserver.serverpackets.ExPCCafePointInfo;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.util.Util;

import java.nio.BufferUnderflowException;

public class RequestMultiSellChoose extends L2GameClientPacket
{
	// format: cdddhdddddddddd
	private int _listId;
	private int _entryId;
	private long _amount;
	private int _enchant = 0;
	private int _enchantIngredient = 0;
	private boolean _keepenchant = false;
	private boolean _notax = false;

	private MultiSellListContainer _list = null;
	private FastList<ItemData> _items = new FastList<ItemData>();

	private class ItemData
	{
		private final int _id;
		private final long _count;
		private final L2ItemInstance _item;

		public ItemData(int id, long count, L2ItemInstance item)
		{
			_id = id;
			_count = count;
			_item = item;
		}

		public int getId()
		{
			return _id;
		}

		public long getCount()
		{
			return _count;
		}

		public L2ItemInstance getItem()
		{
			return _item;
		}

		@Override
		public boolean equals(Object obj)
		{
			if(!(obj instanceof ItemData))
				return false;

			ItemData i = (ItemData) obj;

			return _id == i._id && _count == i._count && _item == i._item;
		}
	}

	@Override
	public void readImpl()
	{
		try
		{
			_listId = readD();
			_entryId = readD();
			_amount = readQ();
		}
		catch(BufferUnderflowException e)
		{
			_log.warn(getClient().getLoginName() + " maybe packet cheater!");
		}
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

		if(_amount < 1)
		{
			player.sendActionFailed();
			return;
		}

		_list = player.getLastMultisell();

		// На всякий случай...
		if(_list == null || _list.getListId() != _listId)
		{
			player.sendActionFailed();
			return;
		}

		if(!_list.community && (player.getLastMultisellNpc() == null || !player.isInRange(player.getLastMultisellNpc(), player.getInteractDistance(player.getLastMultisellNpc()))))
		{
			player.sendActionFailed();
			return;
		}

		_keepenchant = _list.getKeepEnchant();
		_notax = _list.getNoTax();

		for(MultiSellEntry entry : _list.getEntries())
			if(entry.getEntryId() == _entryId)
			{
				doExchange(player, entry);
				break;
			}
	}

	private void doExchange(L2Player player, MultiSellEntry entry)
	{
		PcInventory inv = player.getInventory();

		long totalAdenaCost = 0;
		long tax = 0;
		double taxRate = 0.;
		L2NpcInstance merchant = player.getLastMultisellNpc();

		if(_notax)
			taxRate = 0.;
		else if(!_list.community && !Config.SERVICES_OFFSHORE_NO_CASTLE_TAX && !merchant.isInZone(ZoneType.offshore))
			taxRate = merchant.getCastle().getTaxRate();

		GArray<MultiSellIngredient> productId = entry.getProduction();
		GArray<MultiSellIngredient> ingredientsId = entry.getIngredients();

		if(_keepenchant)
		{
			for(MultiSellIngredient p : productId)
				_enchant = Math.max(_enchant, p.getItemEnchant());
			for(MultiSellIngredient i : entry.getIngredients())
				_enchantIngredient = Math.max(_enchantIngredient, i.getItemEnchant());
		}
		synchronized(inv)
		{
			int slots = inv.slotsLeft();
			if(slots == 0)
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_WEIGHT_AND_VOLUME_LIMIT_OF_INVENTORY_MUST_NOT_BE_EXCEEDED));
				return;
			}
			if(productId.size() == 0)
			{
				System.out.println("WARNING Product list = 0 multisell id=:" + _listId + " player:" + player.getName());
				return;
			}
			if(ingredientsId.size() == 0)
			{
				System.out.println("WARNING Ingredients list = 0 multisell id=:" + _listId + " player:" + player.getName());
				return;
			}
			//Проверяем на валидность новую загрузку если обмен произойдет успешно
			long totalLoad = 0;
			long totalSlots = 0;
			for(MultiSellIngredient i : productId)
			{
				if(i.getItemId() == L2Item.ITEM_ID_FAME_POINTS || i.getItemId() == L2Item.ITEM_ID_CLAN_REPUTATION_SCORE || i.getItemId() == L2Item.ITEM_ID_PC_BANG_POINTS || ItemTable.getInstance().getTemplate(i.getItemId()) == null)
					continue;
				L2Item item = ItemTable.getInstance().getTemplate(i.getItemId());
				totalLoad += item.getWeight() * _amount * i.getItemCount();
				if(item.isStackable())
				{
					if(inv.getItemByItemId(i.getItemId()) == null)
						totalSlots++;
				}
				else
					totalSlots += i.getItemCount() * _amount;
			}
			for(MultiSellIngredient i : ingredientsId)
			{
				if(i.getItemId() == L2Item.ITEM_ID_FAME_POINTS || i.getItemId() == L2Item.ITEM_ID_CLAN_REPUTATION_SCORE || i.getItemId() == L2Item.ITEM_ID_PC_BANG_POINTS || ItemTable.getInstance().getTemplate(i.getItemId()) == null)
					continue;
				totalLoad -= ItemTable.getInstance().getTemplate(i.getItemId()).getWeight() * _amount * i.getItemCount();
				player.sendUserInfo(true);
			}
			if(totalLoad > 0 && !inv.validateWeight(totalLoad) || inv.slotsLeft() < totalSlots)
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_WEIGHT_AND_VOLUME_LIMIT_OF_INVENTORY_MUST_NOT_BE_EXCEEDED));
				return;
			}

			L2Augmentation augmentation = null;
			int[] attribute = null;

			// Перебор всех ингридиентов, проверка наличия и создание списка забираемого
			for(MultiSellIngredient ing : ingredientsId)
			{
				int ingridientItemId = ing.getItemId();
				long ingridientItemCount = ing.getItemCount();
				//Так как ITEM_ID_CLAN_REPUTATION_SCORE и ITEM_ID_PC_BANG_POINTS в ItemTable нет, то проверяем
				if(ingridientItemId != L2Item.ITEM_ID_FAME_POINTS && ingridientItemId != L2Item.ITEM_ID_CLAN_REPUTATION_SCORE && ingridientItemId != L2Item.ITEM_ID_PC_BANG_POINTS)
				{
					if(ItemTable.getInstance().getTemplate(ingridientItemId) == null)
						continue;
				}
				long total;

				try
				{
					total = SafeMath.safeMulLong(ingridientItemCount, _amount);
				}
				catch(ArithmeticException e)
				{
					Util.handleIllegalPlayerAction(player, "RequestMultiSellChoose[308]", "tried an overflow exploit: buy " + _amount + " of " + productId + ", ingridient " + ingridientItemId + " count " + ingridientItemCount, 1);
					player.sendActionFailed();
					return;
				}

				//Обработка не стекируемых вещей
				if(ingridientItemId != L2Item.ITEM_ID_FAME_POINTS && ingridientItemId != L2Item.ITEM_ID_CLAN_REPUTATION_SCORE && ingridientItemId != L2Item.ITEM_ID_PC_BANG_POINTS && !ItemTable.getInstance().getTemplate(ingridientItemId).isStackable())
				{
					for(int i = 0; i < ingridientItemCount * _amount; i++)
					{
						GArray<L2ItemInstance> list = inv.getAllItemsById(ingridientItemId);
						// Если энчант имеет значение - то ищем вещи с точно таким энчантом
						if(_keepenchant)
						{
							L2ItemInstance itemToTake = null;
							for(L2ItemInstance itm : list)
								if((itm.getEnchantLevel() == (_list.isCheckEnchantIngredient() ? _enchantIngredient : _enchant) || itm.getItem().getType2() > 2) && !_items.contains(new ItemData(itm.getItemId(), itm.getCount(), itm)) && !itm.isShadowItem() && !itm.isStandartItem() && (itm.getCustomFlags() & L2ItemInstance.FLAG_NO_TRADE) != L2ItemInstance.FLAG_NO_TRADE && !itm.isEquipped())
								{
									itemToTake = itm;
									break;
								}

							if(itemToTake == null)
							{
								player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
								return;
							}

							if(itemToTake.getAugmentation() != null)
							{
								itemToTake.setWhFlag(true);
								augmentation = itemToTake.getAugmentation();
							}

							if(itemToTake.isEquipable())
								attribute = new int[]{itemToTake.getAttributeElementValue(L2Item.ATTRIBUTE_FIRE),
										itemToTake.getAttributeElementValue(L2Item.ATTRIBUTE_WATER),
										itemToTake.getAttributeElementValue(L2Item.ATTRIBUTE_WIND),
										itemToTake.getAttributeElementValue(L2Item.ATTRIBUTE_EARTH),
										itemToTake.getAttributeElementValue(L2Item.ATTRIBUTE_HOLY),
										itemToTake.getAttributeElementValue(L2Item.ATTRIBUTE_DARK)
								};

							_items.add(new ItemData(itemToTake.getItemId(), 1, itemToTake));
						}
						// Если энчант не обрабатывается берется вещь с наименьшим энчантом
						else
						{
							L2ItemInstance itemToTake = null;
							for(L2ItemInstance itm : list)
								if(!_items.contains(new ItemData(itm.getItemId(), itm.getCount(), itm)) && (itemToTake == null || itm.getEnchantLevel() < itemToTake.getEnchantLevel()) && !itm.isShadowItem() && (itm.getCustomFlags() & L2ItemInstance.FLAG_NO_TRADE) != L2ItemInstance.FLAG_NO_TRADE && !itm.isEquipped())
								{
									itemToTake = itm;
									if(itemToTake.getEnchantLevel() == 0)
										break;
								}

							if(itemToTake == null)
							{
								player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
								return;
							}
							if(itemToTake.getAugmentation() != null)
							{
								itemToTake.setWhFlag(true);
								augmentation = itemToTake.getAugmentation();
							}
							_items.add(new ItemData(itemToTake.getItemId(), 1, itemToTake));
						}
					}
				}
				//Вещи с клановой репутацией
				else if(ingridientItemId == L2Item.ITEM_ID_CLAN_REPUTATION_SCORE)
				{
					if(player.getClanId() == 0)
					{
						player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_A_CLAN_MEMBER));
						return;
					}

					if(player.getClan().getReputationScore() < total)
					{
						player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW));
						return;
					}

					if(!player.isClanLeader())
					{
						player.sendPacket(new SystemMessage(SystemMessage.S1_IS_NOT_A_CLAN_LEADER).addString(player.getName()));
						return;
					}
					_items.add(new ItemData(ingridientItemId, total, null));
				}
				//заменитель клан поинтов или тупо доп игровая валюта(поумолчанию UnUsed включаеться в сервисах)
				else if(ingridientItemId == L2Item.ITEM_ID_PC_BANG_POINTS)
				{
					if(player.getPcBangPoints() < total)
					{
						player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_SHORT_OF_ACCUMULATED_POINTS));
						return;
					}
					_items.add(new ItemData(ingridientItemId, (int) total, null));
				}
				// Fame
				else if(ingridientItemId == L2Item.ITEM_ID_FAME_POINTS)
				{
					if(player.getFame() < total)
					{
						player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
						return;
					}
					_items.add(new ItemData(ingridientItemId, total, null));
				}
				// В последнюю очередь стекируемые вещи.
				else
				{
					if(ingridientItemId == 57)
					{
						try
						{
							long adena = SafeMath.safeMulLong(ingridientItemCount, _amount);
							totalAdenaCost = SafeMath.safeAddLong(adena, totalAdenaCost);
						}
						catch(ArithmeticException e)
						{
							Util.handleIllegalPlayerAction(player, "RequestMultiSellChoose", "tried an overflow exploit: buy " + _amount + " of " + productId + ", ingridient " + ingridientItemId + " count " + ingridientItemCount, 1);
							player.sendActionFailed();
							return;
						}
					}

					L2ItemInstance item = inv.getItemByItemId(ingridientItemId);

					if(item == null || item.getCount() < total)
					{
						player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
						return;
					}

					tax += item.getItem().getReferencePrice() * taxRate * total;
					_items.add(new ItemData(item.getItemId(), total, item));
				}

				if(player.getAdena() < totalAdenaCost + tax)
				{
					player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
					return;
				}
			}

			for(MultiSellIngredient pr : productId)
			{
				long total;
				try
				{
					total = SafeMath.safeMulLong(pr.getItemCount(), _amount);
					total = SafeMath.safeAddLong(total, player.getItemCountByItemId(pr.getItemId()));
				}
				catch(ArithmeticException e)
				{
					player.sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_TRANSFERRED_AT_ONE_TIME);
					player.sendActionFailed();
					return;
				}
				if(total <= 0 || total > L2Item.MAX_COUNT)
				{
					player.sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_TRANSFERRED_AT_ONE_TIME);
					player.sendActionFailed();
					return;
				}
			}

			//Забираем вещи указанные в ингридиентах
			for(ItemData id : _items)
			{
				long count = id.getCount();
				if(count > 0)
				{
					L2ItemInstance item = id.getItem();

					if(item != null)
					{
						if(item.isEquipped())
							inv.unEquipItemAndSendChanges(item);
						player.destroyItem("MultiSell", item.getObjectId(), count, player.getLastMultisellNpc(), true);
					}
					else if(id.getId() == L2Item.ITEM_ID_CLAN_REPUTATION_SCORE)
					{
						player.getClan().incReputation((int) -count, false, "MultiSell");
						player.sendPacket(new SystemMessage(SystemMessage.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_REPUTATION_SCORE).addNumber(count));
					}
					else if(id.getId() == L2Item.ITEM_ID_PC_BANG_POINTS)
					{
						player.setPcBangPoints(player.getPcBangPoints() - (int) count);
						player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_USING_S1_POINT).addNumber(count));
						player.sendPacket(new ExPCCafePointInfo(player));
					}
					else if(id.getId() == L2Item.ITEM_ID_FAME_POINTS)
					{
						player.addFame(-(int) count);
						//TODO нормальный SM
						player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_USING_S1_POINT).addNumber(count));
					}
				}
			}

			if(taxRate > 0 && tax > 0)
			{
				player.reduceAdena("MultiSell", tax, player.getLastMultisellNpc(), false);
				if(player.isGM())
					player.sendMessage("Tax: " + tax);
				if(!_list.community)
				{
					merchant.getCastle().addToTreasury(tax, true, false, "MULTISELL");
				}
			}

			for(MultiSellIngredient pr : productId)
			{
				// Fame points
				if(pr.getItemId() == L2Item.ITEM_ID_FAME_POINTS)
				{
					long total = pr.getItemCount() * _amount;

					if(total < 0 || total > L2Item.MAX_COUNT)
					{
						Util.handleIllegalPlayerAction(player, "RequestMultiSellChoose[476]", "tried an overflow exploit: buy " + _amount * pr.getItemCount() + " of " + pr.getItemId() + ", base amount " + _amount, 1);
						return;
					}
					player.addFame((int) total);
				}
				//Стекируемые вещи
				else if(ItemTable.getInstance().getTemplate(pr.getItemId()).isStackable())
				{
					long total = pr.getItemCount() * _amount;

					if(total < 0 || total > L2Item.MAX_COUNT)
					{
						Util.handleIllegalPlayerAction(player, "RequestMultiSellChoose[476]", "tried an overflow exploit: buy " + _amount * pr.getItemCount() + " of " + pr.getItemId() + ", base amount " + _amount, 1);
						return;
					}

					inv.addItem("MultiSell[" + _listId + "|" + _entryId + "]", pr.getItemId(), total, player, player.getLastMultisellNpc());
					player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1_S).addItemName(pr.getItemId()).addNumber(total));
				}
				//не стекируемые вещи
				else
				{
					for(int i = 0; i < _amount; i++)
					{
						for(int s = 0; s < pr.getItemCount(); s++)
						{
							L2ItemInstance product = inv.addItem("MultiSell[" + _listId + "|" + _entryId + "]", pr.getItemId(), 1, player, player.getLastMultisellNpc());
							if(_keepenchant)
								product.setEnchantLevel(_enchant);
							if(augmentation != null && product.isEquipable())
							{
								augmentation.setItem(product);
								product.setAugmentation(augmentation);
							}

							if(attribute != null && product.isEquipable() && attribute.length == 6)
								product.setAttributeElement(attribute[0], attribute[1], attribute[2], attribute[3], attribute[4], attribute[5], true);
						}
						if(pr.getItemCount() > 1)
							player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1_S).addItemName(pr.getItemId()).addNumber(pr.getItemCount()));
						else
							player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED__S1).addItemName(pr.getItemId()));
					}
				}
			}
		}

		player.updateStats();

		if(_list == null || !_list.getShowAll()) // Если показывается только то, на что хватает материалов обновить окно у игрока
			L2Multisell.getInstance().SeparateAndSend(_listId, player, player.getLastNpc() != null ? player.getLastNpc().getCastle().getTaxRate() : 0);
	}
}
