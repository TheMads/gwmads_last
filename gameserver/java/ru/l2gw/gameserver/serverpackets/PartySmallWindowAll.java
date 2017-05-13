package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.commons.arrays.GArray;

import java.util.List;

/**
 *
 * sample
 * 4E
 * 01 00 00 00  count
 *
 * c1 b2 e0 4a  object id
 * 54 00 75 00 65 00 73 00 64 00 61 00 79 00 00 00  name
 * 5a 01 00 00  hp
 * 5a 01 00 00  hp max
 * 89 00 00 00  mp
 * 89 00 00 00  mp max
 * 0e 00 00 00  level
 * 12 00 00 00  class
 * 00 00 00 00
 * 01 00 00 00
 *
 * format   ddd (dSddddddddddd)
 */
public class PartySmallWindowAll extends L2GameServerPacket
{
	private int leader_id, loot;
	private GArray<PartySmallWindowMemberInfo> members = new GArray<PartySmallWindowMemberInfo>(9);
	private List<L2Player> _partyMembers;

	public PartySmallWindowAll(List<L2Player> party)
	{
		leader_id = party.get(0).getParty().getPartyLeaderOID();
		loot = party.get(0).getParty().getLootDistribution();
		_partyMembers = party;
	}

	@Override
	final public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		for(L2Player member : _partyMembers)
		{
			if(member.equals(player))
				continue;
			members.add(new PartySmallWindowMemberInfo(member));
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x4E);
		writeD(leader_id); // c3 party leader id
		writeD(loot); //c3 party loot type (0,1,2,....)
		writeD(members.size());
		for(PartySmallWindowMemberInfo member : members)
		{
			writeD(member._id);
			writeS(member._name);
			writeD(member.curCp);
			writeD(member.maxCp);
			writeD(member.curHp);
			writeD(member.maxHp);
			writeD(member.curMp);
			writeD(member.maxMp);
			writeD(member.level);
			writeD(member.class_id);
			writeD(0);//writeD(0x01); ??
			writeD(member.race_id);
			writeD(0);
			writeD(0);

			if(member.pet_id != 0)
			{
				writeD(member.pet_id);
				writeD(member.pet_NpcId);
				writeD(member.pet_type);
				writeS(member.pet_Name);
				writeD(member.pet_curHp);
				writeD(member.pet_maxHp);
				writeD(member.pet_curMp);
				writeD(member.pet_maxMp);
				writeD(member.pet_level);
			}
			else
				writeD(0);
		}
	}

	public static class PartySmallWindowMemberInfo
	{
		public String _name, pet_Name;
		public int _id, curCp, maxCp, curHp, maxHp, curMp, maxMp, level, class_id, race_id;
		public int pet_id, pet_NpcId, pet_curHp, pet_maxHp, pet_curMp, pet_maxMp, pet_level, pet_type;

		public PartySmallWindowMemberInfo(L2Player member)
		{
			_name = member.getName();
			_id = member.getObjectId();
			curCp = (int) member.getCurrentCp();
			maxCp = member.getMaxCp();
			curHp = (int) member.getCurrentHp();
			maxHp = member.getMaxHp();
			curMp = (int) member.getCurrentMp();
			maxMp = member.getMaxMp();
			level = member.getLevel();
			class_id = member.getClassId().getId();
			race_id = member.getRace().ordinal();

			L2Summon pet = member.getPet();
			if(pet != null)
			{
				pet_id = pet.getObjectId();
				pet_NpcId = pet.getNpcId() + 1000000;
				pet_type = pet.getSummonType();
				pet_Name = pet.getName();
				pet_curHp = (int) pet.getCurrentHp();
				pet_maxHp = pet.getMaxHp();
				pet_curMp = (int) pet.getCurrentMp();
				pet_maxMp = pet.getMaxMp();
				pet_level = pet.getLevel();
			}
			else
				pet_id = 0;
		}
	}
}