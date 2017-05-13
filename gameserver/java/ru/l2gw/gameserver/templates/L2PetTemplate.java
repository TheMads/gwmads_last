package ru.l2gw.gameserver.templates;

import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.commons.arrays.GArray;

import java.util.HashMap;

/**
 * @author rage
 * @date 02.11.2010 15:41:19
 */
public class L2PetTemplate extends L2NpcTemplate
{
	public int index;
	public int controlItemId;
	public int level;
	public boolean sync_level;
	public int meal_in_battle;
	public int meal_in_normal;
	public long exp;
	public GArray<Integer> food;
	public float exp_type;
	public float hungry_limit;
	public int max_meal;
	public float org_hp;
	public float org_hp_regen;
	public float org_mp;
	public float org_mp_regen;
	public float org_pattack;
	public float org_mattack;
	public float org_pdefend;
	public float org_mdefend;
	public int soulshot_count;
	public int spiritshot_count;
	public int attack_speed_on_ride;
	public int meal_in_battle_on_ride;
	public int meal_in_normal_on_ride;
	public int mattack_on_ride;
	public int pattack_on_ride;
	public int speed_on_ride_g;
	public int speed_on_ride_s;
	public int speed_on_ride_f;
	public int ride_state;

	public L2PetTemplate(StatsSet set, StatsSet aiParams)
	{
		super(set, aiParams);
	}

	public void setSkills(HashMap<Integer, L2Skill> skills)
	{
		_skills = skills;
	}

	@Override
	public String toString()
	{
		return "pet template[npcId=" + npcId + ";name=" + name + ";level=" + level + ";controlItemId=" + controlItemId + ";index=" + index +"]";
	}
}
