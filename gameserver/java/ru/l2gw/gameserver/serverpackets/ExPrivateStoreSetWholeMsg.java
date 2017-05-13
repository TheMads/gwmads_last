package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * User: rage
 * Date: 12.11.2008
 * Time: 17:16:01
 */
public class ExPrivateStoreSetWholeMsg extends L2GameServerPacket
{
	private int char_obj_id;
	private String store_name;

	/**
	 * Название личного магазина продажи
	 * @param player
	 */
	public ExPrivateStoreSetWholeMsg(L2Player player)
	{
		char_obj_id = player.getObjectId();
		store_name = player.getTradeList() == null ? "" : player.getTradeList().getSellStoreName();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0x80);
		writeD(char_obj_id);
		writeS(store_name);
	}
}
