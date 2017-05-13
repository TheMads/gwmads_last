package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.base.MultiSellEntry;
import ru.l2gw.gameserver.model.base.MultiSellIngredient;
import ru.l2gw.gameserver.model.base.MultiSellListContainer;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;

public class MultiSellList extends L2GameServerPacket
{
	protected int _page;
	protected int _finished;

	private int _listId;
	private GArray<MultiSellEntry> _possiblelist = new GArray<MultiSellEntry>();

	public MultiSellList(MultiSellListContainer list, int page, int finished)
	{
		_possiblelist = list.getEntries();
		_listId = list.getListId();
		_page = page;
		_finished = finished;
	}

	@Override
	protected final void writeImpl()
	{
		// ddddd (dchddddddddddhh (ddhdhdddddddddd)(dhdhdddddddddd))
		writeC(0xD0);
		writeD(_listId); // list id
		writeD(_page); // page
		writeD(_finished); // finished
		writeD(Config.MULTISELL_SIZE); // size of pages
		writeD(_possiblelist != null ? _possiblelist.size() : 0); //list lenght
		writeC(0x00); //Type (0x00 - Нормальный, 0xD0 - с шансом)

		if(_possiblelist == null)
			return;

		for(MultiSellEntry ent : _possiblelist)
		{
			writeD(ent.getEntryId());
			writeC(ent.getProduction().get(0).isStackable() ? 1 : 0); // stackable?
			writeH(0x00); // unknown
			writeD(0x00); // инкрустация
			writeD(0x00); // инкрустация
			writeH(-1); // attack element (-1 - none)
			writeH(0x00); // attack element value
			writeH(0x00); // водная стихия (fire pdef)
			writeH(0x00); // огненная стихия (water pdef)
			writeH(0x00); // земляная стихия (wind pdef)
			writeH(0x00); // воздушная стихия (earth pdef)
			writeH(0x00); // темная стихия (holy pdef)
			writeH(0x00); // светлая стихия (dark pdef)
			writeH(ent.getProduction().size());
			writeH(ent.getIngredients().size());

			for(MultiSellIngredient prod : ent.getProduction())
			{
				L2Item template = prod.getItemId() != L2Item.ITEM_ID_FAME_POINTS ? ItemTable.getInstance().getTemplate(prod.getItemId()) : null;
				writeD(prod.getItemId());
				writeD(template != null ? template.getBodyPart() : 0);
				writeH(template != null ? template.getType2() : 0xffff);
				writeQ(prod.getItemCount());
				writeH(prod.getItemEnchant());
				writeD(0x00); // getChance
				writeD(0x00); // инкрустация
				writeD(0x00); // Mana
				writeH(prod.getAttackElement()); // attack element (-2 - none)
				writeH(prod.getAttackValue()); // attack element value
				writeH(prod.getAttrFire()); // водная стихия (fire pdef)
				writeH(prod.getAttrWater()); // огненная стихия (water pdef)
				writeH(prod.getAttrWind()); // земляная стихия (wind pdef)
				writeH(prod.getAttrEarth()); // воздушная стихия (earth pdef)
				writeH(prod.getAttrHoly()); // темная стихия (holy pdef)
				writeH(prod.getAttrDark()); // светлая стихия (dark pdef)
			}

			for(MultiSellIngredient i : ent.getIngredients())
			{
				int itemId = i.getItemId();
				final L2Item item = itemId != L2Item.ITEM_ID_CLAN_REPUTATION_SCORE && itemId != L2Item.ITEM_ID_PC_BANG_POINTS && itemId != L2Item.ITEM_ID_FAME_POINTS ? ItemTable.getInstance().getTemplate(i.getItemId()) : null;
				writeD(itemId); //ID
				writeH(itemId != L2Item.ITEM_ID_CLAN_REPUTATION_SCORE && itemId != L2Item.ITEM_ID_PC_BANG_POINTS && itemId != L2Item.ITEM_ID_FAME_POINTS ? item.getType2() : 0xffff);
				writeQ(i.getItemCount()); //Count
				writeH((itemId != L2Item.ITEM_ID_CLAN_REPUTATION_SCORE && itemId != L2Item.ITEM_ID_PC_BANG_POINTS && itemId != L2Item.ITEM_ID_FAME_POINTS ? item.getType2() : 0x00) <= L2Item.TYPE2_ACCESSORY ? i.getItemEnchant() : 0); //Enchant Level
				writeD(0x00); // инкрустация
				writeD(0x00); // инкрустация
				writeH(i.getAttackElement()); // attack element (-2 - none)
				writeH(i.getAttackValue()); // attack element value
				writeH(i.getAttrFire()); // водная стихия (fire pdef)
				writeH(i.getAttrWater()); // огненная стихия (water pdef)
				writeH(i.getAttrWind()); // земляная стихия (wind pdef)
				writeH(i.getAttrEarth()); // воздушная стихия (earth pdef)
				writeH(i.getAttrHoly()); // темная стихия (holy pdef)
				writeH(i.getAttrDark()); // светлая стихия (dark pdef)
			}
		}
	}
}