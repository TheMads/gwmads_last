package ru.l2gw.gameserver.handler;

import gnu.trove.map.hash.TIntObjectHashMap;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.serverpackets.SocialAction;

/**
 * @author rage
 * @date 11.08.2010 10:59:21
 */
public class RequestAction
{
	public static enum Type
	{
		player_sit,
		player_run,
		player_next_target,
		player_private_store_buy,
		player_private_store_sell,
		player_private_store_package_sell,
		player_private_store_manufacture,
		player_common_craft,
		player_social,
		player_social_couple,
		player_mount,
		player_bot_report,
		player_airship_steer,
		player_airship_cancel_control,
		player_airship_destination_map,
		player_airship_exit,
		servitor_follow,
		servitor_move_to_target,
		servitor_attack,
		servitor_stop,
		servitor_unsummon,
		servitor_skill,
		servitor_skill_by_level,
		tactical_star,
		tactical_heart,
		tactical_moon,
		tactical_cross,
		tactical_search_star,
		tactical_search_heart,
		tactical_search_moon,
		tactical_search_cross
	}

	private final int _actionId;
	private final Type _type;
	private final boolean _transform;
	private final boolean _servitorAction;
	private SocialAction.SocialType _socailType;
	private int _skillId;
	private TIntObjectHashMap<L2Skill> _skillsByLevel;

	public RequestAction(int actionId, Type type, boolean transform)
	{
		_actionId = actionId;
		_type = type;
		_transform = transform;
		_servitorAction = _type.toString().startsWith("servitor_");
	}

	public Type getActionType()
	{
		return _type;
	}

	public boolean allowInTransform()
	{
		return _transform;
	}

	public boolean isServitorAction()
	{
		return _servitorAction;
	}

	public int getSocialId()
	{
		return _socailType.getId();
	}

	public int getSkillId()
	{
		return _skillId;
	}

	public void setSkillId(int skillId)
	{
		_skillId = skillId;
	}

	public void setSocialType(SocialAction.SocialType socialType)
	{
		_socailType = socialType;
	}

	public void addSkillByLevel(L2Skill skill, int minLevel)
	{
		if(_skillsByLevel == null)
			_skillsByLevel = new TIntObjectHashMap<>();
		_skillsByLevel.put(minLevel, skill);
	}

	public L2Skill getSkillByLevel(int level)
	{
		if(_skillsByLevel == null)
			return null;

		int maxLvl = -1;
		for(int lvl : _skillsByLevel.keys())
			if(level >= lvl && lvl > maxLvl)
				maxLvl = lvl;

		if(maxLvl >= 0)
			return _skillsByLevel.get(maxLvl);

		return null;
	}
}
