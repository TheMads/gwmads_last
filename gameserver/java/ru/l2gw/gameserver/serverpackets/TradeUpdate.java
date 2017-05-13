package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;

/**
 * Server packet, that move items "down" in trade
 */
public class TradeUpdate extends AbstractItemPacket
{
	private L2ItemInstance temp;
	private long _amount;

	public TradeUpdate(L2ItemInstance x, long amount)
	{
		temp = x;
		_amount = amount;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x81);
		writeH(1);
		boolean stackable = temp.isStackable();
		if(_amount == 0)
		{
			_amount = 1;
			stackable = false;
		}
		writeH(stackable ? 3 : 2);
		writeH(temp.getItem().getType1()); // item type1
		writeD(temp.getObjectId());
		writeD(temp.getItemId());
		writeQ(_amount);
		writeH(temp.getItem().getType2());
		writeH(temp.getCustomType1());
		writeD(temp.getBodyPart());
		writeH(temp.getEnchantLevel());
		writeH(0x00); // ?
		writeH(0x00);
		writeH(temp.getAttackElement()[0]);
		writeH(temp.getAttackElement()[1]);
		writeH(temp.getDefenceFire());
		writeH(temp.getDefenceWater());
		writeH(temp.getDefenceWind());
		writeH(temp.getDefenceEarth());
		writeH(temp.getDefenceHoly());
		writeH(temp.getDefenceDark());
		writeH(0);
		writeH(0);
		writeH(0);
		writeD(0x00);// Visible itemID
	}
}