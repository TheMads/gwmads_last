package ru.l2gw.gameserver.ai;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.instances.L2ArtefactInstance;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2SiegeGuardInstance;

public class L2StaticObjectAI extends L2CharacterAI
{
	L2Character _actor;
	L2Player _attacker;

	public L2StaticObjectAI(L2Character actor)
	{
		super(actor);
		_actor = actor;
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage, L2Skill skill)
	{
		if(!(_actor instanceof L2DoorInstance) || attacker == null)
			return;

		L2Player player = attacker.getPlayer();

		if(player != null)
		{
			L2Clan clan = player.getClan();

			Siege siege = SiegeManager.getSiege(_actor);

			if(siege == null)
				return;

			//TODO присвоить осаду обьекту при спавне, чтобы избавиться от перебора
			if(clan != null && siege == clan.getSiege() && clan.isDefender())
				return;

			for(L2NpcInstance npc : _actor.getKnownNpc(900))
			{
				if(!(npc instanceof L2SiegeGuardInstance))
					continue;

				if(Math.abs(attacker.getZ() - npc.getZ()) < 200)
					if(Rnd.chance(20))
						npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 10000, skill);
					else
						npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 2000, skill);
			}
		}
	}

	@Override
	protected void onEvtAggression(L2Character attacker, int aggro, L2Skill skill)
	{
		if(!(_actor instanceof L2ArtefactInstance))
			return;

		if(attacker != null && attacker.getPlayer() != null)
		{
			L2Clan clan = attacker.getPlayer().getClan();
			//TODO присвоить осаду обьекту при спавне, чтобы избавиться от перебора
			if(clan != null && SiegeManager.getSiege(_actor) == clan.getSiege() && clan.isDefender())
				return;
			ThreadPoolManager.getInstance().scheduleAi(new notifyGuard(attacker.getPlayer()), 1000, false);
		}
	}

	public class notifyGuard implements Runnable
	{
		notifyGuard(L2Player attacker)
		{
			_attacker = attacker;
		}

		public void run()
		{
			if(_attacker == null)
				return;

			for(L2NpcInstance npc : _actor.getKnownNpc(900))
				if(npc instanceof L2SiegeGuardInstance && Rnd.chance(20))
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, _attacker, 5000);

			if(_attacker.getCastingSkill() != null && _attacker.getCastingSkill().getSkillTargetType() == L2Skill.TargetType.holything)
				ThreadPoolManager.getInstance().scheduleAi(new notifyGuard(_attacker), 10000, false);
		}
	}
}