package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.AcquireSkillInfo;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.tables.SkillTreeTable;

public class RequestAquireSkillInfo extends L2GameClientPacket
{
	// format: cddd
	private int _id;
	private byte _level;
	private int _skillType;

	@Override
	public void readImpl()
	{
		_id = readD();
		_level = (byte) readD();
		_skillType = readD();// normal(0) learn or fisherman(1) clan(2) ? (3) transformation (4)
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || player.getTransformation() != 0 || SkillTable.getInstance().getInfo(_id, _level) == null)
			return;
		L2NpcInstance trainer = player.getLastNpc();
		// В ГоД изучение скилов доступно из панели скилов
		//if((trainer == null || !player.isInRange(trainer, player.getInteractDistance(trainer))) && !player.isGM())
		//	return;
		if(_skillType == SkillTreeTable.SKILL_TYPE_CLAN || _skillType == SkillTreeTable.SKILL_TYPE_CLAN_SUB_PLEDGE)
			sendPacket(new AcquireSkillInfo(_id, _level, player.getClassId(), player.getClan()));
		else
			sendPacket(new AcquireSkillInfo(_id, _level, player.getClassId(), null));
	}
}