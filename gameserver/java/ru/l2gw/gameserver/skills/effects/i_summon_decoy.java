package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2DecoyInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 25.11.2009 9:58:20
 */
public class i_summon_decoy extends i_effect
{
	private final int lifeTime, skillId, skillLvl;
	public i_summon_decoy(EffectTemplate template)
	{
		super(template);
		lifeTime = template._attrs.getInteger("lifeTime", 1200) * 1000;
		skillId = template._attrs.getInteger("provokeSkill", 0);
		skillLvl = template._attrs.getInteger("provokeLevel", 0);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(cha.isPlayer())
		{
			L2Player player = (L2Player) cha;
			if(player.getPet() != null || player.getMountEngine().isMounted())
				return;

			L2NpcTemplate decoyTemplate = NpcTable.getTemplate(getSkill().getNpcId());
			L2DecoyInstance decoy = new L2DecoyInstance(IdFactory.getInstance().getNextId(), decoyTemplate, player, lifeTime, skillId, skillLvl);

			decoy.setCurrentHp(decoy.getMaxHp());
			decoy.setCurrentMp(decoy.getMaxMp());
			decoy.setHeading(player.getHeading());
			decoy.setReflection(player.getReflection());
			player.setDecoy(decoy);
			decoy.spawnMe(player.getLoc().changeZ(10));
		}
	}
}
