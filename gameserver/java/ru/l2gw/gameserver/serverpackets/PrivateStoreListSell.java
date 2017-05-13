package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.TradeItem;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;

import java.util.concurrent.ConcurrentLinkedQueue;

public class PrivateStoreListSell extends L2GameServerPacket
{
	private int seller_id;
	private long buyer_adena;
	private final boolean _package;
	private ConcurrentLinkedQueue<TradeItem> _sellList;

	/**
	 * Список вещей в личном магазине продажи, показываемый покупателю
	 * @param buyer
	 * @param seller
	 */
	public PrivateStoreListSell(L2Player buyer, L2Player seller)
	{
		seller_id = seller.getObjectId();
		buyer_adena = buyer.getAdena();
		_package = seller.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL_PACKAGE;
		_sellList = seller.getSellList();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xA1);
		writeD(seller_id);
		writeD(_package ? 1 : 0);
		writeQ(buyer_adena);

		writeD(_sellList.size());
		for(TradeItem ti : _sellList)
		{
			L2Item tempItem = ItemTable.getInstance().getTemplate(ti.getItemId());
			writeD(ti.getObjectId());
			writeD(ti.getItemId());
			writeD(0x00);
			writeQ(ti.getCount());
			writeH(tempItem.getType2());
			writeH(ti.getCustomType1());
			writeH(0x00);
			writeD(tempItem.getBodyPart());
			writeH(ti.getEnchantLevel());
			writeH(ti.getCustomType2());
			writeD(0x00); // Augmenation
			writeD(-1);		// Mana
			writeD(-9999);	// Time
			writeH(ti.getAttackElement()); // attack element (-2 - none)
			writeH(ti.getAttackValue()); // attack element value
			writeH(ti.getDefenceFire()); // водная стихия (fire pdef)
			writeH(ti.getDefenceWater()); // огненная стихия (water pdef)
			writeH(ti.getDefenceWind()); // земляная стихия (wind pdef)
			writeH(ti.getDefenceEarth()); // воздушная стихия (earth pdef)
			writeH(ti.getDefenceHoly()); // темная стихия (holy pdef)
			writeH(ti.getDefenceDark()); // светлая стихия (dark pdef)
			writeH(ti.getEnchantOptionId(0));
			writeH(ti.getEnchantOptionId(1));
			writeH(ti.getEnchantOptionId(2));

			writeQ(ti.getOwnersPrice());//your price
			writeQ(ti.getStorePrice()); //store price
		}
	}
}