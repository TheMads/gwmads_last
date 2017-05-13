package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;

public class RequestTargetCanceld extends L2GameClientPacket
{
	private int _unselect;

	/**
	 * format:		ch
	 */
	@Override
	public void readImpl()
	{
		_unselect = readH();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.getUnstuck() == 1)
		{
			player.setUnstuck(0);
			player.unblock();
		}

		if(_unselect == 0)
		{
			if(player.isCastingNow())
			{
				if(player.getCastingSkill() != null
						&& ((player.getCastingSkill().getSkillTargetType() == L2Skill.TargetType.ground
						&& player.getCastingSkill().isCastTimeEffect() && player.getAnimationEndTime() < System.currentTimeMillis())))
				{
					player.sendActionFailed();
					return;
				}
				player.abortCast();
			}
			else if(player.getTarget() != null)
				player.setTarget(null, true);
		}
		else if(player.getTarget() != null)
			player.setTarget(null, true);
	}
}
