package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.base.Experience;
import ru.l2gw.gameserver.model.instances.L2SummonInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.funcs.FuncAdd;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 23.11.2009 13:29:14
 */
public class i_summon extends i_effect
{
	private final float expPenalty;
	private final int itemConsumeIdInTime;
	private final int itemConsumeCountInTime;
	private final int itemConsumeDelay;
	private final int lifeTime;

	public i_summon(EffectTemplate template)
	{
		super(template);
		expPenalty = template._attrs.getFloat("expPenalty", 0.f);
		itemConsumeIdInTime = template._attrs.getInteger("itemConsumeIdInTime", 0);
		itemConsumeCountInTime = template._attrs.getInteger("itemConsumeCountInTime", 0);
		itemConsumeDelay = template._attrs.getInteger("itemConsumeDelay", 250) * 1000;
		lifeTime = template._attrs.getInteger("lifeTime", 1200) * 1000;

	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		L2Player player = cha.isPlayer() ? cha.getPlayer() : null;
		if(player == null || player.isPetSummoned() || player.getMountEngine().isMounted())
			return;

		L2NpcTemplate summonTemplate = NpcTable.getTemplate(getSkill().getNpcId());
		L2SummonInstance summon = new L2SummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, player, lifeTime, itemConsumeIdInTime, itemConsumeCountInTime, itemConsumeDelay);

		summon.setTitle(player.getName());
		summon.setExpPenalty(expPenalty);
		summon.setExp(Experience.LEVEL[summon.getLevel()]);
		summon.setCurrentHp(summon.getMaxHp());
		summon.setCurrentMp(summon.getMaxMp());
		summon.setHeading(player.getHeading());
		summon.setRunning();

		player.setPet(summon);
		summon.spawnMe(GeoEngine.findPointToStay(player.getX(), player.getY(), player.getZ(), 40, 40, player.getReflection()));

		if(summon.getSkillLevel(4140) > 0)
			summon.altUseSkill(SkillTable.getInstance().getInfo(4140, summon.getSkillLevel(4140)), player, null);

		if(summon.getName().equalsIgnoreCase("Shadow"))
			summon.addStatFunc(new FuncAdd(Stats.ABSORB_DAMAGE_PERCENT, 0x40, this, 15));

		summon.setFollowStatus(true);
		summon.broadcastPetInfo();
		summon.setShowSpawnAnimation(false);
	}
}
