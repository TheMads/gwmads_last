package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.TradeItem;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;

import java.util.concurrent.ConcurrentLinkedQueue;

public class PrivateStoreListBuy extends L2GameServerPacket
{
	private int buyer_id;
	private long seller_adena;
	private ConcurrentLinkedQueue<TradeItem> _buyerslist;

	/**
	 * Список вещей в личном магазине покупки, показываемый продающему
	 * @param seller
	 * @param storePlayer
	 */
	@SuppressWarnings("unchecked")
	public PrivateStoreListBuy(L2Player seller, L2Player storePlayer)
	{
		seller_adena = seller.getAdena();
		buyer_id = storePlayer.getObjectId();
		_buyerslist = storePlayer.getTradeList().getAvailableItemsForSell(storePlayer.getBuyList(), seller.getInventory().getItemsList(), seller);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xBE);

		writeD(buyer_id);
		writeQ(seller_adena);
		writeD(_buyerslist.size());
		for(TradeItem buyersitem : _buyerslist)
		{
			L2Item tmp = ItemTable.getInstance().getTemplate(buyersitem.getItemId());

			writeD(buyersitem.getObjectId());
			writeD(buyersitem.getItemId());
			writeD(0x00);
			writeQ(buyersitem.getTempValue());
			writeH(tmp.getType2());
			writeH(buyersitem.getCustomType1());
			writeH(0x00);
			writeD(tmp.getBodyPart());
			writeH(buyersitem.getEnchantLevel());
			writeH(buyersitem.getCustomType2());
			writeD(0x00); // Augmenation
			writeD(-1);		// Mana
			writeD(-9999);	// Time

			writeH(buyersitem.getAttackElement()); // attack element (-2 - none)
			writeH(buyersitem.getAttackValue()); // attack element value
			writeH(buyersitem.getDefenceFire()); // водная стихия (fire pdef)
			writeH(buyersitem.getDefenceWater()); // огненная стихия (water pdef)
			writeH(buyersitem.getDefenceWind()); // земляная стихия (wind pdef)
			writeH(buyersitem.getDefenceEarth()); // воздушная стихия (earth pdef)
			writeH(buyersitem.getDefenceHoly()); // темная стихия (holy pdef)
			writeH(buyersitem.getDefenceDark()); // светлая стихия (dark pdef)
			writeH(buyersitem.getEnchantOptionId(0));
			writeH(buyersitem.getEnchantOptionId(1));
			writeH(buyersitem.getEnchantOptionId(2));

			writeD(buyersitem.getObjectId());
			writeQ(buyersitem.getOwnersPrice());
			writeQ(tmp.getReferencePrice() * 2);
			writeQ(buyersitem.getCount());
		}
	}
}