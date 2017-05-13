package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

public class PartySmallWindowAdd extends L2GameServerPacket
{
	//dddSdddddddddd
	private int leader_id = 0;
	private int member_obj_id, member_level, member_class_id;
	private int member_curHp, member_maxHp, member_curCp, member_maxCp, member_curMp, member_maxMp, member_race, distribution;
	private String member_name;

	public PartySmallWindowAdd(L2Player member)
	{
		member_obj_id = member.getObjectId();
		member_name = member.getName();
		member_curCp = (int) member.getCurrentCp();
		member_maxCp = member.getMaxCp();
		member_curHp = (int) member.getCurrentHp();
		member_maxHp = member.getMaxHp();
		member_curMp = (int) member.getCurrentMp();
		member_maxMp = member.getMaxMp();
		member_level = member.getLevel();
		member_class_id = member.getClassId().getId();
		member_race = member.getRace().ordinal();
		distribution = member.getParty().getLootDistribution();
		leader_id = member.getParty().getPartyLeaderOID();

	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x4F);
		writeD(leader_id); // c3
		writeD(distribution);//writeD(0x04); ?? //c3
		writeD(member_obj_id);
		writeS(member_name);
		writeD(member_curCp);
		writeD(member_maxCp);
		writeD(member_curHp);
		writeD(member_maxHp);
		writeD(member_curMp);
		writeD(member_maxMp);
		writeD(0);
		writeD(member_level);
		writeD(member_class_id);
		writeD(0);
		writeD(member_race);
		writeD(0);
		writeD(0);
		writeD(0);
	}
}