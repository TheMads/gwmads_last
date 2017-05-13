package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.instancemanager.CastleManorManager.SeedProduction;
import ru.l2gw.gameserver.model.L2Manor;
import ru.l2gw.commons.arrays.GArray;

/**
 * format(packet 0xFE)
 * ch ddd [dddddcdcd]
 * c  - id
 * h  - sub id
 *
 * d  - manor id
 * d
 * d  - size
 *
 * [
 * d  - seed id
 * d  - left to buy
 * d  - started amount
 * d  - sell price
 * d  - seed level
 * c
 * d  - reward 1 id
 * c
 * d  - reward 2 id
 * ]
 *
 */
public class ExShowSeedInfo extends L2GameServerPacket
{
	private GArray<SeedProduction> _seeds;
	private int _manorId;

	public ExShowSeedInfo(int manorId, GArray<SeedProduction> seeds)
	{
		_manorId = manorId;
		_seeds = seeds;
		if(_seeds == null)
			_seeds = new GArray<SeedProduction>();
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET); // Id
		writeH(0x23); // SubId
		writeC(0);
		writeD(_manorId); // Manor ID
		writeD(0);
		writeD(_seeds.size());
		for(SeedProduction seed : _seeds)
		{
			writeD(seed.getId()); // Seed id
			writeQ(seed.getCanProduce()); // Left to buy
			writeQ(seed.getStartProduce()); // Started amount
			writeQ(seed.getPrice()); // Sell Price
			writeD(L2Manor.getInstance().getSeedLevel(seed.getId())); // Seed Level
			writeC(1); // reward 1 Type
			writeD(L2Manor.getInstance().getRewardItemBySeed(seed.getId(), 1)); // Reward 1 Type Item Id
			writeC(1); // reward 2 Type
			writeD(L2Manor.getInstance().getRewardItemBySeed(seed.getId(), 2)); // Reward 2 Type Item Id
		}
	}
}
