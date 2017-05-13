package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Character;

import java.util.ArrayList;
import java.util.List;

public class MagicSkillLaunched extends L2GameServerPacket
{
	private final int _casterId;
	private final int _skillId;
	private final int _skillLevel;
	private final List<L2Character> _targets;
	private final boolean _isBuff;

	public MagicSkillLaunched(int casterId, int skillId, int skillLevel, L2Character target, boolean isBuff)
	{
		_casterId = casterId;
		_skillId = skillId;
		_skillLevel = skillLevel;
		_targets = new ArrayList<>(1);
		_targets.add(target);
		_isBuff = isBuff;
	}

	public MagicSkillLaunched(int casterId, int skillId, int skillLevel, List<L2Character> targets, boolean isBuff)
	{
		_casterId = casterId;
		_skillId = skillId;
		_skillLevel = skillLevel;
		_targets = targets;
		_isBuff = isBuff;
	}

	public boolean isBuffPacket()
	{
		return _isBuff;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x54);
		writeD(0x00); // L2WT GOD
		writeD(_casterId);
		writeD(_skillId);
		writeD(_skillLevel);
		writeD(_targets.size());
		for(L2Character target : _targets)
			if(target != null)
				writeD(target.getObjectId());
	}
}