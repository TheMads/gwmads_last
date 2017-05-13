package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.util.EffectsComparator;

import java.util.Arrays;

public class PartySpelled extends AbstractAbnormalStatus
{
	private int char_type;
	private int char_obj_id = 0;

	public PartySpelled(L2Character cha, boolean full)
	{
		if(cha == null)
			return;

		char_obj_id = cha.getObjectId();
		char_type = cha.isPet() ? 1 : cha.isSummon() ? 2 : 0;
		// 0 - L2Player // 1 - петы // 2 - саммоны
		if(full)
		{
			L2Effect[] effects = cha.getAllEffectsArray();
			Arrays.sort(effects, EffectsComparator.getInstance());
			for(L2Effect effect : effects)
				if(effect != null && effect.isInUse())
					L2Effect.addIcon(effect, this);
		}
	}

	@Override
	protected final void writeImpl()
	{
		if(char_obj_id == 0)
			return;

		writeC(0xF4);
		writeD(char_type);
		writeD(char_obj_id);
		writeD(_abnormals.size());
		for(AbnormalStatus status : _abnormals)
		{
			writeD(status.skillId);
			writeH(status.skillLvl);
			writeD(status.timeLeft);
		}
	}
}