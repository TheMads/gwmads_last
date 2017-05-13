package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.tables.SkillTable;

public class RequestMagicSkillUse extends L2GameClientPacket
{
	private Integer _magicId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;

	/**
	 * format:		cddc
	 */
	@Override
	public void readImpl()
	{
		_magicId = readD();
		_ctrlPressed = readD() != 0;
		_shiftPressed = readC() != 0;
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		if(player.inObserverMode())
		{
			player.sendPacket(Msg.OBSERVERS_CANNOT_PARTICIPATE);
			player.sendActionFailed();
			return;
		}

		if(player.isOutOfControl())
		{
			player.sendActionFailed();
			return;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(_magicId, player.getSkillLevel(_magicId));
		if(skill != null)
		{
			if(skill.isPassive() /*|| skill.isOnAttack() || skill.isOnMagicAttack() || skill.isOnUnderAttack() || skill.isOnCrit() || skill.isOnDebuff()*/)
				return;

			// В режиме трансформации доступны только скилы трансформы
			if(player.getTransformation() != 0 && !player.getAllSkills().contains(skill))
				return;

			L2Character target = skill.getAimingTarget(player);
			player.fireMethodInvoked(MethodCollection.onSkillUse, new Object[]{skill, target, null, _ctrlPressed});

			//if(skill.checkCondition(player, target, _ctrlPressed, true))
			//{
				player.setGroundSkillLoc(null);
				player.getAI().Cast(skill, target, null, _ctrlPressed, _shiftPressed);
			//}
			//else
			//	player.sendActionFailed();
		}
		else
			player.sendActionFailed();
	}
}
