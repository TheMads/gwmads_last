package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2ShortCut;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.skills.TimeStamp;

import java.util.Collection;

public class ShortCutInit extends L2GameServerPacket
{
	private Collection<L2ShortCut> _shortCuts;

	public ShortCutInit(L2Player pl)
	{
		_shortCuts = pl.getAllShortCuts();
		for(final L2ShortCut sc : _shortCuts)
			if(sc.type == L2ShortCut.TYPE_ITEM)
			{
				L2ItemInstance item = pl.getInventory().getItemByObjectId(sc.id);
				if(item != null && item.getItem().getDelayShareGroup() > 0)
				{
					TimeStamp timeStamp = pl.getSkillReuseTimeStamp(-item.getItem().getDelayShareGroup());
					int remainingTime = timeStamp != null ? (int) (timeStamp.getEndTime() - System.currentTimeMillis()) / 1000 : -1;
					sc.delay_group = item.getItem().getDelayShareGroup();
					sc.reuse_left = remainingTime;
					sc.reuse_delay = item.getItem().getReuseDelay() / 1000;
				}
			}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x45);
		writeD(_shortCuts.size());

		for(final L2ShortCut sc : _shortCuts)
		{
			writeD(sc.type);
			writeD(sc.slot + sc.page * 12);

			switch(sc.type)
			{
				case L2ShortCut.TYPE_ITEM:
					writeD(sc.id);
					writeD(0x01);
					writeD(sc.delay_group);
					writeD(sc.reuse_left);
					writeD(sc.reuse_delay);
					writeH(0x00); // Variation 1 ID
					writeH(0x00); // Variation 2 ID
					writeD(0x00); // Todo Harmony
					break;
				case L2ShortCut.TYPE_SKILL:
					writeD(sc.id);
					writeD(sc.level);
					writeD(sc.delay_group);
					writeC(0x00);
					writeD(0x01); // Character type
					break;
				case L2ShortCut.TYPE_ACTION:
				case L2ShortCut.TYPE_MACRO:
				case L2ShortCut.TYPE_RECIPE:
				case L2ShortCut.TYPE_MYTELEPORT:
					writeD(sc.id);
					writeD(0x01); // Character type
			}
		}
	}
}