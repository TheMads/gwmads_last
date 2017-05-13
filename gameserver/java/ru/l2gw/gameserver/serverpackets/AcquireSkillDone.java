package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 08.06.2010 15:48:12
 */
public class AcquireSkillDone extends L2GameServerPacket
{
	public static final AcquireSkillDone ACQUIRE_DONE = new AcquireSkillDone(); 
	@Override
	protected void writeImpl()
	{
		writeC(0x94);
	}
}
