package ru.l2gw.gameserver.pservercon.gspackets;

/**
 * @author: rage
 * @date: 17.10.11 10:55
 */
public class RequestBuyProductItem extends GSBasePacket
{
	public RequestBuyProductItem(int jobId, int accountId, int objectId, int productId, int amount, long price, String charName)
	{
		writeH(0x0C);
		writeD(jobId);
		writeD(accountId);
		writeD(objectId);
		writeD(productId);
		writeD(amount);
		writeD(0x00);
		writeD(0x00);
		writeQ(price);
		writeS(charName);
	}
}
