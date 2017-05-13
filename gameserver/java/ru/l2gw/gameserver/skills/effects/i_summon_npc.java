package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.extensions.scripts.Script;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Location;

import java.lang.reflect.Constructor;

/**
 * @author rage
 * @date 23.11.2009 18:30:41
 */
public class i_summon_npc extends i_effect
{
	private final int lifeTime;

	public i_summon_npc(EffectTemplate template)
	{
		super(template);
		lifeTime = template._attrs.getInteger("lifeTime", 1200) * 1000;
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		try
		{
			L2NpcTemplate template = NpcTable.getTemplate(getSkill().getNpcId());
			String implementationName = template.type;
			Constructor<?> constructor;
			try
			{
				constructor = Class.forName("ru.l2gw.gameserver.model.instances." + implementationName + "Instance").getConstructors()[0];
			}
			catch(ClassNotFoundException e)
			{
				Script script = Scripts.getInstance().getClasses().get("npc.model." + implementationName + "Instance");
				if(script == null)
				{
					_log.warn("Script " + "npc.model." + implementationName + "Instance.java not found or loaded with errors. Npc id: " + template.npcId + " use L2Npc.");
					constructor = L2NpcInstance.class.getConstructors()[0];
				}
				else
					constructor = script.getRawClass().getConstructors()[0];
			}

			Object tmp = constructor.newInstance(IdFactory.getInstance().getNextId(), template, 0L, cha.getStoredId(), 0L, 0L);

			// Check if the Instance is a L2NpcInstance
			if(!(tmp instanceof L2NpcInstance))
				return;

			L2NpcInstance mob = (L2NpcInstance) tmp;
			mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
			mob.setNpcState(0);
			mob.stopAllEffects();
			mob.setHeading(cha.getHeading());

			Location newLoc = GeoEngine.findPointToStay(cha.getX(), cha.getY(), cha.getZ(), 40, 40, cha.getReflection());
			mob.setSpawnedLoc(newLoc);

			if(cha.getReflection() != 0)
				mob.setReflection(cha.getReflection());

			mob.spawnMe(newLoc);
			mob.onSpawn();
			if(lifeTime > 0)
				ThreadPoolManager.getInstance().scheduleGeneral(new DespawnTask(mob), lifeTime);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private class DespawnTask implements Runnable
	{
		private L2NpcInstance _npc;

		public DespawnTask(L2NpcInstance npc)
		{
			_npc = npc;
		}

		public void run()
		{
			if(_npc != null)
				_npc.deleteMe();
		}
	}
}