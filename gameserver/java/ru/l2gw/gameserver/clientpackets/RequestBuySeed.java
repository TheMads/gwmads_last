package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.SafeMath;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.CastleManorManager;
import ru.l2gw.gameserver.instancemanager.CastleManorManager.SeedProduction;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.instances.L2ManorManagerInstance;
import ru.l2gw.gameserver.serverpackets.StatusUpdate;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.util.Util;

/**
 * Format: cdd[dd]
 * c    // id (0xC5)
 *
 * d    // manor id
 * d    // seeds to buy
 * [
 * d    // seed id
 * d    // count
 * ]
 * @author l3x
 */
public class RequestBuySeed extends L2GameClientPacket
{
	private int _count;

	private int _manorId;

	private long[] _items; // size _count * 2

	@Override
	protected void readImpl()
	{
		_manorId = readD();
		_count = readD();

		if(_count > Short.MAX_VALUE || _count <= 0 || _count * 12 < _buf.remaining())
		{
			_count = 0;
			return;
		}

		_items = new long[_count * 2];

		for(int i = 0; i < _count; i++)
		{
			_items[i * 2 + 0] = readD();
			long cnt = readQ();
			if(cnt > L2Item.MAX_COUNT || cnt < 1)
			{
				_count = 0;
				_items = null;
				return;
			}
			_items[i * 2 + 1] = cnt;
		}
	}

	@Override
	protected void runImpl()
	{
		long totalPrice = 0;
		int slots = 0;
		int totalWeight = 0;

		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(_count < 1)
		{
			player.sendActionFailed();
			return;
		}

		L2Object target = player.getTarget();

		if(!(target instanceof L2ManorManagerInstance))
			target = player.getLastNpc();

		if(!(target instanceof L2ManorManagerInstance))
			return;

		Castle castle = ResidenceManager.getInstance().getCastleById(_manorId);
		if(!castle.isCastle)
			return;
		for(int i = 0; i < _count; i++)
		{
			int seedId = (int)_items[i * 2 + 0];
			long count = _items[i * 2 + 1];
			long price = 0;
			long residual = 0;

			SeedProduction seed = castle.getSeed(seedId, CastleManorManager.PERIOD_CURRENT);
			price = seed.getPrice();
			residual = seed.getCanProduce();

			if(price <= 0)
				return;

			if(residual < count)
				return;

			try
			{
				totalPrice = SafeMath.safeAddLong(totalPrice, SafeMath.safeMulLong(count, price));
			}
			catch(ArithmeticException e)
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + Long.MAX_VALUE + " adena worth of goods.\r\n" + e.getMessage(), "", Config.DEFAULT_PUNISH);
				return;
			}

			L2Item template = ItemTable.getInstance().getTemplate(seedId);
			totalWeight += count * template.getWeight();
			if(!template.isStackable())
				slots += count;
			else if(player.getInventory().getItemByItemId(seedId) == null)
				slots++;
		}

		if(!player.getInventory().validateWeight(totalWeight))
		{
			sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			return;
		}

		if(!player.getInventory().validateCapacity(slots))
		{
			sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
			return;
		}

		// Charge buyer
		if(totalPrice < 0 || !player.reduceAdena("BuySeed", totalPrice, null, false))
			return;

		// Adding to treasury for Manor Castle
		castle.addToTreasuryNoTax((int) totalPrice, false, true, "BUY_SEED");

		// Proceed the purchase
		for(int i = 0; i < _count; i++)
		{
			int seedId = (int)_items[i * 2 + 0];
			long count = _items[i * 2 + 1];
			if(count < 0)
				count = 0;

			// Update Castle Seeds Amount
			SeedProduction seed = castle.getSeed(seedId, CastleManorManager.PERIOD_CURRENT);
			seed.setCanProduce(seed.getCanProduce() - count);
			// TODO update SQL for long
			if(Config.MANOR_SAVE_ALL_ACTIONS)
				ResidenceManager.getInstance().getCastleById(_manorId).updateSeed(seed.getId(), seed.getCanProduce(), CastleManorManager.PERIOD_CURRENT);

			// Add item to Inventory and adjust update packet
			player.addItem("BuySeed", seedId, count, player.getLastNpc(), true);
		}

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
}
