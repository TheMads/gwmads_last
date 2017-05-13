package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;

/**
 * @author: rage
 * @date: 17.07.2010 12:52:11
 */
public abstract class AbstractAbnormalStatus extends L2GameServerPacket
{
	protected final GArray<AbnormalStatus> _abnormals = new GArray<AbnormalStatus>();

	public void addEffect(int skillId, int skillLvl, int timeLeft)
	{
		_abnormals.add(new AbnormalStatus(skillId, skillLvl, timeLeft));
	}

	protected class AbnormalStatus
	{
		public final int skillId;
		public final int skillLvl;
		public final int timeLeft;

		public AbnormalStatus(int _skillId, int _skillLvl, int _timeLeft)
		{
			skillId = _skillId;
			skillLvl = _skillLvl;
			timeLeft = _timeLeft;
		}
	}
}
