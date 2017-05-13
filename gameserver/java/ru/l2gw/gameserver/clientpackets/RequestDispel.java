package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.tables.SkillTable;

/**
 * @author viRUS
 */
public class RequestDispel extends L2GameClientPacket
{
	// Format: chddd
	private int objectId;
	private int skillId;
	private int skillLvl;

	@Override
	public void readImpl()
	{
		objectId = readD();
		skillId = readD();
		skillLvl = readD();
		// TODO: Корректно конвертировать точеные скиллы от клиента в уровень скилла датапака
		if(skillLvl > 100)
			skillLvl = 1;
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
		if(skill == null)
		{
			_log.debug("RequestDispel skillId=" + skillId + " skillLvl=" + skillLvl + " " + player);
			return;
		}

		if(!skill.isDebuff() && !skill.isSongDance() && !skill.isToggle()
				&& skill.isCancelable() && !skill.getAbnormalTypes().contains("transformation") && !skill.getAbnormalTypes().contains("BlessOfNoble") && !skill.getAbnormalTypes().contains("hourglass") &&
				skill.getId() != 5104 && skill.getId() != 5105)
		{
			if(player.getObjectId() == objectId)
				player.stopEffect(skillId);
			else if(player.getPet() != null && player.getPet().getObjectId() == objectId)
				player.getPet().stopEffect(skillId);
		}
		else
			player.sendPacket(Msg.ONLY_AN_ENHANCED_SKILL_CAN_BE_CANCELLED);
	}
}