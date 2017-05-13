package ru.l2gw.gameserver.pservercon.gspackets;

import ru.l2gw.gameserver.instancemanager.ProductManager;

/**
 * @author: rage
 * @date: 17.10.11 11:39
 */
public class DeleteProductItems extends GSBasePacket
{
	public DeleteProductItems(long transaction)
	{
		writeH(0x17);
		writeD(ProductManager.getNextJobId());
		writeD(0x01);
		writeQ(transaction);
	}
}
