package ru.l2gw.gameserver.serverpackets;

/**
 * @author: rage
 * @date: 15.10.11 21:58
 */
public class ExBR_BuyProduct extends  L2GameServerPacket
{
	public static final int RESULT_OK = 1; // ok
	public static final int RESULT_NOT_ENOUGH_POINTS = -1;
	public static final int RESULT_WRONG_PRODUCT = -2; // also -5
	public static final int RESULT_INVENTORY_FULL = -4;
	public static final int RESULT_SALE_PERIOD_ENDED = -7; // also -8
	public static final int RESULT_WRONG_USER_STATE = -9; // also -11
	public static final int RESULT_WRONG_PRODUCT_ITEM = -10;
	public static final int RESULT_WRONG_DAY = -12;
	public static final int RESULT_WRONG_HOUR = -13;
	public static final int RESULT_OUT_OF_STOCK = -14;

	private final int result;

	public ExBR_BuyProduct(int result)
	{
		this.result = result;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0xD9);
		writeD(result);
	}
}

