package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.util.Location;

/**
 * @author SYS
 * @date 08/9/2007
 * Format: chdddddc
 *
 * Пример пакета:
 * D0
 * 2F 00
 * E4 35 00 00 x
 * 62 D1 02 00 y
 * 22 F2 FF FF z
 * 90 05 00 00 skill id
 * 00 00 00 00 ctrlPressed
 * 00 shiftPressed
 */
public class RequestExMagicSkillUseGround extends L2GameClientPacket
{
	private Location _loc = new Location(0, 0, 0);
	private int _skillId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;

	@Override
	public void readImpl()
	{
		_loc.setX(readD());
		_loc.setY(readD());
		_loc.setZ(readD());
		_skillId = readD();
		_ctrlPressed = readD() != 0;
		_shiftPressed = readC() != 0;
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.isOutOfControl())
		{
			player.sendActionFailed();
			return;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(_skillId, player.getSkillLevel(_skillId));
		if(skill != null)
		{
			// В режиме трансформации доступны только скилы трансформы
			if(player.getTransformation() != 0 && !player.getAllSkills().contains(skill))
				return;

			L2Character target = skill.getAimingTarget(player);
			player.fireMethodInvoked(MethodCollection.onSkillUse, new Object[]{skill, target, null, _ctrlPressed});

			player.setGroundSkillLoc(_loc);
			if(skill.checkCondition(player, target, null, _ctrlPressed, true))
				player.getAI().Cast(skill, target, null, _ctrlPressed, _shiftPressed);
			else
			{
				player.sendActionFailed();
				player.setGroundSkillLoc(null);
			}
		}
		else
			player.sendActionFailed();
	}
}