package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2ShortCut;

/**
 * sample
 *
 * 56
 * 01000000 04000000 dd9fb640 01000000
 *
 * 56
 * 02000000 07000000 38000000 03000000 01000000
 *
 * 56
 * 03000000 00000000 02000000 01000000
 *
 * format   dd d/dd/d d
 */
public class ShortCutRegister extends L2GameServerPacket
{
	private L2ShortCut sc;

	/**
	 * Register new skill shortcut
	 */
	public ShortCutRegister(L2ShortCut _sc)
	{
		sc = _sc;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x44);

		writeD(sc.type);
		writeD(sc.slot + sc.page * 12); // C4 Client
		if(sc.type == L2ShortCut.TYPE_SKILL) // Skill
		{
			writeD(sc.id);
			writeD(sc.level);
			writeC(0x00); // C5
		}
		else if(sc.type == L2ShortCut.TYPE_ITEM)
		{
			writeD(sc.id);
			writeD(0x01);
			writeD(sc.delay_group);
			writeD(sc.reuse_left);
			writeD(sc.reuse_delay);
			writeH(0x00);
			writeH(0x00);
		}
		else
			writeD(sc.id);

		writeD(1);//??
	}
}