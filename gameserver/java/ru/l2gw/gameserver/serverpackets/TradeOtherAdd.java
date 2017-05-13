package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;

public class TradeOtherAdd extends AbstractItemPacket
{
	private L2ItemInstance temp;
	private long _amount;

	public TradeOtherAdd(L2ItemInstance x, long amount)
	{
		temp = x;
		_amount = amount;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x1b);
		writeH(1); // item count
		writeH(temp.getItem().getType1());
		writeD(temp.getObjectId());
		writeD(temp.getItemId());
		writeQ(_amount);
		writeH(temp.getItem().getType2());
		writeH(temp.getCustomType1());
		writeD(temp.getItem().getBodyPart());
		writeH(temp.getEnchantLevel());
		writeH(0x00);
		writeH(temp.getCustomType2());
		writeH(temp.getAttackElement()[0]);
		writeH(temp.getAttackElement()[1]);
		writeH(temp.getDefenceFire());
		writeH(temp.getDefenceWater());
		writeH(temp.getDefenceWind());
		writeH(temp.getDefenceEarth());
		writeH(temp.getDefenceHoly());
		writeH(temp.getDefenceDark());
		writeH(temp.getEnchantOptionId(0));
		writeH(temp.getEnchantOptionId(1));
		writeH(temp.getEnchantOptionId(2));
		writeD(0x00);// Visible itemID
	}
}