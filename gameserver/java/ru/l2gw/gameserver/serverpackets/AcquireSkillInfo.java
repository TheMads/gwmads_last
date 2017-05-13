package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2SkillLearn;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.tables.SkillTreeTable;
import ru.l2gw.commons.arrays.GArray;

public class AcquireSkillInfo extends L2GameServerPacket
{
	private GArray<Req> _reqs;
	private int _id;
	private byte _level;
	private int _spCost;
	private int _mode;
	private ClassId _classId;
	private L2Clan _clan;

	class Req
	{
		public int id;
		public int count;
		public int type;
		public int unk;

		Req(int type, int id, int count, int unk)
		{
			this.id = id;//0
			this.type = type;//2
			this.count = count;//count spb
			this.unk = unk;//2
		}
	}

	public AcquireSkillInfo(int id, byte level, ClassId classid, L2Clan clan)
	{
		_reqs = new GArray<Req>();
		_id = id;
		_level = level;
		_classId = classid;
		_clan = clan;
	}

	@Override
	final public void runImpl()
	{
		L2SkillLearn skillLearn = SkillTreeTable.getSkillLearn(_id, _level, _classId, _clan, getClient().getPlayer());
		if(skillLearn == null)
			return;

		_spCost = _clan != null ? skillLearn.getRepCost() : skillLearn.getSpCost();
		_mode = skillLearn.getSkillGroup();

		if(skillLearn.getItemId() > 0)
			_reqs.add(new Req(skillLearn.isCommon() ? 4 : skillLearn.isClan() ? 2 : skillLearn.isTransferSkill() ? 5 : 99, skillLearn.getItemId(), skillLearn.getItemCount(), skillLearn.isCommon() || skillLearn.isClan() ? 2 : 50));
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x91);
		writeD(_id);
		writeD(_level);
		writeD(_spCost);
		writeD(_mode);

		writeD(_reqs.size());

		for(Req temp : _reqs)
		{
			writeD(temp.type);
			writeD(temp.id);
			writeQ(temp.count);
			writeD(temp.unk);
		}
	}
}