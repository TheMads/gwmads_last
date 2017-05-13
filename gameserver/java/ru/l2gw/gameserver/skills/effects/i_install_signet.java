package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.instances.L2EffectPointInstance;
import ru.l2gw.gameserver.serverpackets.MagicSkillUse;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Location;

/**
 * @author: rage
 * @date: 15.07.2010 16:20:01
 */
public class i_install_signet extends i_effect
{
	private final int _startDelay;
	private final int _delay;
	private final int _totalTime;
	private L2Skill _skill;
	private L2NpcTemplate _npcTemplate;

	public i_install_signet(EffectTemplate template)
	{
		super(template);
		String[] conf = template._options.split(";");
		_startDelay = Integer.parseInt(conf[0]);
		_delay = Integer.parseInt(conf[1]);
		_totalTime = Integer.parseInt(conf[2]);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		_skill = SkillTable.getInstance().getInfo(getSkill().getTriggerSkillId(), getSkill().getTriggerSkillLvl());
		_npcTemplate = NpcTable.getTemplate(getSkill().getEffectNpcId());

		if(_skill == null || !cha.isPlayer() || _npcTemplate == null)
		{
			_log.info(this + " cannot install signet skill: " + _skill + " isPlayer: " + cha.isPlayer() + " npcTemplate: " + _npcTemplate + " npcId: " + getSkill().getEffectNpcId());
			return;
		}

		try
		{
			L2Player player = (L2Player) cha;
			Location loc = cha.getLoc();
			if(player.getGroundSkillLoc() != null)
			{
				loc = player.getGroundSkillLoc();
				player.setGroundSkillLoc(null);
			}

			L2EffectPointInstance effectPoint = new L2EffectPointInstance(IdFactory.getInstance().getNextId(), _npcTemplate, cha.getPlayer());
			player.setEffectPoint(effectPoint);
			effectPoint.setCurrentHp(effectPoint.getMaxHp());
			effectPoint.setCurrentMp(effectPoint.getMaxMp());

			effectPoint.setIsInvul(true);
			effectPoint.setSkill(_skill, getSkill());
			effectPoint.setReflection(cha.getReflection());
			effectPoint.spawnMe(loc);
			effectPoint.startActionTask(_startDelay, _delay, _totalTime);
			player.broadcastPacket(new MagicSkillUse(player, _skill.getDisplayId(), _skill.getDisplayLevel(), 0, 0));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

}
